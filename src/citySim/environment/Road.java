package citySim.environment;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Road extends Entity{
	
	private Road next; // Next road segment
	private Road preceding; // The preceding Road segment
	private Road nextLane; // The other lane. There are no more than 2 lanes currently
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Road(GridPoint location, ContinuousSpace<Object> space, Grid<Object> grid) {
		super(location, space, grid);
		// TODO Auto-generated constructor stub
	}
	
	
	
}
