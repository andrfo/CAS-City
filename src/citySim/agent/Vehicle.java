package citySim.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import citySim.environment.*;
import citySim.environment.electric.Building;
import citySim.environment.roads.BusStop;
import citySim.environment.roads.Despawn;
import citySim.environment.roads.NorthEastRoad;
import citySim.environment.roads.ParkingSpace;
import citySim.environment.roads.Road;
import citySim.environment.roads.RoundaboutRoad;
import citySim.environment.roads.SideWalk;
import citySim.environment.roads.SouthWestRoad;
import citySim.environment.roads.Spawn;
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
import structures.Goals;
import utils.Tools;
import utils.Vector2D;

public class Vehicle extends Agent{

	
	
	private int occupantLimit = 5;
	
	protected List<Person> occupants;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	
	private Goals goals;
	
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
	private boolean lookingForParking = false;
	private int placesChecked = 0;
	private boolean goingToWork;
	private boolean hasRightOfWay = false;
	
	private int rightOfWayCounter = 0;
	private int rightOfWayTime = 3;
	private Vehicle blockingCar;
	
	private int deadlockTimer;
	
	
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
	protected int tickCount = 0;
	
	protected int scanWait = 0;
	protected int calcWait = 0;
	protected int scWaitTime = 1;
	
	private HashSet<Road> open;
	protected HashSet<Road> closed;
	private int deadlockTime = 100;

	private List<Road> parkingNexi;
	
	public Vehicle(ContinuousSpace<Object> space, Grid<Object> grid, int occupantLimit, List<Road> parkingNexi) {
		super(space, grid);
		this.occupantLimit = occupantLimit;
		this.grid = grid;
		this.space = space;
		this.pathIndex = 0;
		this.speed = maxSpeed = 0.5 + RandomHelper.nextDouble();
		this.goals = new Goals();
		this.open = new HashSet<Road>();
		this.closed = new HashSet<Road>();
		this.parked = false;
		this.goingToWork = false;
		this.blockingCar = null;
		this.deadlockTimer = deadlockTime;
		this.moved = true;
		this.occupants = new ArrayList<Person>(occupantLimit);
		this.parkingNexi = parkingNexi;
	}
	
