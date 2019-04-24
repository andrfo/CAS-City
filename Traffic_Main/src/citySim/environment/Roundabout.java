package citySim.environment;

import java.util.ArrayList;
import java.util.List;

import citySim.agent.Vehicle;
import citySim.environment.roads.Road;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import utils.Tools;

public class Roundabout {
	
	private List<Road> edgeRoads;
	private List<Road> roads;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private final int ACTIVATION_DELAY = 2;
	
	
	private NdPoint center;
	
	public Roundabout(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.edgeRoads = new ArrayList<Road>();
		this.roads =  new ArrayList<Road>();
		this.space = space;
		this.grid = grid;
		// TODO Auto-generated constructor stub
	}
	
	
	
//	@ScheduledMethod(start = 1, interval = 1)
//	public void step() {
//		if(queue.size() == 0) {
//			return;
//		}
//		List<Car> remove = new ArrayList<Car>();
//		
//		for(Car c : remove) {
//			queue.remove(c);
//		}
//		remove.clear();
//		
//	}
//	
	
	
	public NdPoint getCenter() {
		if(center != null) {
			return center;			
		}
		if(roads.size() == 0 || roads == null) {
			return null;
		}
		
		double Ax = 0;
		double Ay = 0;
		double n = roads.size();
		
		for(Road r : roads) {
			Ax += space.getLocation(r).getX();
			Ay += space.getLocation(r).getY();
		}
		
		center = new NdPoint(Ax/n, Ay/n);
		return center;
	}

	public boolean isOccupied() {
		
		for (Road road : roads) {
			if(road.isOccupied()) {
				return true;
			}
		}
		return false;
	}
	
	public void addEdgeRoad(Road road) {
		edgeRoads.add(road);
	}

	public void addRoad(Road road) {
		roads.add(road);
	}
}
