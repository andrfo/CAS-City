package citySim.agent;

import java.util.List;

import citySim.environment.Road;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Car extends Agent{

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private Road goal;
	private Road start;
	private List<RepastEdge<Object>> path;
	int pathIndex;
	
	public Car(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.grid = grid;
		this.space = space;
		this.pathIndex = 0;
		// TODO Auto-generated constructor stub
	}
	public List<RepastEdge<Object>> getPath() {
		return path;
	}
	public void setPath(List<RepastEdge<Object>> list) {
		this.path = list;
	}
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		if(goal == null) {
			return;
		}
		// get the grid location of this Agent
		GridPoint pt = grid.getLocation(this);
		
		//use the GridCellNgh class to create GridCells 
		// for the surrounding neighbourhood.
		GridCellNgh<Road> nghCreator = new GridCellNgh<Road>(grid, pt, Road.class, 1, 1);
		
		// import repast . simphony . query . space . grid . GridCell
		List<GridCell<Road>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		/*
		GridPoint pointNearestGoal = null;
		float minDist = Float.MAX_VALUE;
		for (GridCell<Road> cell : gridCells) {
			float dist = distance(cell.getPoint(), grid.getLocation(goal));
			if(dist < minDist && cell.size() > 0) {
				pointNearestGoal = cell.getPoint();			
			}
		}
		*/
		if(pathIndex < path.size() - 1) {
			GridPoint next = grid.getLocation((Road)path.get(pathIndex).getTarget());
			//System.out.println("x: " + next.getX() + " y: " + next.getY() + " lenght: " + path.size());
			moveTowards(next);
			pathIndex++;
		}
		else {
			Context<Object> context = ContextUtils.getContext(this);
			context.remove(this);
		}
	}
	
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if(!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
		}
	}

	public Road getGoal() {
		return goal;
	}

	public void setGoal(Road goal) {
		this.goal = goal;
	}

	public Road getStart() {
		return start;
	}

	public void setStart(Road start) {
		this.start = start;
	}
	
	private float distance(GridPoint a, GridPoint b) {
		float dx = a.getX() - b.getX();
		float dy = a.getY() - b.getY();
		return (float) Math.sqrt(dx*dx + dy*dy);
		
	}
	
	
	

}
