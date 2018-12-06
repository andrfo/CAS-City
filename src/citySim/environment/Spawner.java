package citySim.environment;



import java.math.BigDecimal;
import java.util.List;

import org.apache.ivy.plugins.matcher.MapMatcher;

import citySim.agent.Agent;
import citySim.agent.Car;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.graph.ShortestPath;
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
	private Network<Object> net;
	
	

	/** TimeCycle
	 * 	1 Tick = 1 Minute
	 * 	24h = 1440 minutes
	 */
	private static final int TICKS_PER_DAY = 1440;
	
	private static final int[] NIGHT = {0, 360}; 				//00:00 - 06:00
	private static final int[] MORNING = {360, 720}; 			//06:00 - 12:00
	private static final int[] AFTERNOON = {720, 1080}; 		//12:00 - 18:00
	private static final int[] EVENING = {1080, 1440}; 			//18:00 - 00:00
	
	private static final int[] MORNING_RUSH = {420, 510}; 		//07:00 - 08:30
	private static final int[] AFTERNOON_RUSH = {930, 1020}; 	//15:30 - 17:00
	
	
	private Double nightFrequency;
	private Double morningFrequency;
	private Double afternoonFrequency;
	private Double eveningFrequency;
	private Double rushFrequency;
	
	private double frequency; //Spawns per tick
	
	
	public Spawner(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context, List<Road> spawnPoints, List<Road> despawnPoints, List<Road> parkingSpaces, List<Building> buildings) {
		super();
		this.space = space;
		this.grid = grid;
		this.context = context;
		this.spawnPoints = spawnPoints;
		this.despawnPoints = despawnPoints;
		this.parkingSpaces = parkingSpaces;
		this.buildings = buildings;
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
		
		
	}
	
	
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		setFrequency();
		
		spawn();
		
		
	}
	
	public void spawn() {
		//TODO: fix that the cars don't spawn when blocked
		int time = getTime();
		boolean blocked = false;
		BigDecimal[] valRem = BigDecimal.valueOf(frequency).divideAndRemainder(BigDecimal.ONE);
		
		int  spawnCount = valRem[0].intValue();
		if(Tools.isTrigger(valRem[1].doubleValue())) { //Uses the remainder as a probability for an extra spawn
			spawnCount++;
		}
		
		for (int i = 0; i < spawnCount; i++) {
			blocked = false;
			//Start and goal
			int spawnPointIndex = RandomHelper.nextIntFromTo(0,  spawnPoints.size() - 1);
			int despawnPointIndex = RandomHelper.nextIntFromTo(0,  despawnPoints.size() - 1);
			Road start = spawnPoints.get(spawnPointIndex);
			Road endGoal = despawnPoints.get(despawnPointIndex);
			NdPoint spacePt = space.getLocation(start);
			GridPoint pt = grid.getLocation(start);
			
			//Check surroundings
			GridCellNgh<Car> roadNghCreator = new GridCellNgh<Car>(grid, pt, Car.class, 1, 1);
			List<GridCell<Car>> roadGridCells = roadNghCreator.getNeighborhood(true);
			for (GridCell<Car> gridCell : roadGridCells) {
				if(gridCell.items().iterator().hasNext()) {
					//There is a car close to spawn, wait
					blocked = true;
				}
			}
			
			if(blocked) {
				continue;
			}
			
			//Add the agent to the context
			Car car = new Car(space, grid);
			context.add(car);
			space.moveTo(car, spacePt.getX(), spacePt.getY());
			grid.moveTo(car, pt.getX(), pt.getY());
			
			//Setup
			
			
			//work?
			if(		(isInInterval(time, MORNING_RUSH) || 
					isInInterval(time, AFTERNOON_RUSH)) &&
					Tools.isTrigger(rushFrequency/frequency)) {
				car.addGoal(buildings.get(RandomHelper.nextIntFromTo(0, buildings.size() - 1)));					
			}
			//errand?
			else if(Tools.isTrigger(0.6d)){
				car.addGoal(parkingSpaces.get(RandomHelper.nextIntFromTo(0, parkingSpaces.size() - 1)));
			}
			car.setStart(start);
			car.addGoal(endGoal); //TODO:make cars return to near start
			car.setNet(net);
			
		}
		
		
	}
	
	public double getSpawnRate() {
		return frequency*100d;
	}
	
	private int getTime() {
		double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		int time = (int) (currentTick % TICKS_PER_DAY);
		return time;
	}
	
	private void setFrequency() {
		int time = getTime();
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
		
		if(isInInterval(time, MORNING_RUSH)) {
			frequency += rushFrequency;
		}
		else if(isInInterval(time, AFTERNOON_RUSH)) {
			frequency += rushFrequency;
		}
		
		
	}
	
	
	private boolean isInInterval(int n, int[] interval) {
		return n >= interval[0] && n < interval[1];
	}
}


