	/**
	 * Runs every step. Checks for "dead" after each main step because model destruction does not trigger until next step.
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		//Scans the surrounding area and sees if the vehicle is able to move
		getSurroundings(); 		if(!isMovable()) {moved = false;return;}
		
		//Has the vehicle reached a goal?
		isReachedGoal(); 		if(dead) { return;}
		
		//Uses its data on past and current surounds to select a new short-term goal for navigation
		selectNewLocalGoal(); 	if(dead) { return;}
		
		//Moves one step along the path
		move();	
		
		
	}
	
	//Main functions
	
	
	/**
	 * Scans the surrounding area. Remebering parking locations and adding roads to a list of viewed roads.
	 */
	private void getSurroundings() {
		
		tickCount++; //For time measurement
		
		//Add time use to passengers
		if(this instanceof Bus) {
			for(Person p: occupants) {
				p.addTimeUse(1);
			}
		}
		
		//Does not scan if it has not moved (no new info)
		if(!moved) {
			return;
		}
		
		//Used to set a timer on the scans to save computations
		if(scanWait > 0) {
			scanWait--;
			return;
		}
		else {
			scanWait = scWaitTime;
		}
		
		
		//Sets up a grid neighborhood and iterates through it.
		GridPoint pt = grid.getLocation(this);
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		GridCellNgh<Road> roadNghCreator = new GridCellNgh<Road>(grid, pt, Road.class, viewDistance, viewDistance);
		List<GridCell<Road>> roadGridCells = roadNghCreator.getNeighborhood(true);
		for (GridCell<Road> gridCell : roadGridCells) {
			if(gridCell.items().iterator().hasNext()) {
				Road r = gridCell.items().iterator().next();
				
				//Saves the road to the list of viewed roads
				addOpen(r);
				
				dist = Tools.gridDistance(pt, grid.getLocation(r));
				
				//Selects the road a new goal if it is in parking search mode
				if(lookingForParking) {
					if(r instanceof ParkingSpace && !((ParkingSpace) r ).isReserved()) {
						goals.replaceCurrent(r);
						lookingForParking = false;
					}
				}
				
				//Sets the closest road as the road the car is currently on
				if(dist < minDist && !(r instanceof SideWalk)) {
					minDist = dist;
					currentRoad = r;
				}
			}
		}
		
		//Sets this as being in queue for an intersection if it is at the edge of one. 
		//This will trigger tests for entry into the intersection.
		if(currentRoad.isEdge() && !currentRoad.isExit()) {
			setInQueue(true);
		}
	}
	
	
	/**
	 * Tests a number of conditions to see of the vehicle is movable(Allowed/able to make a move)
	 * @return boolean, true if it can move, false of not.
	 */
	private boolean isMovable() {
		if(parked) {
			if(tickCount > 0) {
				tickCount--;
			}
			//Is the occupant done working or shopping?
			if(!occupants.get(0).isWantToLeave()) {
				return false;
			}
			//Is the way clear to exit the parking space?
			else {
				//Check surroundings
				GridPoint pt = grid.getLocation(this);
				NdPoint spacePt = space.getLocation(this);
				Double dist = Double.MAX_VALUE;
				GridCellNgh<Vehicle> agentNghCreator = new GridCellNgh<Vehicle>(grid, pt, Vehicle.class, 2, 2); //Set to a radius of 2
				List<GridCell<Vehicle>> agentGridCells = agentNghCreator.getNeighborhood(false);
				for (GridCell<Vehicle> cell : agentGridCells) {
					if(cell.items().iterator().hasNext()) {
						for(Vehicle v : cell.items()) {
							if(v == this) {
								continue;
							}
							dist = Tools.spaceDistance(space.getLocation(v), spacePt);
							if(dist <= 1.6 && !v.isParked()) { //Distance threshold set to 1.6 which seemed to work well
								//blocked, wait
								return false;
							}
						}
					}
				}
				
				//Leaves the parking space
				parked = false;
				parkingSpace.vacate();
				parkingSpace = null;
				setSpeed(maxSpeed);
				gatherOccupants();
			}
		}
		if(isInQueue) {
			//Moves if the way into the intersection is clear
			if(isClear(this)) {
				setInQueue(false);
			}
			else {
				return false;				
			}
		}
		
		//If we get there nothing has triggered and it is movable
		return true;
	}
	
	/**
	 * Checks if a global goal is reached like the work place or a parking spot.
	 * @return
	 */
	private boolean isReachedGoal() {
		
		GridPoint pt = grid.getLocation(this);//Current location
		Entity goal = goals.getCurrent();//current goal
		double triggerDistance; //The goal is defined to be reached within this distance
		
		//Buildings are triggered further away as it needs to find parking and buildings are some distance from the roads.
		if(goal instanceof Building) {
			triggerDistance = 10;
		}
		else {
			triggerDistance = 2;
		}
		if(Tools.gridDistance(pt, grid.getLocation(goal)) < triggerDistance) {
			
			//Reached the intended building, now find parking.
			if(goal instanceof Building) {
				ParkingSpace p = findParking(grid.getLocation(goal));
				
				//No parking free nearby or along the path, go looking.
				if(p == null) {
					gotoNextNexus();
					lookingForParking = true;
					return false;
				}
				goals.replaceCurrent(p);
				goingToWork = true;	//As shopper now also have buildings as targets, this may have to be removed. //TODO				
				
			}
			//Reached the intended parking space
			else if (goal instanceof ParkingSpace) {
				//is it still free? If so, find a new one
				if(((ParkingSpace) goal).isReserved()) {
					ParkingSpace p = findParking(grid.getLocation(this));
					if(p == null) {
						gotoNextNexus();
						return false;
					}
					
					goals.replaceCurrent(p);
					return false;
				}
				//Parking space is not reserved, start parking.
				
				//"Teleports" to the centre of the parking space
				stop();
				space.moveTo(this, space.getLocation(goal).getX(), space.getLocation(goal).getY());
				grid.moveTo(this, pt.getX(), pt.getY());
				for(Person p : occupants) {
					p.setReachedGoal(this, false);
				}
				park(2880, (ParkingSpace) goals.getCurrent());//8h
				goals.next(); //Sets the goal to be the next one
				
				closed.clear();
				open.clear();
				getSurroundings();
			}
			//Reached the exit of the model. Updates the measurements and destroys the vehicle
			else if (goal instanceof Despawn) {
				for(Person p : occupants) {
					p.setReachedGoal(this, true);
				}
				die("");
				return true;
			}
			
			//For buses only. Drops off and picks up passenger
			else if (goal instanceof BusStop) {
				stop();
				space.moveTo(this, space.getLocation(goal).getX(), space.getLocation(goal).getY());
				grid.moveTo(this, pt.getX(), pt.getY());
				for(Person p : occupants) {
					if(p.getNearestBusStop() == goal) {
						p.setReachedGoal(this, false);						
					}
				}
				gatherOccupants();
				goals.next();
				closed.clear();
				open.clear();
				getSurroundings();
			}
			
			//This is only a goal when looking for parking.
			else if(goal instanceof NorthEastRoad || goal instanceof SouthWestRoad) {
				parkingNexi.remove(goals.getCurrent());
				placesChecked++;
				if(placesChecked < 3) {
					gotoNextNexus();
				}
				else {
					System.out.println("cannot find parking, going home");
					goals.goToEnd();
					return false;
				}
			}
			else{
				die("Unknown Goal");
				return true;
			}
		}
		return false;
	}
	
	
	//Gets a random one of the parking nexi and sets it as a goal
	private void gotoNextNexus() {
		if(parkingNexi.size() > 0) {
			goals.replaceCurrent(parkingNexi.get(RandomHelper.nextIntFromTo(0, parkingNexi.size() - 1)));
		}
		else {
			goals.goToEnd();
		}
	}
	
