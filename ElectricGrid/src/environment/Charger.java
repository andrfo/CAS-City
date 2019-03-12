package environment;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Charger extends Entity{

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public Charger(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
	}
}
