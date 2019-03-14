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
	
	protected ElectricEntity parent;
	protected GridPoint location; //Location of the top-right GridPoint
	protected Double baseLoad;
	protected Double unitLoad;
	protected Double totalLoad;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public ElectricEntity(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}
	
	public void setParent(ElectricEntity parent) {
		this.parent = parent;
	}
	
	public void setChange(Double oldValue, Double newValue) {
		totalLoad += newValue - oldValue;
	}
	
	public void onChange(Double oldValue, Double newValue) {
		if(parent != null) {
			parent.setChange(oldValue, newValue);
		}
	}

	public void init() {
		onChange(0d, totalLoad);
	}
	
	public String getLoad() {
		return Double.toString(totalLoad);
	}
}
