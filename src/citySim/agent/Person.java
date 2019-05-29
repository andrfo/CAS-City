package citySim.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import citySim.environment.Spawner;
import citySim.environment.electric.Building;
import citySim.environment.roads.BusStop;
import citySim.environment.roads.SideWalk;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import structures.Trip;
import utils.Tools;

/**
 * The person in the simulation. Making choioces on travel choice.
 * @author andrfo
 *
 */
public class Person extends Agent{

	private Building workPlace;
	private Building shop;
	private Double dailyBudget;
	private Double accumulatedTripCost;
	private boolean isInstantiated;
	private int tripTimeUsed;
	
	
	//Factors that apply in calculations (A value of 1 has no effect)
	//===============================================================
	
	//What is the cost of driving: Gas prices, wear and tear, ...
	private static final Double DISTANCE_CONSTANT_CAR = 0.012d;
	
	//Cost per distance of of walking from bus stop to destination
	private static final Double DISTANCE_CONSTANT_BUS = 1d;
	
	//Time value/dislike of driving (Set low because the tick count is pretty high for a usual trip)
	private static final Double TIME_CONSTANT_CAR = 0.1d;
	
	//Time Value/dislike of busses (Set low because the tick count is pretty high for a usual trip, but higher than car from personal preference)
	private static final Double TIME_CONSTANT_BUS = 0.15d;
	
	//To account for the feeling that crowded buses are less pleasant
	private static final Double CROWD_CONSTATNT_BUS = 1.5d;
	
	//To add some to the toll cost at will. For instance if people hate tolls. (Set to neutral at the moment)
	private static final Double TOLL_CONSTANT = 1d;
	
	//The degree to which people click to their habits
	private static final Double MEMORY_FACTOR = 0.05d;
	
	
	//Estimates
	//===============================================================
	
	//Estimate on how much time a trip will take
	private static final int TIME_ESTIMATION = 100;
	
	//Costs
	//===============================================================
	
	//The cost of a bus ticket
	private static final Double BUS_FARE_COST = 40d;
	
	//The toll cost of a trip
	private int TOLL_COST;//Set in the GUI
	
	
	private List<Trip> previousTrips;
	
	
	private Spawner spawner;
	private BusStop nearestBusStop;
	private int lastTimeUse = 0;
	
	//0 = car, 1 = bus.
	//pob: car = 1 - x, bus = x
	private Double travelPreference;
	private int parkedTimer = 0;
	
	
	
	public Person(ContinuousSpace<Object> space, Grid<Object> grid, Spawner spawner) {
		super(space, grid);
		Parameters params = RunEnvironment.getInstance().getParameters();
		this.TOLL_COST = params.getInteger("toll_cost"); //Ads the parameter to the Repast GUI
		this.space = space;
		this.grid = grid;
		workPlace = null;
		accumulatedTripCost = 0d;
		this.spawner = spawner;
		previousTrips = new ArrayList<Trip>();
		tripTimeUsed = 0;
		
		// TODO Auto-generated constructor stub
	}



	/**
	 * Gets the assigned work place for this person
	 * @return Building, the work place
	 */
	public Building getWorkPlace() {
		return workPlace;
	}
	
	/**
	 * Is called as a check each tick to see if the person is done and ready to leave. If not the times is decremented
	 * @return True if the person is ready to leave
	 */
	public boolean isWantToLeave() {
		if(parkedTimer > 0) {
			parkedTimer--;
			return false;
		}
		if(workPlace != null) {
			workPlace.removeOccupants(this);
		}
		else {
			shop.removeOccupants(this);
		}
		return true;
	}
	
	/**
	 * Sets a timer for how long it will stay before it is ready to leave.
	 * @param time
	 */
	public void setParked(int time) {
		parkedTimer = time;
	}
	
	
	/**
	 * Models the fact that people tend to continue making the same choices they have done in the past
	 * For a given choice, it goes through its choice history and counts the number of times that choice has been made
	 * Calculates a weight to weigh on the choice probability based on the count and the MEMORY FACTOR(a).
	 * @param choice
	 * @param days
	 * @return Double, (1 - a)^n
	 */
	private Double memoryFactor(String choice, int days) {
		int count = 0;
		int i = 0;
		for (Trip t: previousTrips) {
			if(t.getChoice().equals(choice)) {
				count++;
			}
			i++;
			if(i >= days) {
				break;
			}
		}
		return Math.pow((1 - MEMORY_FACTOR), count);
	}
	
