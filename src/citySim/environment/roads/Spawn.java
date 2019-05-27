package citySim.environment.roads;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import citySim.agent.Agent;
import citySim.agent.Bus;
import citySim.agent.Car;
import citySim.agent.Person;
import citySim.agent.Vehicle;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import utils.Tools;

/**
 * A spawn point in the simulation that spews forth the masses of agents as directed by the Spawner
 * @author andrfo
 *
 */
public class Spawn extends Road {
	
	private List<Vehicle> vehicleQueue;
	private List<Person> busQueue;
	
	private Despawn despawn;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private Network<Object> net;
	private Context<Object> context;

	@SuppressWarnings("unchecked")
	public Spawn(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.context = context;
		vehicleQueue = new ArrayList<Vehicle>();
		busQueue = new ArrayList<Person>();
		despawn = null;
		net = (Network<Object>)context.getProjection("road network");
	}
	
	/**
	 * Runs every step
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		spawn();
	}
	
	/**
	 * Ads a person to the list of persons waiting for a bus
	 * @param p, Person
	 */
	public void addToBusQueue(Person p) {
		this.busQueue.add(p);
	}
	
	/**
	 * Ads a vehicle to a queue of vehicles waiting to be spawned
	 * @param v
	 */
	public void addToVehicleQueue(Vehicle v) {//Have a dedicated queue item class instead of storing stuff in this?
		vehicleQueue.add(v);
	}
	
	/**
	 * Spawns vehicles from the queues if there is room.
	 * Also ads persons to buses when they spawn
	 */
	private void spawn() {
		
		for(Person p: busQueue) {//Add time spent waiting for the bus
			p.addTimeUse(1);
		}
		
		if(vehicleQueue.size() == 0) {
			return;
		}
		//Check surroundings to see if it's clear to spawn a vehicle
		GridPoint pt = grid.getLocation(this);
		NdPoint spacePt = space.getLocation(this);
		Double dist = Double.MAX_VALUE;
		GridCellNgh<Vehicle> agentNghCreator = new GridCellNgh<Vehicle>(grid, pt, Vehicle.class, 2, 2);
		List<GridCell<Vehicle>> agentGridCells = agentNghCreator.getNeighborhood(true);
		for (GridCell<Vehicle> cell : agentGridCells) {
			if(cell.items().iterator().hasNext()) {
				for(Vehicle v : cell.items()) {
					dist = Tools.spaceDistance(space.getLocation(v), spacePt);
					if(dist <= 1.6) {
						//blocked, wait
						return;
					}
				}
			}
		}
		
		
		//Add the agent to the context
		Vehicle vehicle = vehicleQueue.remove(0);
		context.add(vehicle);
		space.moveTo(vehicle, spacePt.getX(), spacePt.getY());
		grid.moveTo(vehicle, pt.getX(), pt.getY());
		vehicle.setStart(this);
		vehicle.setNet(net);
		
		//Setup
		if(vehicle instanceof Car) {
			//something
		}
		else if(vehicle instanceof Bus) {//Get people from those that are waiting for the bus
			for(int i = 0; i < busQueue.size(); i++) {
				if(busQueue.size() > 0 && !vehicle.isFull()) {
					vehicle.addOccupant(busQueue.remove(i));
				}
			}
		}
		vehicle.getGoals().setEndGoal(getNearestDespawn()); //The vehicle will return to the despawn near here when done
		
	}
	
	/**
	 * Finds the nearest despawn point
	 * @return Nearest despawn
	 */
	private Despawn getNearestDespawn() {
		GridPoint pt = grid.getLocation(this);
		int range = 6;
		while(despawn == null && range < 10000) {
			GridCellNgh<Despawn> roadNghCreator = new GridCellNgh<Despawn>(grid, pt, Despawn.class, range, range);
			List<GridCell<Despawn>> roadGridCells = roadNghCreator.getNeighborhood(true);
			for (GridCell<Despawn> gridCell : roadGridCells) {
				if(gridCell.items().iterator().hasNext()) {
					despawn = gridCell.items().iterator().next();
					return despawn;
				}
			}
			range += 20;
		}
		return despawn;
	}
	
	/**
	 * Returns the size of the spawn queue for use in the GUI
	 * @return spawn queue size
	 */
	public String getQueueLabel() {
		String newLine = System.getProperty("line.separator");
		return 	"Vehicles: 			" + vehicleQueue.size() + newLine +
				"Waiting for bus:   " + busQueue.size();
	}

}
