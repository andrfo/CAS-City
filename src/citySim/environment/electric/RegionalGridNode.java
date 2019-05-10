package citySim.environment.electric;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class RegionalGridNode extends ElectricEntity{

	private ElectricEntity parent;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public RegionalGridNode(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		totalLoad = 0d;
		this.grid = grid;
		this.space = space;
	}
	
}
