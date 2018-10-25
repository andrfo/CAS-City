package citySim.agent;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public abstract class Agent {

	//Cost
	
	//LocalGoal
	
	//GlobalGoal
	
	//Location
	
	//Constructor
	
	//Interaction function
	//See what it is interacting with and handle it accordingly
	
	//Step function
	
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public Agent(ContinuousSpace<Object> space, Grid<Object> grid) {
		super();
		this.space = space;
		this.grid = grid;
		
	}
	
	
}
