package citySim.agent;


import java.util.List;

import citySim.environment.roads.Road;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Car extends Vehicle{
	
	
	

	public Car(ContinuousSpace<Object> space, Grid<Object> grid, int occupantLimit, List<Road> parkingNexi) {
		super(space, grid, occupantLimit, parkingNexi);
		
		// TODO Auto-generated constructor stub
	}
	
	public Double getDistanceMoved() {
		return distanceMoved;
	}
	
	/**
	 * Not used as the toll is constant for each trip at the moment
	 * @return
	 */
	public Double getTollCost() {
		//TODO: Add Tolls
		return 1d; // set to 1 for now as it is a product and is being ignored
	}
}
