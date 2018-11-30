package citySim.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


import citySim.environment.*;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import utils.Tools;
import utils.Vector2D;

public class Car extends Agent{


	/**
	 * TODO:local naviation: 
	 * 		large grid neighbourhood that detects the surrounding area
	 * 		This can also be a tweakable variable!
	 * TODO: remove cars from memory:
	 * 		Seems the cars are not being removed from the memory and we are running out of memory.
	 * 			- Memory leak
	 * 			- Force garbage collector to collect cars?
	 * 			- Remove all references of car and set them to null when dead
	 * TODO: use floyd-warshall algorithm to calc shortest path in local navigation instead of A*
	 * 
	 */
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private Road globalGoal;
	private Road start;
	private Road localGoal;
	private Road currentRoad;
	
	private int viewDistance = 8;
	
	private List<RepastEdge<Object>> path;
	private Network<Object> net;
	
	private double pathIndex;
	private boolean moved;
	private boolean isInQueue;
	private Vector2D direction;
	private boolean isInJunction;
	private boolean dead = false;
	
	//Speed control
	private double speed;
	private double maxSpeed;
	
	public double thresholdStop = 1.6;
	public double thresholdDecelerate = 2;
	public double thresholdAccelerate = 3;
	
	public double forceDecelerate = 0.2;
	public double forceAccelerate = 0.2;
	
	private String debugString = "";
	
	
	private List<Road> open;
	private List<Road> closed;
	
	public Car(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.grid = grid;
		this.space = space;
		this.pathIndex = 0;
		this.speed = maxSpeed = 0.5 + RandomHelper.nextDouble();
		this.open = new ArrayList<Road>();
		this.closed = new ArrayList<Road>();
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		if(isInQueue || dead) {
			return;
		}
		
		isReachedGlobalGoal();
		if(dead) { return;}
		getSurroundings();
		selectNewLocalGoal();
		if(dead) { return;}
		move();	
	}
	
	private void move() {
		// get the grid location of this Agent
		GridPoint pt = grid.getLocation(currentRoad);
		
		//Adjust speed
		speedControl();	
		
		//Follow path
		if(pathIndex <= path.size() - 1) {
			selectNewLocalGoal();
		}	
		if(pathIndex <= path.size() - 1) {
			int index = (int) Math.ceil(pathIndex);
			GridPoint next = grid.getLocation((Road)path.get(index).getTarget());
			direction = Tools.create2DVector(pt, next);
			moveTowards(next);
			
		}
		else {
			
			//Goal reached, die
			//die("Path ended, car dies");
		}
		
		//================================
	}
	
	private boolean isReachedGlobalGoal() {
		
		GridPoint pt = grid.getLocation(this);
		if(Tools.distance(pt, grid.getLocation(globalGoal)) < 2) {
			die("Arrived at global goal, car dies");
			return true;
		}
		return false;
	}
	
	private void selectNewLocalGoal() {
		debugString = "";
		//TODO: account for other cars
		debugRemoveEdges(this);
		//Get the surrounding roads and add new roads
		
		if(open.size() == 0) {
			die("no new goal");
			return;
		}
		
		//Pick the road within view that is closest to goal
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		for (Road road : open) {
			dist = Tools.distance(grid.getLocation(globalGoal), grid.getLocation(road));
			if(dist < minDist && !(road instanceof Spawn)) {
				localGoal = road;
				minDist = dist;
			}
		}
		open.remove(localGoal);
		
		path = Tools.aStar(currentRoad, localGoal, net);
		if(path.size() == 0) {
			
			debugPointTo(globalGoal);
			debugPointTo(localGoal);
			DecimalFormat df = new DecimalFormat("####0.0");
			if(path != null) {
				debugString = df.format(speed) + "; " + path.size();			
			}
			
			
//			System.out.println(
//					"Path is empty. currentRoad: (" + 
//			currentRoad.getLocation().getX() + 
//			", " + 
//			currentRoad.getLocation().getY() +
//			") localGoal: (" + 
//			localGoal.getLocation().getX() + 
//			", " + 
//			localGoal.getLocation().getY() +
//			")" + 
//			" Car: (" + 
//			grid.getLocation(this).getX() + 
//			", " + 
//			grid.getLocation(this).getY() +
//			")");
		}
		pathIndex = 0;			
		
	}
	
