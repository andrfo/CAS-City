package environment;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Substation extends Entity{

GridPoint location; //Location of the top-right GridPoint
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public Substation(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
	}
	
	
	
	/**
	 * Runs every step
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		
		System.out.println("running");
	}
}
