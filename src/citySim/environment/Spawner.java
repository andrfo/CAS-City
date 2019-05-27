package citySim.environment;



import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import citySim.Reporter;
import citySim.agent.Bus;
import citySim.agent.Car;
import citySim.agent.Person;
import citySim.agent.Vehicle;
import citySim.environment.electric.Building;
import citySim.environment.roads.BusStop;
import citySim.environment.roads.Road;
import citySim.environment.roads.Spawn;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;

import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import utils.Tools;

/**
 * Handles the spawning of agents in the simulation and delegates to Spawns
 * @author andrfo
 *
 */
public class Spawner {

	
	
	/**
	 * Spawns agents periodically
	 * TODO: Figure out class stuff (Generalize)
	 */
	
	
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private Context<Object> context;
	private Road[] spawnPoints;
	private List<Road> despawnPoints;
	private List<Road> parkingNexi;
	private List<Road> parkingSpaces;
	private List<Building> buildings;
	private List<BusStop> busStops;
	private Network<Object> net;
	
	

	/** TimeCycle
	 * 	1 Tick = 10 sec
	 * 	24h = 8640 ticks
	 * 	1 hour = 360 ticks
	 */
	
	//Time of day translation from ticks and schedule
	private static final int[] NIGHT = {0, 2160}; 				//00:00 - 06:00
	private static final int[] MORNING = {2160, 4320}; 			//06:00 - 12:00
	private static final int[] AFTERNOON = {4320, 6480}; 		//12:00 - 18:00
	private static final int[] EVENING = {6480, 8640}; 			//18:00 - 00:00
	
	private static final int[] MORNING_RUSH = {2520, 3060}; 	//07:00 - 08:30
	private static final int[] AFTERNOON_RUSH = {5580, 6120}; 	//15:30 - 17:00
	
	private static final int[] BUS =  {2160, 7920};
	
	/**
	 * The distribution between the spawn points
	 * where the lists index + 1 corresponds to the spawn point number noted in the simulation by sidewalks in the shape of numbers
	 * 
	 * Does not need to sum to 1, you can have relative probabilities
	 */
	private Double[] loadDistribution;
	private Double[] csum; //Cumulative load distribution
	
	private Double nightFrequency;
	private Double morningFrequency;
	private Double afternoonFrequency;
	private Double eveningFrequency;
	private Double rushFrequency;
	private int populationStartCount;
	private int personsPerCar;
	
	
	private List<Person> population;
	
	private static final int DAYS_TO_RUN = 7;
	
