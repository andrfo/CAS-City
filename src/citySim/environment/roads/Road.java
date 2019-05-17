package citySim.environment.roads;

import java.util.List;

import citySim.agent.Vehicle;
import citySim.environment.Entity;
import citySim.environment.Junction;
import citySim.environment.Roundabout;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import utils.Tools;
import utils.Vector2D;

public class Road extends Entity{
	
	
	private Junction junction;
	private Roundabout roundabout;
	private boolean isEdge;
	private GridPoint pt;
	private Double weight;
	
	private boolean isExit;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Road(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.isEdge = false;
		this.isExit = false;
	}
	
//	@Watch(
//			watcheeClassName = "citySim.agent.Car", 
//			watcheeFieldNames = "moved",
//			query = "colocated",
//			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
//	@ScheduledMethod
//	public void onTriggerEnter() {
//		
//		pt = grid.getLocation(this);
//		
//		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
//			if(obj instanceof Car) {
//				Car c = (Car)obj;
//				c.addVisited(this);//TODO:have in car instead
//				if(isEdge && !(this instanceof RoundaboutRoad)) {
//					if(!isExit) {
//						c.setInQueue(true);							
//					}
//				}				
//			}
//		}
//	}
	
	
	
	public boolean isExit() {
		return isExit;
	}
	
	public Double getWeight() {
		return weight;
	}
	
	public int getIntWeight() {
		return (int) Math.round(weight);
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Roundabout getRoundabout() {
		return roundabout;
	}

	public void setRoundabout(Roundabout roundabout) {
		this.roundabout = roundabout;
		if(this instanceof RoundaboutRoad) {
			return;
		}
		
		GridPoint pt = grid.getLocation(this);
		
		GridCellNgh<Road> nghCreator = new GridCellNgh<Road>(grid, pt, Road.class, 1, 1);
		List<GridCell<Road>> gridCells = nghCreator.getNeighborhood(false);
		Road closestRoad = null;
		Double minDist = Double.MAX_VALUE;
		Double distance = 0d;
		for (GridCell<Road> cell : gridCells) {
			if(cell.size() <= 0) {
				continue;
			}
			Road r = cell.items().iterator().next();
			distance = Tools.gridDistance(this.getLocation(), r.getLocation());
			if(
					distance < minDist &&
					r instanceof RoundaboutRoad) {
				minDist = distance;
				closestRoad = r;
			}
		}
		
		int dir = Tools.getMooreDirection(grid.getLocation(closestRoad), grid.getLocation(this));
		if(this instanceof NorthEastRoad) {
			if(		dir == Tools.NORTHWEST ||		// points:
					dir == Tools.NORTH	 ||			// x x x
					dir == Tools.NORTHEAST ||		// 0 0 x
					dir == Tools.EAST		 ||		// 0 0 x
					dir == Tools.SOUTHEAST) {
				isExit = true;
			}
		}
		else if(this instanceof SouthWestRoad) {
			if(		dir == Tools.NORTHWEST ||		// points:
					dir == Tools.WEST		 ||		// x 0 0
					dir == Tools.SOUTHWEST ||		// x 0 0
					dir == Tools.SOUTH	 ||			// x x x
					dir == Tools.SOUTHEAST) {
				isExit = true;
			}
		}
	}
	
	public Junction getJunction() {
		return junction;
	}
	
	public boolean isEdge() {
		return isEdge;
	}
	
	public void setJunctionEdge(boolean isJunctionEdge) {
		this.isEdge = isJunctionEdge;
	}
	
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}
	
	public Vehicle getCar() {
		pt = grid.getLocation(this);
		
		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
			if(obj instanceof Vehicle) {	
				Vehicle c = (Vehicle)obj;
				return c;
			}
		}
		return null;
	}
	
	public boolean isOccupied() {
		pt = grid.getLocation(this);
		
		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
			if(obj instanceof Vehicle) {	
				return true;
			}
		}
		return false;
	}
	
	
	
	
	
	
	
	
	
}
