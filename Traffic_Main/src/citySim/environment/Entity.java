package citySim.environment;

import java.util.List;


import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

/**
 * Entities are fixed geography and stuff
 * @author andrfo
 *
 */

public abstract class Entity {
	
	
	//List<Entity> entities;
	
	GridPoint location; //Location of the top-right GridPoint
	
	private ContinuousSpace<Object> space;
	protected Grid<Object> grid;
	public Entity(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	
	
	//TODO: add location: gridpoint
	
	
	//add @watch function?

}
