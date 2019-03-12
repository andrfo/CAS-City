package environment;

import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

/**
 * Entities are fixed geography and stuff
 * @author andrfo
 *
 */

public abstract class ElectricEntity {
	
	
	//List<Entity> entities;
	
	GridPoint location; //Location of the top-right GridPoint
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public ElectricEntity(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}
	
	/**
	 * Runs every step
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		
		
	}

}
