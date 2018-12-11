package citySim.agent;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Bus extends Vehicle{

	public Bus(ContinuousSpace<Object> space, Grid<Object> grid, int occupantLimit) {
		super(space, grid, occupantLimit);
		// TODO Auto-generated constructor stub
	}
	
	
	public Double getCost() {
		return 100d;
	}

}
