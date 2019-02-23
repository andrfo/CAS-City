package citySim.agent;


import java.util.List;

import citySim.environment.Road;
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
	
	public Double getTollCost() {
		//TODO: Add Tolls
		return 12d;
	}
}
