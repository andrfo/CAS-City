package citySim.agent;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public abstract class Agent {

	
	
	
	protected ContinuousSpace<Object> space;
	protected Grid<Object> grid;
	public Agent(ContinuousSpace<Object> space, Grid<Object> grid) {
		super();
		this.space = space;
		this.grid = grid;
		
	}
	
	
}
