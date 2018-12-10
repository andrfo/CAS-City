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
import repast.simphony.util.SimUtilities;
import utils.Tools;
import utils.Vector2D;

public class Vehicle extends Agent{

	
	
	private int occupantLimit = 5;
	
	private List<Person> occupants;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private List<Entity> goals;
	private Road start;
	private Road localGoal;
	private Road currentRoad;
	private ParkingSpace parkingSpace;
	
	private int viewDistance = 8;
	
	private List<RepastEdge<Object>> path;
	private Network<Object> net;
	
	private double pathIndex;
	private boolean moved;
	private boolean isInQueue;
	private Vector2D direction;
	private boolean isInJunction;
	private boolean dead = false;
	private boolean parked;
	private boolean goingToWork;
	private boolean hasRightOfWay = false;
	
	private int rightOfWayCounter = 0;
	private int rightOfWayTime = 3;
	private Vehicle blockingCar;
	
	private int deadlockTimer;
	private int parkedTimer;
	
	//Speed control
	private double speed;
	private double maxSpeed;
	
	public double thresholdStop = 1.6;
	public double thresholdDecelerate = 2;
	public double thresholdAccelerate = 3;
	
	public double forceDecelerate = 0.2;
	public double forceAccelerate = 0.2;
	
	public String debugString = "";
	
	protected double distanceMoved = 0;
	
	
	private List<Road> open;
	private List<Road> closed;
	private int deadlockTime = 8;
	
	public Vehicle(ContinuousSpace<Object> space, Grid<Object> grid, int occupantLimit) {
		super(space, grid);
		this.occupantLimit = occupantLimit;
		this.grid = grid;
		this.space = space;
		this.pathIndex = 0;
		this.speed = maxSpeed = 0.5 + RandomHelper.nextDouble();
		this.goals = new ArrayList<Entity>();
		this.open = new ArrayList<Road>();
		this.closed = new ArrayList<Road>();
		this.parked = false;
		this.parkedTimer = 0;
		this.goingToWork = false;
		this.blockingCar = null;
		this.deadlockTimer = deadlockTime;
		this.moved = true;
		this.occupants = new ArrayList<Person>(occupantLimit);
	}
	
	/**
	 * Runs every step
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		
		getSurroundings(); 		if(!isMovable()) {return;}
		
		isReachedGoal(); 		if(dead) { return;}
		
		selectNewLocalGoal(); 	if(dead) { return;}
		
		move();	
	}
	
	//Main functions
	
	private void getSurroundings() {
		
		if(!moved) {
			return;
		}
		
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
		if(currentRoad.isEdge() && !currentRoad.isExit()) {
			setInQueue(true);
		}
	}
	
	private boolean isMovable() {
		if(parked) {
			if(parkedTimer > 0) {
				parkedTimer--;
				moved = false;
				return false;
			}
			else {
				parked = false;
				parkingSpace.vacate();
				parkingSpace = null;
				setSpeed(maxSpeed);
				gatherOccupants();
			}
		}
		if(isInQueue) {
			if(isClear(this)) {
				setInQueue(false);
			}
			else {
				moved = false;
				return false;				
			}
		}
		return true;
	}
	
	private boolean isReachedGoal() {
		
		GridPoint pt = grid.getLocation(this);
		Entity goal = goals.get(0);
		double triggerDistance;
		
		if(goal instanceof Building) {
			triggerDistance = 10;
		}
		else {
			triggerDistance = 2;
		}
		if(Tools.distance(pt, grid.getLocation(goal)) < triggerDistance) {
			
			
			if(goal instanceof Building) {
				ParkingSpace p = findParking(grid.getLocation(goal));
				
				if(p == null) {
					System.out.println("Did not find parking for building.");
					return false;
				}
				goals.remove(goal);
				goals.add(0, p);
				goingToWork = true;					
				
			}
			else if (goal instanceof ParkingSpace) {
				if(((ParkingSpace) goal).isReserved()) {
					System.out.println("taken");
					ParkingSpace p = findParking(grid.getLocation(this));
					if(p == null) {
						//TODO: don't die when no parking is available
						die("No parking");
					}
					goals.remove(goal);
					goals.add(0, p);
					return false;
				}
				stop();
				space.moveTo(this, space.getLocation(goal).getX(), space.getLocation(goal).getY());
				grid.moveTo(this, pt.getX(), pt.getY());
				for(Person p : occupants) {
					p.setReachedGoal(this, false);
				}
				park(480, (ParkingSpace) goals.get(0));//8h
				goals.remove(0);
				
				closed.clear();
				open.clear();
				getSurroundings();
			}
			else if (goal instanceof Despawn) {
				for(Person p : occupants) {
					p.setReachedGoal(this, true);
				}
				die("");
				return true;
			}
			else{
				die("Unknown Goal");
				return true;
			}
		}
		return false;
	}
	
	private void selectNewLocalGoal() {
		
		if(open.size() == 0) {
			die("no new goal");
			return;
		}
		
		//Do not calculate new route if in a roundabout
		if((	currentRoad instanceof RoundaboutRoad ||
				currentRoad.isEdge()) &&
				path.size() > 1 &&
				Math.ceil(pathIndex) < path.size() ) {
			return;
		}
		
		Entity goal = goals.get(0);
		
		//Pick the road within view that is closest to goal
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		for (Road road : open) {
			dist = Tools.distance(grid.getLocation(goal), grid.getLocation(road));
			if(dist < minDist && !(road instanceof Spawn)) {
				if(road instanceof ParkingSpace && road != goal) {
					continue;
				}
				localGoal = road;
				minDist = dist;
			}
		}
		
		path = Tools.aStar(currentRoad, localGoal, net);
		pathIndex = 0;			
		
	}
	
	private void move() {
		// get the grid location of this Agent
//		GridPoint pt = grid.getLocation(currentRoad);
		
		
		
		//Adjust speed
		speedControl();	
		
		//Follow path
		int index = (int) Math.ceil(pathIndex);
		if(index > path.size() - 1) {
			selectNewLocalGoal();
		}	
		else{
			GridPoint next = grid.getLocation((Road)path.get(index).getTarget());
			
			boolean s = moveTowards(next);
			addVisited((Road)path.get(index).getTarget());
			if(!s) {
				pathIndex += 1;
				move();
			}
			
		}
		moved = true;
	}
	
	//===========================
	
	/**
	 * removes the persons that are bound to this car from the context.(They get back in the car)
	 */
	private void gatherOccupants() {
		@SuppressWarnings("unchecked")
		Context<Object> context = ContextUtils.getContext(this);
		for(Person p : occupants) {
			context.remove(p);
		}
	}
	
	
	public boolean moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if(pt.equals(grid.getLocation(currentRoad))) {
			return false;
		}
		//current and target
		NdPoint myPoint = space.getLocation(this);
		NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
		
