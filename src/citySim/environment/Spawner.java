package citySim.environment;



import java.util.List;

import citySim.agent.Agent;
import citySim.agent.Car;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.graph.ShortestPath;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

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
	
	
	public Spawner(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context, List<Road> spawnPoints, List<Road> goals) {
		super();
		this.space = space;
		this.grid = grid;
		this.context = context;
		this.spawnPoints = spawnPoints;
		this.goals = goals;
		
		Network<Object> net = (Network<Object>)context.getProjection("road network");
		shortestPath = new ShortestPath<>(net);
		shortestPath.getPath(spawnPoints.get(0), goals.get(0));
		//TODO: init path somehow
		
		
		//TODO: Check instance of Agent?
		//TODO: Variable start and interval?
		
	}
	
	@ScheduledMethod(start = 1, interval = 10)
	public void spawn() {
		if (spawnPoints.size() > 0) {
			Car a = new Car(space, grid);
			int sp = RandomHelper.nextIntFromTo(0,  spawnPoints.size() - 1);
			Road start = spawnPoints.get(sp);
			NdPoint spacePt = space.getLocation(start);
			GridPoint pt = grid.getLocation(start);
			context.add(a);
			space.moveTo(a, spacePt.getX(), spacePt.getY());
			grid.moveTo(a, pt.getX(), pt.getY());
			//System.out.println("Car spawned at: " + spacePt.getX() + ", " + j);
			if (goals.size() > 0) {
				int ep = RandomHelper.nextIntFromTo(0,  goals.size() - 1);
				a.setGoal(goals.get(ep));
				a.setStart(start);
				a.setPath(shortestPath.getPath(start, goals.get(ep)));
			}
		}
	}
}
