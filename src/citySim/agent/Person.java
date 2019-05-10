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

public class Person extends Agent{

	private Building workPlace;
	private Building shop;
	private Double dailyBudget;
	private Double accumulatedTripCost;
	private boolean isInstantiated;
	
	private static final Double DISTANCE_CONSTANT_CAR = 0.012d;
	private static final Double DISTANCE_CONSTANT_BUS = 1d;
	private static final Double TIME_CONSTANT_CAR = 0.1d;
	private static final Double TIME_CONSTANT_BUS = 0.1d;
	private static final Double CROWD_CONSTATNT_BUS = 1d;
	private static final Double TOLL_CONSTANT = 40d;
	private static final Double FARE_CONSTANT = 40d;
	private static final Double MEMORY_FACTOR = 0.05d;
	private static final int TIME_ESTIMATION = 100;
	private int TOLL_COST_ESTIMATION;
	
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
		this.TOLL_COST_ESTIMATION = params.getInteger("toll_cost");
		this.space = space;
		this.grid = grid;
		workPlace = null;
		accumulatedTripCost = 0d;
		this.spawner = spawner;
		previousTrips = new ArrayList<Trip>();
		
		// TODO Auto-generated constructor stub
	}



	public Building getWorkPlace() {
		return workPlace;
	}
	
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
	
	public void setParked(int time) {
		parkedTimer = time;
	}
	
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
	
	public int getLastTimeUse() {
		return lastTimeUse;
	}
	
	//TODO: Distance estimation for within and outside the city
	
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
		cost += TOLL_COST_ESTIMATION * TOLL_CONSTANT;
		return cost;
	}
	
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
		cost += FARE_CONSTANT;
		return cost;
	}
	
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
		if(cost == 0) {
			if(choice.equals("bus")) {
				cost = busCostEstimate() * mf;
			}
			else {
				cost = carCostEstimate() * mf;
			}
		}
		return cost;
	}
	
	public String getTravelChoice() {
		
		Double carCost = getLastCost("car");
		Double busCost = getLastCost("bus");
		Double pobabilityOfCar = 1 - (carCost / (carCost + busCost));
		
		if(Tools.isTrigger(pobabilityOfCar)) {
			return "car";
		}
		return "bus";
		
		
	}
		
	public BusStop getNearestBusStop() {
		return nearestBusStop;
	}
		

	public void setWorkPlace(Building workPlace) {
		this.workPlace = workPlace;
		this.nearestBusStop = workPlace.getNearestBusStop();
	}
	
	public void setShoppingPlace(Building shop) {
		this.shop = shop;
	}
	
	private void updateCostAndChoice(Vehicle v) {
		//Set the cost
		accumulatedTripCost = 0d;
		String choice = "";
		if(v instanceof Car) {
			choice = "car";
			//Distance
			accumulatedTripCost += ((Car) v).getDistanceMoved() * DISTANCE_CONSTANT_CAR;	
			//Time
			accumulatedTripCost += ((Car) v).getTickCount() * TIME_CONSTANT_CAR;	
			lastTimeUse = ((Car) v).getTickCount();
			//Toll
			accumulatedTripCost += ((Car) v).getTollCost() * TOLL_CONSTANT;
		}
		else if(v instanceof Bus){
			choice = "bus";
			//Distance from bus stop to work
			if(workPlace != null) {
				accumulatedTripCost += workPlace.getDistanceToNearestBusStop() * DISTANCE_CONSTANT_BUS;
			}
			else {
				accumulatedTripCost += 50 * DISTANCE_CONSTANT_BUS;
			}
			//Bus fare
			accumulatedTripCost += FARE_CONSTANT;
			//Time
			accumulatedTripCost += ((Bus) v).getTickCount() * TIME_CONSTANT_CAR;
			lastTimeUse = ((Bus) v).getTickCount();
			//How full the bus is
			//TODO: get bus pop count
			//TODO: Get time waited for bus
		}
		
		previousTrips.add(new Trip(accumulatedTripCost, choice));
		printPriceHistory();
		
		
	}
	
	private void printPriceHistory() {
		String newLine = System.getProperty("line.separator");
		String s = 
				  "PriceHistory:" + newLine;
		for(int i = 1; i <= previousTrips.size(); i++) {
			s += "    -Trip " + i + ": " + newLine;
			s += "        -Vehicle: " + previousTrips.get(i - 1).getChoice() + newLine;
			s += "        -Price:   " + previousTrips.get(i - 1).getCost() + newLine;
		}
		System.out.println(s);
	}
	
	
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
