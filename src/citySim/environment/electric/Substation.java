package citySim.environment.electric;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Substation extends ElectricEntity{

GridPoint location; //Location of the top-right GridPoint
	
	public Substation(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.totalLoad = 0.01;
		this.grid = grid;
		this.space = space;
	}
	
	
}