	private double frequency; //Spawns per tick
	private ArrayList<Person> idleWorkers;
	private ArrayList<Person> idleShoppers;
	private Reporter reporter;
	
	
	@SuppressWarnings("unchecked")
	public Spawner(
			ContinuousSpace<Object> space, 
			Grid<Object> grid, 
			Context<Object> context, 
			Road[] spawnPoints, 
			List<Road> despawnPoints, 
			List<Road> parkingSpaces, 
			List<Building> buildings, 
			List<BusStop> busStops, 
			List<Road> parkingNexiRoads) {
		super();
		this.space = space;
		this.grid = grid;
		this.context = context;
		this.spawnPoints = spawnPoints;
		this.despawnPoints = despawnPoints;
		this.parkingSpaces = parkingSpaces;
		this.buildings = buildings;
		this.busStops = busStops;
		this.parkingNexi = parkingNexiRoads;
		this.reporter = new Reporter();
		if(spawnPoints.length == 0 || despawnPoints.size() == 0) {
			throw new IllegalArgumentException("no spawn or goal");
		}
		
		
		net = (Network<Object>)context.getProjection("road network");
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		//Sets up the parameters to be determined in the GUI
		this.nightFrequency = params.getDouble("Car_frequency_at_Night");
		this.morningFrequency = params.getDouble("Car_frequency_in_the_Morning");
		this.afternoonFrequency = params.getDouble("Car_frequency_in_the_Afternoon");
		this.eveningFrequency = params.getDouble("Car_frequency_in_the_Evening");
		this.rushFrequency = params.getDouble("Car_frequency_in_Rushhour");
		this.populationStartCount = params.getInteger("population_start_count");
		
		this.loadDistribution = new Double[4];
		loadDistribution[0] = params.getDouble("load_on_entry_1");
		loadDistribution[1] = params.getDouble("load_on_entry_2");
		loadDistribution[2] = params.getDouble("load_on_entry_3");
		loadDistribution[3] = params.getDouble("load_on_entry_4");
		
		this.population = new ArrayList<Person>(populationStartCount);
		this.idleWorkers = new ArrayList<Person>();
		this.idleShoppers = new ArrayList<Person>();
		generatePopulation();
		
		//Create a cumulative probabilities list
		csum = new Double[spawnPoints.length];
		Double sum = 0d;
		for(int i = 0; i < loadDistribution.length; i++) {
			csum[i] = sum + loadDistribution[i];
			sum = Double.valueOf(csum[i]);
		}
		
	}
	
	
	
	
	/**
	 * Is called each step of the simulation
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		
		//Are we there yet?
		isRunEnd();
		
		//Spawn frequencies
		setFrequency();
		
		//Spawns agents into the model
		spawn();
	}
	
	/**
	 * Checks whether the simulation has run the determined amount of days. if so it ends the run.
	 */
	private void isRunEnd() {
		double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		if(currentTick >= Tools.TICKS_PER_DAY * DAYS_TO_RUN) {
			RunEnvironment.getInstance().endRun();
		}
	}
	
	
	/**
	 * Generates the population and splits it into workers and shoppers
	 */
	private void generatePopulation() {
		for(int i = 0; i < populationStartCount; i++) {
			Person p = new Person(space, grid, this);
			if(Tools.isTrigger(0.75d)) { //75% chance
				//Worker
				p.setWorkPlace(buildings.get(RandomHelper.nextIntFromTo(0, buildings.size() - 1)));
				population.add(p);
				idleWorkers.add(p);
			}
			else {
				//Shopper
				population.add(p);
				idleShoppers.add(p);
				
			}
			
		}
	}
	
	/**
	 * Sets up and spawns the agents of the simulation, and ads them to the queue of a(random) spawn point
	 */
	private void spawn() {
		//TODO: implement car pooling
		int spawnCount;
		int time = Tools.getTime();
		if(time % 30 == 0 /*&& isInInterval(time, BUS)*/) { //Spawn bus every 5 minutes from a random spawn
			Road r = getSpawnPoint();
				Spawn s = (Spawn) r;
				Bus bus = new Bus(space, grid, 50, parkingNexi);
				for(int i = 0; i < busStops.size(); i++) {
					Road currentBussStop = r;
					List<Road> used = new ArrayList<Road>();
					double dist = 0;
					double maxDist = Double.MAX_VALUE;
					Road bestBusStop = null;
					for(Road b : busStops) {
						if(used.contains(b)) {
							continue;
						}
						dist = Tools.gridDistance(currentBussStop.getLocation(), b.getLocation());
						if(dist < maxDist) {
							bestBusStop = b;
						}
					}
					bus.addGoal(bestBusStop);
					used.add(bestBusStop);
					currentBussStop = bestBusStop;
				}
				s.addToVehicleQueue(bus);
		}
		if(isInInterval(time, MORNING_RUSH)) { //Spawn worker
			//98% of the workers are going to work over an hour and a half(2% are sick)
			Double workers = ((double) idleWorkers.size())*0.98d*(1d/540d);
			BigDecimal[] valRem = BigDecimal.valueOf(workers).divideAndRemainder(BigDecimal.ONE);
			spawnCount = valRem[0].intValue();
			if(Tools.isTrigger(valRem[1].doubleValue())) { //Uses the remainder as a probability for an extra spawn
				spawnCount++;
			}
			spawnAgent(true, spawnCount);
		}
		else { //Spawn shopper
			BigDecimal[] valRem = BigDecimal.valueOf(frequency).divideAndRemainder(BigDecimal.ONE);
			spawnCount = valRem[0].intValue();
			if(Tools.isTrigger(valRem[1].doubleValue())) { //Uses the remainder as a probability for an extra spawn
				spawnCount++;
			}
			spawnAgent(false, spawnCount);
		}
	}
	
