package citySim.environment;

import java.util.ArrayList;
import java.util.List;

import citySim.agent.Car;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Junction extends Entity {
	private List<Road> edgeRoads;
	private List<Road> roads;
	private List<Car> queue; 
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Junction(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.edgeRoads = new ArrayList<Road>();
		this.roads =  new ArrayList<Road>();
		this.queue = new ArrayList<Car>();
		// TODO Auto-generated constructor stub
	}

	public void addCar(Car car) {
		if(!queue.contains(car)) {
			queue.add(car);
			car.setInQueue(true);
		}
	}
	
	private void activate(Car car) {
		car.setInQueue(false);
		queue.remove(car);
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		if(isOccupied()) {
			return;
		}
		
		//Random car goes first for now
		//TODO: Traffic rules
		int s = queue.size();
		if(s > 0) {
			int index = RandomHelper.nextIntFromTo(0, s - 1);
			Car c = queue.get(index);
			queue.remove(c);
			activate(c);
		}
		
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

	public List<Road> getRoads() {
		return roads;
	}

	public void addRoad(Road road) {
		roads.add(road);
	}
	
	public void connectEdgeRoads() {
		//TODO
	}
	
}
