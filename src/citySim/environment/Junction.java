package citySim.environment;

import java.util.ArrayList;
import java.util.List;

import citySim.agent.Car;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Junction extends Entity {
	private List<Road> edgeRoads;
	private List<Car> queue; 
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Junction(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.edgeRoads = new ArrayList<Road>();
		// TODO Auto-generated constructor stub
	}

	public void addCar(Car car) {
		if(!queue.contains(car)) {
			queue.add(car);
			car.setAtJunction(true);
		}
	}
	
	private void activate(Car car) {
		car.setAtJunction(false);
		queue.remove(car);
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		
		
		//pick a car from queue and send it to the right road
	}
	
	public void addEdgeRoad(Road road) {
		edgeRoads.add(road);
	}
}