		//Movement Geometry
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

		pathIndex += distanceToMove;
		
		distanceMoved += distanceToMove;
		
		space.moveByVector(this, distanceToMove, angle, 0);
		myPoint = space.getLocation(this);
		grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
		moved = true;
		return true;
	}
	
	private void speedControl() {
		
		GridPoint pt = grid.getLocation(currentRoad);
		
		int pathDistance = 3;
		
		Double minDist = Double.MAX_VALUE;
		GridCellNgh<Agent> agentNghCreator = new GridCellNgh<Agent>(grid, pt, Agent.class, 3, 3);
		List<GridCell<Agent>> agentGridCells = agentNghCreator.getNeighborhood(false);
		
		
		
		for (GridCell<Agent> cell : agentGridCells) {
			if(cell.size() <= 0) {
				continue;
			}
			for(Agent a : cell.items()) {
				Vehicle c = (Vehicle)a;
				if(c.isParked() || c.getRoad() instanceof ParkingSpace) {continue;}
				if(
						!isInPath(c, pathDistance) || 
						!Tools.isPathIntersect(this, c, pathDistance)) {
					continue;
				}
				double dist = Tools.distance(cell.getPoint(), grid.getLocation(this));
				if(dist < minDist) {
					minDist = dist;	
					blockingCar = c;
				}
			}
		}
		if(minDist <= thresholdStop) {
			checkDeadlock();
		}
		if(hasRightOfWay) {
			if ((currentRoad instanceof RoundaboutRoad) || currentRoad.isEdge()) {
				minDist = Double.MAX_VALUE;
				blockingCar = null;
			}
			else {
				hasRightOfWay = false;
			}
			
		}
		if(minDist <= thresholdStop) {
			stop();
		}
		else if(minDist >= thresholdAccelerate) {
			blockingCar = null;
			accelerate(minDist);
		}
		else if(minDist <= thresholdDecelerate) {
			blockingCar = null;
			descelerate(minDist);
		}
		
//		DecimalFormat df = new DecimalFormat("####0.0");
//			debugString += " " + df.format(speed) + " ";			
	}

	private void stop() {
		speed = 0;
	}
	
	private void descelerate(Double minDist) {
//		debugString += " B ";
		if(speed >= forceDecelerate) {
			speed -= forceDecelerate;			
		}
		else {
			speed = 0;
		}
		if(speed >= minDist + 0.5) {
			speed = minDist;
		}
	}
	
	private void accelerate(Double minDist) {
//		debugString += " A ";
		if(speed <= (maxSpeed - forceAccelerate) ){
			speed += forceAccelerate;
		}
		else {
			speed = maxSpeed;
		}
		if(speed >= minDist + 0.5) {
			speed = minDist;
		}
	}
	
	public void die(String message) {
		@SuppressWarnings("unchecked")
		Context<Object> context = ContextUtils.getContext(this);
		System.out.println(message);
		occupants.clear();
		try {
			context.remove(this);			
		}
		catch (NullPointerException e) {
			System.out.println("Tried to kill a dead car.");
			// TODO: handle exception
		}
		dead = true;
	}
	
	private void checkDeadlock() {
		
		if(deadlockTimer > 0) {
			if(currentRoad instanceof RoundaboutRoad) {
				deadlockTimer--;
			}
			else {
				deadlockTimer = deadlockTime;
			}
		}
		else {
			giveWay();
		}
		
		
		if(		blockingCar.getBlockingCar() != null &&
				blockingCar != null) {
//			if(blockingCar.getBlockingCar() == this) {
//				debugString = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
//				blockingCar.giveWay();	
//			}
			int counter = 0;
			Vehicle b = blockingCar;
			while(b != null && counter < 10) {
				if(b.getBlockingCar() == this) {
					b.giveWay();
					break;
				}
				if(b.getBlockingCar() != null) {
					b = b.getBlockingCar();					
				}
				else {
					break;
				}
				counter++;
			}
		}
		
		
	}
	
	public void giveWay() {
		this.hasRightOfWay = true;
//		rightOfWayCounter = rightOfWayTime;
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
	
	private void park(int time, ParkingSpace p) {
		parked = true;
		this.parkingSpace = p;
		if(goingToWork) {
			parkedTimer = time;			
		}
		else {
			parkedTimer = 90;
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
	
	
	private boolean isInPath(Vehicle car, int pathDistance) {
		for(	int i = getPathIndex(); 
				i < getPathIndex() + pathDistance &&
				i < path.size() - 1; 
				i++) {
			Road r = (Road) path.get(i).getTarget();
			if(r.getCar() == car) {
				return true;
			}
		}
		return false;
//		for(RepastEdge<Object> edge : path) {
//			if(counter > pathDistance) {
//				break;
//			}
//			counter++;
//		}
	}
	
	public int getPathIndex() {
		return (int) Math.ceil(pathIndex);
	}
	
	private boolean isBehind(Vehicle c) {
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
	
	public Vehicle getBlockingCar() {
		return this.blockingCar;
	}
	
	
	public void setInQueue(boolean isInQueue) {
		this.isInQueue = isInQueue;
		if(isInQueue) {
			stop();
		}
		else {
			setSpeed(maxSpeed);
//			step();
		}
	}
	
	private boolean isClear(Vehicle c) {
		GridPoint pt = grid.getLocation(c);
				
		GridCellNgh<Vehicle> nghCreator = new GridCellNgh<Vehicle>(grid, pt, Vehicle.class, 1, 1);
		List<GridCell<Vehicle>> gridCells = nghCreator.getNeighborhood(false);
		for (GridCell<Vehicle> cell : gridCells) {
			if(cell.size() == 0) {
				continue;
			}
			Vehicle car = cell.items().iterator().next();
			if(car.getRoad() instanceof RoundaboutRoad && Tools.isPathIntersect(c, car, 3)) {
//				c.debugString += " C ";
				return false;
			}
		}
		return true;
	}
	
	private ParkingSpace findParking(GridPoint target) {
		double min = Double.MAX_VALUE;
		ParkingSpace p = null;
		for (Road road : open) {
			ParkingSpace parking;
			if(road instanceof ParkingSpace) {
				parking = (ParkingSpace) road;
				if(parking.isReserved()){
					Double distance = Tools.distance(grid.getLocation(this), target);
					if(distance < min) {
						min = distance;
						p = parking;
					}
				}
			}
		}
		int range = 8;
		while(p == null) {
			p = findRandomProximateParking(target, range);
			range += 10;
			if(range >= 10000) {
				die("cannot find parking within 10000");
			}
		}
		return p;
	}
	
	private ParkingSpace findRandomProximateParking(GridPoint target, int range) {
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<Road> roadNghCreator = new GridCellNgh<Road>(grid, pt, Road.class, range, range);
		List<GridCell<Road>> roadGridCells = roadNghCreator.getNeighborhood(true);
		SimUtilities.shuffle(roadGridCells, RandomHelper.getUniform());
		for (GridCell<Road> gridCell : roadGridCells) {
			if(gridCell.items().iterator().hasNext()) {
				Road r = gridCell.items().iterator().next();
				if(r instanceof ParkingSpace && !((ParkingSpace) r).isReserved()) {
					return (ParkingSpace) r;
				}
			}
		}
		return null;
	}

	public boolean isParked() {
		return parked;
	}
	
	public boolean isInQueue() {
		return isInQueue;
	}

	
	public void addGoal(Entity goal) {
		this.goals.add(goal);
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
	
	public boolean removeOccupant(Person p) {
		return occupants.remove(p);
	}

	public boolean addOccupant(Person p) {
		if(occupants.size() < occupantLimit) {
			this.occupants.add(p);
			return true;
		}
		return false;
	}
	
	
}
