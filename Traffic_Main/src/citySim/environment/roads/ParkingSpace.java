package citySim.environment.roads;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class ParkingSpace extends Road {

	public static final int DEFAULT_TIME = 90;
	private boolean isReserved = false;
	
	public ParkingSpace(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		// TODO Auto-generated constructor stub
	}
	
	public boolean reserve() {
		if(isReserved) {return false;}
		else {
			isReserved = true;
			return true;
		}
	}
	
	public void vacate() {
		isReserved = false;
	}
	
	public boolean isReserved() {
		return isReserved;
	}

}
