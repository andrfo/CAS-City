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
import utils.Tools;
import utils.Vector2D;

public class Car extends Agent{

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private Road goal;
	private Road start;
	private List<RepastEdge<Object>> path;
	private double pathIndex;
	private boolean moved;
	private boolean isAtJunction;
	private Vector2D direction;
	
	//Speed control
	private double speed;
	private double maxSpeed;
	
	private double thresholdStop = 1.2;
	private double thresholdDecelerate = 2;
	private double thresholdAccelerate = 3;
	
	private double forceDecelerate = 0.2;
	private double forceAccelerate = 0.2;
	
	public Car(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.grid = grid;
		this.space = space;
		this.pathIndex = 0;
		this.speed = maxSpeed = 1 + RandomHelper.nextDouble();
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
		GridCellNgh<Road> roadNghCreator = new GridCellNgh<Road>(grid, pt, Road.class, 1, 1);
		List<GridCell<Road>> roadGridCells = roadNghCreator.getNeighborhood(true);
		SimUtilities.shuffle(roadGridCells, RandomHelper.getUniform());
		
		
		speedControl();	
		
		//Follow path
		if(pathIndex < path.size() - 1) {
			int index = (int) Math.floor(pathIndex);
			GridPoint next = grid.getLocation((Road)path.get(index).getTarget());
			//System.out.println("x: " + next.getX() + " y: " + next.getY() + " lenght: " + path.size());
			direction = Tools.create2DVector(pt, next);
			moveTowards(next);
			pathIndex = pathIndex + speed;
			
		}
		else {
			//Goal reached, die(for now)
			//TODO: agent life cycle
			Context<Object> context = ContextUtils.getContext(this);
			context.remove(this);
		}
		//================================
	}
	
	
	/**
	 * TODO:local naviation: 
	 * 		large grid neighbourhood that detects the surrounding area
	 * 		This can also be a tweakable variable!
	 * 
	 */
	
	
	
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if(!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, speed, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
			moved = true;
		}
	}
	private void speedControl() {
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<Agent> agentNghCreator = new GridCellNgh<Agent>(grid, pt, Agent.class, 3, 3);
		List<GridCell<Agent>> agentGridCells = agentNghCreator.getNeighborhood(false);
		
		double minDist = Float.MAX_VALUE;
		for (GridCell<Agent> cell : agentGridCells) {
			if(cell.size() <= 0) {
				continue;
			}
			Car c = (Car)cell.items().iterator().next();
			if(!isSameWay(c)) {
				continue;
			}
			double dist = Tools.distance(cell.getPoint(), grid.getLocation(this));
			if(dist < minDist && cell.size() > 0) {
				minDist = dist;		
			}
		}
		if(minDist >= thresholdAccelerate) {
			accelerate();
		}
		else if(minDist <= thresholdStop) {
			stop();
		}
		else if(minDist <= thresholdDecelerate) {
			descelerate();
		}
	}
	private void stop() {
		speed = 0;
	}
	private void descelerate() {
		if(speed >= forceDecelerate) {
			speed -= forceDecelerate;			
		}
	}
	private void accelerate() {
		if(speed <= (maxSpeed - forceAccelerate) ){
			speed += forceAccelerate;
		}
	}
	
	private boolean isSameWay(Car c) {
		Vector2D cDir = c.getDirection();
		if(cDir == null || this.direction == null) {
			return false;
		}
		double angle = direction.angle(cDir);
		
		if(angle < Math.PI/2) {
			return true;
		}
		return false;
	}
	
	
	
	

	public Vector2D getDirection() {
		return direction;
	}

	public boolean isAtJunction() {
		return isAtJunction;
	}

	/**
	 * 
	 * @param isAtJunction
	 */
	public void setAtJunction(boolean isAtJunction) {
		if(isAtJunction) {
			stop();
		}
		else {
			accelerate();
		}
		this.isAtJunction = isAtJunction;
	}

	public void setGoal(Road goal) {
		this.goal = goal;
	}

	public void setStart(Road start) {
		this.start = start;
	}
	
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		if(speed > maxSpeed) {
			this.speed = maxSpeed;
			return;
		}
		this.speed = speed;
	}
	public void setMaxSpeed(double maxSpeed) {
		this.maxSpeed = maxSpeed;
	}
	public void setPath(List<RepastEdge<Object>> list) {
		this.path = list;
	}
}
