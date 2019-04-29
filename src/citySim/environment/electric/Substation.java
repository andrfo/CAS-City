package citySim.environment.electric;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Substation extends ElectricEntity{

GridPoint location; //Location of the top-right GridPoint
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public Substation(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.baseLoad = 0.01;//lights and the like
		this.totalLoad = baseLoad;
	}
	
	//Has small base cost
	//Has the cost of all children
//	@Override
//	public void setChange(Double oldValue, Double newValue) {
//		
//		Double old = Double.valueOf(totalLoad);
//		totalLoad += newValue - oldValue;
//		onChange(old, Double.valueOf(totalLoad));
//	}
	
	
}
