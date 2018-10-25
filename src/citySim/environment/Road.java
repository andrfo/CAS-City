package citySim.environment;

import java.util.List;


import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Road extends Entity{
	
	private Road next; // Next road segment
	private Road preceding; // The preceding Road segment
	private Road nextLane; // The other lane. There are no more than 2 lanes currently
	private String type;
	
	private int x;
	private int y;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public Road(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		// get the grid location of this
		GridPoint pt = grid.getLocation(this);
		
	}
	
	
	
	
	
	
	
}
