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
	
	
	protected ElectricEntity parent;
	protected ContinuousSpace<Object> space;
	protected Grid<Object> grid;
	protected Double totalLoad;
	public ElectricEntity(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.totalLoad = 0.01;
		this.grid = grid;
		this.space = space;
	}
	
	
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}
	
	public void setParent(ElectricEntity parent) {
		this.parent = parent;
	}
	
	public void update(Double delta) {
		this.totalLoad += delta;
		updateParent(delta);
	}
	
	public void updateParent(Double delta) {
		if(parent != null) {
			this.parent.update(delta);
		}
	}

	public void init() {
		updateParent(totalLoad);
	}
	
	public String getLoad() {
		return Double.toString(Math.round(totalLoad));
	}
}