	/**
	 * Scans the list of viewed roads, selects the one closest to the global goal,
	 * and calculates a path to it using A-star
	 */
	private void selectNewLocalGoal() {
		
		//Used to delay calculating a new path
		if(calcWait > 0) {
			calcWait--;
			return;
		}
		else {
			calcWait = scWaitTime;
		}
		
		//If there is no viewed roads, scan the area
		if(open.size() == 0) {
			scanWait = 0;
			moved = true;
			getSurroundings();
			if(open.size() == 0) {
				die("no new goal");
				return;
			}
		}
		
		//Do not calculate new route if in a roundabout
		if((	currentRoad instanceof RoundaboutRoad ||
				currentRoad.isEdge()) &&
				path.size() > 1 &&
				Math.ceil(pathIndex) < path.size() ) {
			return;
		}
		Entity goal = goals.getCurrent();
		
		
		//Pick the road within view that is closest to goal
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		for (Road road : open) {
			if(!isValidGoal(road, goal)) {
				continue;
			}
			dist = Tools.gridDistance(grid.getLocation(goal), grid.getLocation(road));
			if(dist < minDist) {
				localGoal = road;
				minDist = dist;
			}
		}
		
		//Create a new path, and reset the position.
		path = Tools.aStar(currentRoad, localGoal, net);
		pathIndex = 0;	
		
	}
	