	/**
	 * Sets up and spawns a person into the simulation in either a car, or as waiting for a bus as a spawn point.
	 * @param isWorker is it a worker? if not, its a shopper
	 * @param spawnCount The number of agents to spawn
	 */
	private void spawnAgent(boolean isWorker, int spawnCount) {
		if(isWorker) {
			for (int i = 0; i < spawnCount; i++) {
				if(idleWorkers.size() == 0) {
					return;
				}
				Person p = idleWorkers.remove(0);
				
				//Start and goal
				Spawn start = getSpawnPoint();
				if(p.getTravelChoice().equals("bus")) {//Bus
					start.addToBusQueue(p);
				}
				else {//Car
					
					Car car = new Car(space, grid, 5, parkingNexi);
					car.addOccupant(p);
					
					//Setup
					
					car.addGoal(p.getWorkPlace());
					car.setStart(start);
					car.setNet(net);
					
					start.addToVehicleQueue(car);
				}
			}
		}
		else {//Shopper
			for (int i = 0; i < spawnCount; i++) {
				if(idleShoppers.size() == 0) {
					continue;
				}
				
				//Start and goal
				Spawn start = getSpawnPoint();
				Person p = idleShoppers.remove(0);
				
				//Random shopping place each trip
				p.setShoppingPlace(buildings.get(RandomHelper.nextIntFromTo(0, buildings.size() - 1)));
				
				if(p.getTravelChoice().equals("bus")) {//Bus
					start.addToBusQueue(p);
				}
				else {//car
				
					//Add the agent to the context
					Car car = new Car(space, grid, 5, parkingNexi);
					
					car.addOccupant(p);
					
					//Setup
					
//					car.addGoal(parkingSpaces.get(RandomHelper.nextIntFromTo(0, parkingSpaces.size() - 1)));//Random parking space as a goal
					car.addGoal(p.getShoppingPlace());
					car.setStart(start);
					car.setNet(net);
					
					start.addToVehicleQueue(car);
				}
			}
		}
	}
	
	/**
	 * Returns a random spawn point from the defined probability distribution
	 * @return Random spawn from distribution
	 */
	private Spawn getSpawnPoint() {
		Spawn start = null;
		
		double r = new Random().nextDouble() * csum[csum.length-1];
		
		for(int i = 0; i < spawnPoints.length; i++) {
		    if (csum[i] > r) {
		        start = (Spawn) spawnPoints[i];
		    	break;
		    }
		}
		return start;
	}
	
	/**
	 * spawn rate times 100 to make it show up on the graph in the GUI
	 * @return double, spawn rate*100
	 */
	public double getSpawnRate() {
		return frequency*100d;
	}
	
	public Reporter getReporter() {
		return reporter;
	}
	/**
	 * Sets the spawn rate based on the time of day
	 */
	private void setFrequency() {
		int time = Tools.getTime();
		frequency = 0;
		
		if(isInInterval(time, NIGHT)) {
			frequency += nightFrequency;
		}
		else if(isInInterval(time, MORNING)) {
			frequency += morningFrequency;
		}
		else if(isInInterval(time, AFTERNOON)) {
			frequency += afternoonFrequency;		
		}
		else if(isInInterval(time, EVENING)) {
			frequency += eveningFrequency;
		}
		
//		if(isInInterval(time, MORNING_RUSH)) {
//			frequency += rushFrequency;
//		}
//		else if(isInInterval(time, AFTERNOON_RUSH)) {
//			frequency += rushFrequency;
//		}
		
		
	}
	
	/**
	 * Helper function to determine of a tick tick count mod(day) is within an interval
	 * @param n
	 * @param interval
	 * @return True if within interval, false otherwise
	 */
	private boolean isInInterval(int n, int[] interval) {
		return n >= interval[0] && n < interval[1];
	}
	
	/**
	 * Returns a shopper to the pool of shopper
	 * @param p, Person
	 */
	public void returnShopper(Person p) {
		idleShoppers.add(p);
	}
	
	/**
	 * Returns are worker to the pool of workers
	 * @param p, Person
	 */
	public void returnWorker(Person p) {
		idleWorkers.add(p);
	}
}


































