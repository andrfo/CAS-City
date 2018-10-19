package citySim.environment;

import java.util.List;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Intersection extends Entity {
	private List<Road> roads;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Intersection(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		// TODO Auto-generated constructor stub
	}

	
}