	/**
	 * Gets the number of ticks for the last trip
	 * @return int, number of ticks
	 */
	public int getLastTimeUse() {
		return lastTimeUse;
	}
	
	//TODO: Distance estimation for within and outside the city
	
	/**
	 * Estimates the cost of a trip with a car
	 * @return Double, cost
	 */
	private Double carCostEstimate() {
		Double cost = 0d;
		if(workPlace != null) {
			cost += workPlace.getDistanceToNearestSpawn() * DISTANCE_CONSTANT_CAR;
			cost += workPlace.getDistanceToNearestSpawn() * TIME_CONSTANT_CAR;
		}
		else {
			cost += 50 * DISTANCE_CONSTANT_CAR;
			cost += 50 * TIME_CONSTANT_CAR;
		}
		cost += TOLL_COST * TOLL_CONSTANT;
		return cost;
	}
	
	/**
	 * Estimates the cost of a trips with a bus
	 * @return Double, cost
	 */
	private Double busCostEstimate() {
		Double cost = 0d;
		if(workPlace != null) {
			//Distance from bus stop to work
			cost += workPlace.getDistanceToNearestBusStop() * DISTANCE_CONSTANT_BUS;
			//Time
			cost += workPlace.getDistanceToNearestBusStop() * TIME_CONSTANT_BUS;	
		}
		else {
			accumulatedTripCost += 50 * DISTANCE_CONSTANT_BUS;
		}
		//Bus fare
		cost += BUS_FARE_COST;
		return cost;
	}
	
	/**
	 * Gets the last cost with the provided choice if it exists, otherwise it estimates the cost.
	 * @param choice
	 * @return Double, cost
	 */
	private Double getLastCost(String choice) {
		Double mf = memoryFactor(choice, 3);
		Double cost = 0d;
		if(previousTrips.size() > 0) {
			for(int i = previousTrips.size() - 1; i >= 0; i--) {
				if(previousTrips.get(i).getChoice().equals(choice)) {
					cost = previousTrips.get(i).getCost() * mf;
					break;
				}
			}
		}
		if(cost == 0) { //No history of the choice, predicting.
			if(choice.equals("bus")) {
				cost = busCostEstimate() * mf;
			}
			else {
				cost = carCostEstimate() * mf;
			}
		}
		return cost;
	}
	
	/**
	 * Triggers the travel choice based on costs and probabilites thereby generated
	 * @return String, "bus" or "car"
	 */
	public String getTravelChoice() {
		
		Double carCost = getLastCost("car");
		Double busCost = getLastCost("bus");
		Double pobabilityOfCar = 1 - (carCost / (carCost + busCost));
		
		if(Tools.isTrigger(pobabilityOfCar)) {
			return "car";
		}
		return "bus";
		
		
	}
	
	/**
	 * Gets the nearest bus stop to this persons destination
	 * @return BusStop, nearest busstop
	 */
	public BusStop getNearestBusStop() {
		return nearestBusStop;
	}
		

	/**
	 * Sets the nearest busstop and find the nearest busstop
	 * @param workPlace
	 */
	public void setWorkPlace(Building workPlace) {
		this.workPlace = workPlace;
		this.nearestBusStop = workPlace.getNearestBusStop();
	}
	
	/**
	 * Sets the shopping place
	 * @param shop
	 */
	public void setShoppingPlace(Building shop) {
		this.shop = shop;
	}
	
	public Building getShoppingPlace() {
		return shop;
	}
	
	public void addTimeUse(int n) {
		this.tripTimeUsed += n;
	}
	
