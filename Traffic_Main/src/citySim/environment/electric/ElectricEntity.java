package citySim.environment.electric;


import citySim.environment.Entity;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

/**
 * Entities are fixed geography and stuff
 * @author andrfo
 *
 */

public abstract class ElectricEntity extends Entity{
	
	
	//List<Entity> entities;
	protected String debugString = "";
	protected ElectricEntity parent;
	protected GridPoint location; //Location of the top-right GridPoint
	protected Double baseLoad;
	protected Double unitLoad;
	protected Double totalLoad;
	
	private ContinuousSpace<Object> space;
	protected Grid<Object> grid;
	public ElectricEntity(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.parent = null;
	}
	
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}
	
	public void setParent(ElectricEntity parent) {
		this.parent = parent;
	}
	
	public void setChange(Double oldValue, Double newValue) {
		Double old = Double.valueOf(totalLoad);
		totalLoad += newValue - oldValue;
		onChange(old, Double.valueOf(totalLoad));
	}
	
	public void onChange(Double oldValue, Double newValue) {
		if(parent != null) {
			parent.setChange(oldValue, newValue);
		}
	}
	
	public String hasParent() {
		if(parent != null) {
			debugString = "x";
		}
		else {
			debugString = "O";
		}
		return debugString;
	}

	public void init() {
		onChange(0d, totalLoad);
	}
	
	public String getLoad() {
		return Double.toString(totalLoad);
	}
}
