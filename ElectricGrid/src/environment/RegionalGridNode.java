package environment;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class RegionalGridNode extends ElectricEntity {

	public RegionalGridNode(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		// TODO Auto-generated constructor stub
		this.totalLoad = 0.0;
	}

	
	//function for el price based on load
	//base cost per kWh
	//base cost increase with power line failures
	
}
