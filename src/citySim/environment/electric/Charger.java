package citySim.environment.electric;

import java.util.List;

import citySim.agent.Person;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Charger extends ElectricEntity{

	private int chargeTime = 0;
	private Double unitLoad;
	private boolean isCharging = false;
	public Charger(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.unitLoad = 6d;//3 to 20, typically 6
		this.totalLoad = 0.01;
		this.grid = grid;
		this.space = space;
	}
	
	
	//has small base cost
	//has large cost while in use
	public void setIsCharging(boolean isCharging) {
		if(isCharging) {
			
			Double newValue = Double.valueOf(totalLoad) + unitLoad;
			update(newValue - Double.valueOf(totalLoad));
			totalLoad = newValue;
		}
		else {
			Double newValue = Double.valueOf(totalLoad) - unitLoad;
			update(newValue - Double.valueOf(totalLoad));
			totalLoad = newValue;
		}
		this.isCharging = isCharging;
	}
	
	/**
	 * Runs every step
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step(){
		
		if(isCharging) {
			if(chargeTime > 0) {
				chargeTime--;
			}
			else {
				setIsCharging(false);
			}
		}
	}
}
