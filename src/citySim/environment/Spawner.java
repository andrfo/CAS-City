package citySim.environment;



import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import citySim.agent.Bus;
import citySim.agent.Car;
import citySim.agent.Person;
import citySim.agent.Vehicle;
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

public class Spawner {

	
	
	/**
	 * Spawns agents periodically
	 * TODO: Figure out class stuff (Generalize)
	 */
	
	
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private Context<Object> context;
	private List<Road> spawnPoints;
	private List<Road> despawnPoints;
	private List<Road> parkingSpaces;
	private List<Building> buildings;
	private List<BusStop> busStops;
	private Network<Object> net;
	
	

	/** TimeCycle
	 * 	1 Tick = 1 Minute
	 * 	24h = 1440 minutes
	 */
	
	/** TimeCycle
	 * 	1 Tick = 10 sec
	 * 	24h = 8640 ticks
	 * 	1 hour = 360 ticks
	 */
	
	private static final int[] NIGHT = {0, 2160}; 				//00:00 - 06:00
	private static final int[] MORNING = {2160, 4320}; 			//06:00 - 12:00
	private static final int[] AFTERNOON = {4320, 6480}; 		//12:00 - 18:00
	private static final int[] EVENING = {6480, 8640}; 			//18:00 - 00:00
	
	private static final int[] MORNING_RUSH = {2520, 3060}; 	//07:00 - 08:30
	private static final int[] AFTERNOON_RUSH = {5580, 6120}; 	//15:30 - 17:00
	
	private static final int[] BUS =  {2160, 7920};
	
	private Double nightFrequency;
	private Double morningFrequency;
	private Double afternoonFrequency;
	private Double eveningFrequency;
	private Double rushFrequency;
	private int populationStartCount;
	private int personsPerCar;
	
	
	private List<Person> population;
	
	
	private double frequency; //Spawns per tick
	private ArrayList<Person> idleWorkers;
	private ArrayList<Person> idleShoppers;
	
	
	@SuppressWarnings("unchecked")
	public Spawner(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context, List<Road> spawnPoints, List<Road> despawnPoints, List<Road> parkingSpaces, List<Building> buildings, List<BusStop> busStops) {
		super();
		this.space = space;
		this.grid = grid;
		this.context = context;
		this.spawnPoints = spawnPoints;
		this.despawnPoints = despawnPoints;
		this.parkingSpaces = parkingSpaces;
		this.buildings = buildings;
		this.busStops = busStops;
		if(spawnPoints.size() == 0 || despawnPoints.size() == 0) {
			throw new IllegalArgumentException("no spawn or goal");
		}
		
		
		net = (Network<Object>)context.getProjection("road network");
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		this.nightFrequency = params.getDouble("Car_frequency_at_Night");
		this.morningFrequency = params.getDouble("Car_frequency_in_the_Morning");
		this.afternoonFrequency = params.getDouble("Car_frequency_in_the_Afternoon");
		this.eveningFrequency = params.getDouble("Car_frequency_in_the_Evening");
		this.rushFrequency = params.getDouble("Car_frequency_in_Rushhour");
		this.populationStartCount = params.getInteger("population_start_count");
		
		this.population = new ArrayList<Person>(populationStartCount);
		this.idleWorkers = new ArrayList<Person>();
		this.idleShoppers = new ArrayList<Person>();
		generatePopulation();
		
	}
	
	
	
	
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		setFrequency();
		
		spawn();
		
		
	}
	
	
	private void generatePopulation() {
		for(int i = 0; i < populationStartCount; i++) {
			Person p = new Person(space, grid, this);
			if(Tools.isTrigger(0.75d)) {
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
	
	private void spawn() {
		//TODO: implement car pooling
		int spawnCount;
		int time = Tools.getTime();
		if(time % 120 == 0 && isInInterval(time, BUS)) { //Spawn bus
			for(Road r : spawnPoints) {
				Spawn s = (Spawn) r;
				Bus bus = new Bus(space, grid, 50);
				for(Road b : busStops) {
					bus.addGoal(b);
				}
				s.addToVehicleQueue(bus);
				
			}
		}
		if(isInInterval(time, MORNING_RUSH)) { //Spawn worker
			//98% of the workers are going to work over an hour
			Double workers = (double) idleWorkers.size();
			spawnCount = (int) Math.ceil(workers*0.98d*(1d/60d));
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
	
	private void spawnAgent(boolean isWorker, int spawnCount) {
		if(isWorker) {
			for (int i = 0; i < spawnCount; i++) {
				if(idleWorkers.size() == 0) {
					return;
				}
				Person p = idleWorkers.remove(0);
				//Start and goal
				int spawnPointIndex = RandomHelper.nextIntFromTo(0,  spawnPoints.size() - 1);
				Spawn start = (Spawn) spawnPoints.get(spawnPointIndex);
				if(p.getTravelChoice().equals("bus")) {
					start.addToBusQueue(p);
				}
				else {
					
					Car car = new Car(space, grid, 5);
					car.addOccupant(p);
					
					//Setup
					
					car.addGoal(p.getWorkPlace());
					car.setStart(start);
					car.setNet(net);
					
					start.addToVehicleQueue(car);
				}
			}
		}
		else {
			for (int i = 0; i < spawnCount; i++) {
				if(idleShoppers.size() == 0) {
					continue;
				}
				
				//Start and goal
				int spawnPointIndex = RandomHelper.nextIntFromTo(0,  spawnPoints.size() - 1);
				Spawn start = (Spawn) spawnPoints.get(spawnPointIndex);
				
				
				
				//Add the agent to the context
				Car car = new Car(space, grid, 5);
				
				Person p = idleShoppers.remove(0);
				car.addOccupant(p);
				
				//Setup
				
				car.addGoal(parkingSpaces.get(RandomHelper.nextIntFromTo(0, parkingSpaces.size() - 1)));
				car.setStart(start);
				car.setNet(net);
				
				start.addToVehicleQueue(car);
				
			}
		}
		
		
		
	}
	
	public double getSpawnRate() {
		return frequency*100d;
	}
	
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
	
	
	private boolean isInInterval(int n, int[] interval) {
		return n >= interval[0] && n < interval[1];
	}
	
	
	public void returnShopper(Person p) {
		idleShoppers.add(p);
	}
	
	public void returnWorker(Person p) {
		idleWorkers.add(p);
	}
}


