	/**
	 * Calculates and updates the logs and such with the cost of the trip.
	 * @param Vehicle v, The vehicle used for the trip
	 */
	private void updateCostAndChoice(Vehicle v) {
		//TODO: Add parking costs
		
		//Set the cost
		accumulatedTripCost = 0d;
		String choice = "";
		if(v instanceof Car) {
			choice = "car";
			
			//Distance
			Double distance = ((Car) v).getDistanceMoved();
			accumulatedTripCost += 	distance  * DISTANCE_CONSTANT_CAR;
			spawner.getReporter().addToAverageCarTravelDistance(distance);
			
			//Time
			Double time = (double) ((Car) v).getTickCount();
			accumulatedTripCost += time  * TIME_CONSTANT_CAR;	
			lastTimeUse = (int) Math.round(time);
			spawner.getReporter().addToAverageCarTravelTime(time);
			
			//Toll
			accumulatedTripCost += ((Car) v).getTollCost() * TOLL_COST * TOLL_CONSTANT;
			
			spawner.getReporter().addToAverageCarCost(Double.valueOf(accumulatedTripCost));
		}
		else if(v instanceof Bus){
			choice = "bus";
			
			//Distance from bus stop to work
			Double distance;
			if(workPlace != null) {
				distance = workPlace.getDistanceToNearestBusStop() * DISTANCE_CONSTANT_BUS * 2;
			}
			else {
				distance = shop.getDistanceToNearestBusStop() * DISTANCE_CONSTANT_BUS * 2;
			}
			accumulatedTripCost += distance;
			spawner.getReporter().addToAverageBusTravelDistance(distance);
			
			//Time
			Double time = (double) tripTimeUsed;
			accumulatedTripCost +=  time * TIME_CONSTANT_BUS;
			lastTimeUse = Integer.valueOf(tripTimeUsed);
			spawner.getReporter().addToAverageBusTravelTime(time);
			tripTimeUsed = 0;
			
			//Bus fare
			accumulatedTripCost += BUS_FARE_COST;
			
			spawner.getReporter().addToAverageBusCost(Double.valueOf(accumulatedTripCost));
			
			
			
			
			//How full the bus is
			//TODO: get bus pop count
			//TODO: Get time waited for bus
		}
		
		previousTrips.add(new Trip(accumulatedTripCost, choice));
		//printPriceHistory();
		
		
	}
	
	/**
	 * Prints the logs
	 */
	private void printPriceHistory() {
		String newLine = System.getProperty("line.separator");
		String s = 
				  "Average price: " + spawner.getReporter().getAverageTravelCost() + newLine +
				  "PriceHistory:" + newLine;
		for(int i = 1; i <= previousTrips.size(); i++) {
			s += "    -Trip " + i + ": " + newLine;
			s += "        -Vehicle: " + previousTrips.get(i - 1).getChoice() + newLine;
			s += "        -Price:   " + previousTrips.get(i - 1).getCost() + newLine;
		}
		System.out.println(s);
	}
	
	
	/**
	 * Called from the vehicle when a goal is reached for the person (work place, shop, or despawn)
	 * @param Vehicle v, the vehicle used during the trip
	 * @param isEndGoal, Boolean, True if the goal reach is the despawn.
	 */
	public void setReachedGoal(Vehicle v, boolean isEndGoal) {
		if(isEndGoal) {
			updateCostAndChoice(v);
			if(workPlace == null) {
				spawner.returnShopper(this);
			}
			else {
				spawner.returnWorker(this);
			}
			return;
		}
		if(workPlace != null) {
			workPlace.addOccupants(this);;
		}
		else {
			shop.addOccupants(this);
		}
		
		//Dump the passenger on the sidewalk(symbolizing that it's busy)
		@SuppressWarnings("unchecked")
		Context<Object> context = ContextUtils.getContext(v);
		GridPoint pt = grid.getLocation(v);
		GridCellNgh<SideWalk> roadNghCreator = new GridCellNgh<SideWalk>(grid, pt, SideWalk.class, 1, 1);
		List<GridCell<SideWalk>> roadGridCells = roadNghCreator.getNeighborhood(false);
		SimUtilities.shuffle(roadGridCells, RandomHelper.getUniform());
		for (GridCell<SideWalk> gridCell : roadGridCells) {
			if(gridCell.items().iterator().hasNext()) {
				SideWalk s = gridCell.items().iterator().next();
				context.add(this);
				space.moveTo(this, grid.getLocation(s).getX(), grid.getLocation(s).getY());
				grid.moveTo(this, grid.getLocation(s).getX(), grid.getLocation(s).getY());
				return;
			}
		}
	}
	
	
	//TODO: 

}
