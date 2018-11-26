package citySim.environment;



import java.util.List;

import citySim.agent.Agent;
import citySim.agent.Car;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
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
	private List<Road> goals;
	private ShortestPath<Object> shortestPath;
	private Network<Object> net;
	
	
	public Spawner(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context, List<Road> spawnPoints, List<Road> goals) {
		super();
		this.space = space;
		this.grid = grid;
		this.context = context;
		this.spawnPoints = spawnPoints;
		this.goals = goals;
		if(spawnPoints.size() == 0 || goals.size() == 0) {
			throw new IllegalArgumentException("no spawn or goal");
		}
		
		
		net = (Network<Object>)context.getProjection("road network");
		
		
		
		//TODO: Check instance of Agent?
		//TODO: Variable start and interval?
		
	}
	
	//TODO: spawn frequency
	@ScheduledMethod(start = 1, interval = 1)
	public void spawn() {
		
		//Start and goal
		int spawnPointIndex = RandomHelper.nextIntFromTo(0,  spawnPoints.size() - 1);
		int despawnPointIndex = RandomHelper.nextIntFromTo(0,  goals.size() - 1);
		Road start = spawnPoints.get(spawnPointIndex);
		Road goal = goals.get(despawnPointIndex);
		NdPoint spacePt = space.getLocation(start);
		GridPoint pt = grid.getLocation(start);
		
		//Check surroundings
		GridCellNgh<Car> roadNghCreator = new GridCellNgh<Car>(grid, pt, Car.class, 1, 1);
		List<GridCell<Car>> roadGridCells = roadNghCreator.getNeighborhood(true);
		for (GridCell<Car> gridCell : roadGridCells) {
			if(gridCell.items().iterator().hasNext()) {
				//There is a car close to spawn, wait
				return;
			}
		}
		
		//Add the agent to the context
		Car car = new Car(space, grid);
		context.add(car);
		space.moveTo(car, spacePt.getX(), spacePt.getY());
		grid.moveTo(car, pt.getX(), pt.getY());
		
		//Setup
		car.setGoal(goal);
		car.setStart(start);
		car.setNet(net);
		
	}
}
