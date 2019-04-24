package citySim.environment.electric;

import citySim.environment.roads.BusStop;
import citySim.environment.roads.Spawn;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import utils.Tools;

public class Building extends ElectricEntity{

	private int occupants = 0;
	private int occupiedTime;
	
	public Building(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.unitLoad = 3d;//TODO: research
		this.baseLoad = unitLoad;//lights and the like
		this.totalLoad = baseLoad;
		this.parent = null;
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Runs every step
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		
		if(occupiedTime > 0) {
			occupiedTime--;
		}
		else {
			removeOccupants(1);
		}
		if(RandomHelper.nextDouble() < 0.01) {
			addOccupants(1);
		}
	}

	//Has base cost
	//Has cost per occupant
	
	public void addOccupants(int n) {
		occupants += n;
		Double newValue = baseLoad + occupants*unitLoad;
		onChange(totalLoad, newValue);//Pass by reference error?
		totalLoad = newValue;
	}
	
	public void removeOccupants(int n) {
		if(occupants > n) {
			occupants -= n;
		}
		else {
			occupants = 0;
		}
		Double newValue = baseLoad + occupants*unitLoad;
		onChange(totalLoad, newValue);//Pass by reference error?
		occupiedTime = RandomHelper.nextIntFromTo(0, 50);
		totalLoad = newValue;
	}

	public Double getDistanceToNearestBusStop() {
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		for(Object o: grid.getObjects()){
			if(o instanceof BusStop) {
				dist = Tools.manhattanDistance(grid.getLocation(o), grid.getLocation(this));
				if(dist < minDist) {
					minDist = dist;
				}
			}
		}
		return minDist;
	}
	
	public BusStop getNearestBusStop() {
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		BusStop nearest = null;
		for(Object o: grid.getObjects()){
			if(o instanceof BusStop) {
				dist = Tools.manhattanDistance(grid.getLocation(o), grid.getLocation(this));
				if(dist < minDist) {
					minDist = dist;
					nearest = (BusStop) o;
				}
			}
		}
		return nearest;
	}
	
	public Double getDistanceToNearestSpawn() {
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		for(Object o: grid.getObjects()){
			if(o instanceof Spawn) {
				dist = Tools.manhattanDistance(grid.getLocation(o), grid.getLocation(this));
				if(dist < minDist) {
					minDist = dist;
				}
			}
		}
		return minDist;
	}
	
}