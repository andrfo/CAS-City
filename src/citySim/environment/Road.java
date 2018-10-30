package citySim.environment;

import java.util.List;

import citySim.agent.Car;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Road extends Entity{
	
	
	private String type;
	private Junction junction;
	
	
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public Road(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
	}
	@Watch(
			watcheeClassName = "citySim.agent.Car", 
			watcheeFieldNames = "moved",
			query = "colocated",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void trigger() {
		if(junction != null) {
			GridPoint pt = grid.getLocation(this);
			Car c = (Car) grid.getObjectAt(pt.getX(), pt.getY());
			junction.addCar(c);
		}
		
		
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Junction getJunction() {
		return junction;
	}
	public void setJunction(Junction junction) {
		this.junction = junction;
	}
	
	
	
	
	
	
	
	
	
}
