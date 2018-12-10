package citySim.agent;


import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Car extends Vehicle{
	
	
	

	public Car(ContinuousSpace<Object> space, Grid<Object> grid, int occupantLimit) {
		super(space, grid, occupantLimit);
		
		// TODO Auto-generated constructor stub
	}
	
	public Double getDistanceMoved() {
		return distanceMoved;
	}

	
	
	
	

}