	/**
	 * Moves the vehicle along the path
	 */
	private void move() {
		// get the grid location of this Agent
		// GridPoint pt = grid.getLocation(currentRoad);
		
		
		
		//Adjust speed
		speedControl();	
		
		//Follow path
		int index = (int) Math.ceil(pathIndex);
		if(index > path.size() - 1) {
			selectNewLocalGoal();
		}	
		//Move
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
			if(p.isWantToLeave()) {
				context.remove(p);				
			}
		}
	}
	
	/**
	 * Moves the vehicle towards the point (pt)
	 * @param pt, point to move towards
	 * @return boolean, true of sucessful, false if failed.
	 */
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
	
	/**
	 * Checks the surroundings for other Agents and checks whether action is needed (speed adjustment)
	 */
	private void speedControl() {
		
		//Location of this vehile
		GridPoint pt = grid.getLocation(currentRoad);
		
		int pathDistance = 3;
		
		Double minDist = Double.MAX_VALUE;
		GridCellNgh<Agent> agentNghCreator = new GridCellNgh<Agent>(grid, pt, Agent.class, 3, 3);
		List<GridCell<Agent>> agentGridCells = agentNghCreator.getNeighborhood(false);
		for (GridCell<Agent> cell : agentGridCells) {
			if(cell.size() <= 0) {
				continue;
			}
			//Iterates through the nearby agents
			for(Agent a : cell.items()) {
				if(a instanceof Person) {//Run over people, for now
					continue;
				}
				Vehicle c = (Vehicle)a;
				if(c.isParked() || c.getRoad() instanceof ParkingSpace) {continue;} //Ignore parked cars
				if(
						!isInPath(c, pathDistance) || 
						!Tools.isPathIntersect(this, c, pathDistance)) { //Ignore non-intersecting cars
					continue;
				}
				double dist = Tools.gridDistance(cell.getPoint(), grid.getLocation(this));
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
	
	public int getTickCount() {
		return tickCount;
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
	
	/**
	 * Removes the vehicle from the context and prints out the message
	 * @param message
	 */
	public void die(String message) {
		@SuppressWarnings("unchecked")
		Context<Object> context = ContextUtils.getContext(this);
		if(message.length() > 0) {
			System.out.println(message);			
		}
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
	
	/**
	 * Tries to find out if there is a deadlock and tries to clear it by 
	 * giving one car right of way and permission to run over things.
	 */
	private void checkDeadlock() {
		
		//If it has stood still for a long time, give it right of way
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
		
		//Goes through the chain of blocking cars and tries to detect a cycle and clear it.
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
	
	/**
	 * Returns the current road the vehicle is on.
	 * @return
	 */
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
	
	/**
	 * Determines if the road is a valid short-term goal.
	 * @param road
	 * @param goal
	 * @return
	 */
	public boolean isValidGoal(Road road, Entity goal) {
		if((road instanceof ParkingSpace && road != goal)) {
			return false;
		}
		if(road instanceof RoundaboutRoad) {
			return false;
		}
		if(road instanceof SideWalk) {
			return false;
		}
		if(road instanceof Spawn) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * A getter for the debug label which can be accessed by the GUI
	 * @return
	 */
	public String debugLabel() {
		return debugString;
	}
	
	/**
	 * Starts a parking session for a parking space.
	 * @param time, duration of the parking
	 * @param p, the parking space
	 */
	private void park(int time, ParkingSpace p) {
		parked = true;
		this.parkingSpace = p;
		p.reserve();
		if(goingToWork) {
			occupants.get(0).setParked(time);			
		}
		else {
			occupants.get(0).setParked(500);		
		}
	}
	
	
	/**
	 * Adds a road as visited by the vehicle
	 * @param r
	 */
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
			if(!(this instanceof Bus) && r instanceof BusStop) {
				return;
			}
			open.add(r);
		}
	}
	
	/**
	 * Checks if a vehicle is in the path of this one.
	 * @param car
	 * @param pathDistance
	 * @return
	 */
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
	
	public Vehicle getBlockingCar() {
		return this.blockingCar;
	}
	
	/**
	 * Sets a vehicle to be in queue for an intersection
	 * @param isInQueue
	 */
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
	
	/**
	 * Checks of the way is clear to move
	 * @param c
	 * @return
	 */
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
			else if(currentRoad instanceof ParkingSpace && !(car.getRoad() instanceof ParkingSpace) && Tools.isPathIntersect(c, car, 3)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Goes through the parking spaces in the list of viewed roads to find the closest one that is free.
	 * @param target
	 * @return ParkingSpace which is free, if none is found, returns null.
	 */
	private ParkingSpace findParking(GridPoint target) {
		double minDist = Double.MAX_VALUE;
		ParkingSpace parking = null;
		for (Road road : open) {
			if(road instanceof ParkingSpace) {
				ParkingSpace p = (ParkingSpace) road;
				if(!p.isReserved()){
					Double distance = Tools.gridDistance(grid.getLocation(this), target);
					if(distance < minDist) {
						minDist = distance;
						parking = p;
					}
				}
			}
		}
		return parking;
	}
	
	@Deprecated
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

	public Goals getGoals() {
		return goals;
	}
	
	public void addGoal(Entity goal) {
		this.goals.addGoal(goal);
	}
	
	/**
	 * Ads an edge in the debug network(will show up in the GUI)
	 * @param obj
	 */
	public void debugPointTo(Object obj) {
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> net = (Network<Object>)context.getProjection("debug network");
		net.addEdge(this, obj);
		
	}
	
	/**
	 * Removes the edges from an object.
	 * @param obj
	 */
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
	
	public int getOccupantCount() {
		return occupants.size();
	}
	
	public boolean isFull() {
		return !(occupants.size() < occupantLimit);
	}
	
}
