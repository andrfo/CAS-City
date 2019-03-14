package environment;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Building extends ElectricEntity{

	private int occupants = 0;
	private int occupiedTime;
	
	public Building(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.unitLoad = 3d;//TODO: research
		this.baseLoad = unitLoad;//lights and the like
		this.totalLoad = baseLoad;
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
	
	
}