	private void getSurroundings() {
		
		GridPoint pt = grid.getLocation(this);
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		GridCellNgh<Road> roadNghCreator = new GridCellNgh<Road>(grid, pt, Road.class, viewDistance, viewDistance);
		List<GridCell<Road>> roadGridCells = roadNghCreator.getNeighborhood(true);
		for (GridCell<Road> gridCell : roadGridCells) {
			if(gridCell.items().iterator().hasNext()) {
				Road r = gridCell.items().iterator().next();
				addOpen(r);
				dist = Tools.distance(pt, grid.getLocation(r));
				if(dist < minDist) {
					minDist = dist;
					currentRoad = r;
				}
			}
		}
	}
	
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if(!pt.equals(grid.getLocation(this))) {
			
			
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			
			double dx = otherPoint.getX() - myPoint.getX();
			double dy = otherPoint.getY() - myPoint.getY();
			
			double distance = Math.sqrt(dx*dx + dy*dy);
			
			double distanceToMove;
			if(distance >= speed) {
				distanceToMove = speed;
			}
			else {
				distanceToMove = distance;
			}

			pathIndex = pathIndex + distanceToMove;
			
			space.moveByVector(this, distanceToMove, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
			moved = true;
		}
	}
	
	public void die(String message) {
		Context<Object> context = ContextUtils.getContext(this);
//		System.out.println(message);
		try {
			context.remove(this);			
		}
		catch (NullPointerException e) {
			System.out.println("Tried to kill a dead car.");
			// TODO: handle exception
		}
		dead = true;
	}
	
	public Road getRoad() {
		
		if(currentRoad == null) {
			getSurroundings();
		}
		if(currentRoad != null) {
			return currentRoad;
		}
		System.out.println("road is null");
		return null;
	}
	
	public String debugLabel() {
		return debugString;
	}
	
	
	
	
	
	private void speedControl() {
		
		GridPoint pt = grid.getLocation(this);
		
		int pathDistance = 3;
		
		Double minDist = Double.MAX_VALUE;
		GridCellNgh<Agent> agentNghCreator = new GridCellNgh<Agent>(grid, pt, Agent.class, 3, 3);
		List<GridCell<Agent>> agentGridCells = agentNghCreator.getNeighborhood(false);
		
		
		
		for (GridCell<Agent> cell : agentGridCells) {
			if(cell.size() <= 0) {
				continue;
			}
			for(Agent a : cell.items()) {
				Car c = (Car)a;
				if(!isInPath(c, pathDistance) || !Tools.isPathIntersect(this, c, 3)) {
					continue;
				}
				double dist = Tools.distance(cell.getPoint(), grid.getLocation(this));
				if(dist < minDist) {
					minDist = dist;		
				}
			}
		}
		if(isInJunction) {
			minDist = Double.MAX_VALUE;
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
	
	public void addVisited(Road r) {
		if(!closed.contains(r)) {
			closed.add(r);
			open.remove(r);
		}
	}
	
	/**
	 * Adds a road to the open list if it has not been seen before by this car
	 * @param r
	 */
	public void addOpen(Road r) {
		if(!open.contains(r) && !closed.contains(r)) {
			open.add(r);
		}
	}
	
	private boolean isInPath(Car car, int pathDistance) {
		int counter = 0;
		for(RepastEdge<Object> edge : path) {
			if(counter > pathDistance) {
				break;
			}
			counter++;
			Road r = (Road) edge.getTarget();
			if(r.getCar() == car) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isBehind(Car c) {
		if(this.direction == null) {
			return true;
		}
		Vector2D diff = Tools.create2DVector(grid.getLocation(c), grid.getLocation(this));
		double angle = direction.angle(diff);
		
		if(angle < Math.PI/2) {
			return true;
		}
		return false;
	}
	
	private boolean isSameWay(Car c) {
		Road cR = c.getRoad();
		Road tR = this.getRoad();
		if(cR instanceof RoundaboutRoad) {
			return false;
		}
		
		return cR.getClass() == tR.getClass();
	}
	
	public Vector2D getDirection() {
		return direction;
	}
	
	public void setInQueue(boolean isInQueue) {
		this.isInQueue = isInQueue;
		if(isInQueue) {
			stop();
		}
		else {
			setSpeed(maxSpeed);
			step();
		}
	}
	
	
	
	public boolean isInQueue() {
		return isInQueue;
	}

	public void setGoal(Road goal) {
		this.globalGoal = goal;
	}
	
	public void debugPointTo(Object obj) {
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> net = (Network<Object>)context.getProjection("debug network");
		net.addEdge(this, obj);
		
	}
	
	public void debugRemoveEdges(Object obj) {
		Context<Object> context = ContextUtils.getContext(obj);
		Network<Object> net = (Network<Object>)context.getProjection("debug network");
		for(RepastEdge<Object> edge : net.getEdges(this)) {
			net.removeEdge(edge);
		}
	}
	
	public void setStart(Road start) {
		this.start = start;
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

	public List<RepastEdge<Object>> getPath() {
		return path;
	}

	public void setNet(Network<Object> net) {
//		int size = 0;
//		for(RepastEdge<Object> edge: net.getEdges()) {
//			size++;
//		}
//		System.out.println("Net set. Nodes: " + size);
		this.net = net;
	}
	
	
	
}
