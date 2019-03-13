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
	protected Double baseCost;
	protected Double unitCost;
	protected Double totalCost;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public ElectricEntity(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
		this.totalCost = baseCost;
	}
	
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}
	
	public void setParent(ElectricEntity parent) {
		this.parent = parent;
	}
	
	public void setChange(Double oldValue, Double newValue) {
		totalCost += newValue - oldValue;
	}
	
	public void onChange(Double oldValue, Double newValue) {
		parent.setChange(oldValue, newValue);
	}

}
