package environment;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Charger extends ElectricEntity{

	private int chargeTime = 0;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private boolean isCharging = false;
	public Charger(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.baseLoad = 0.01;//lights and the like
		this.unitLoad = 6d;//3 to 20, typically 6
		this.totalLoad = 0.01;
	}
	
	//has small base cost
	//has large cost while in use
	public void setIsCharging(boolean isCharging) {
		if(isCharging) {
			
			Double newValue = baseLoad + unitLoad;
			onChange(totalLoad, newValue);//Pass by reference error?
			totalLoad = newValue;
		}
		else {
			Double newValue = baseLoad;
			onChange(totalLoad, newValue);//Pass by reference error?
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
//				System.out.println("stopped charging");
			}
		}
		else if(RandomHelper.nextDouble() < 0.01) {
//			System.out.println("Chargning");
			setIsCharging(true);
			chargeTime = RandomHelper.nextIntFromTo(0, 50);
		}
	}
}
