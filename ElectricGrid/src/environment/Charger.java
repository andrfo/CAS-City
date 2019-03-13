package environment;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Charger extends ElectricEntity{

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public Charger(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.baseCost = 0.01;//lights and the like
		this.unitCost = 6d;//3 to 20, typically 6
	}
	
	//has small base cost
	//has large cost while in use
	public void setIsCharging(boolean isCharging) {
		if(isCharging) {
			
			Double newValue = baseCost + unitCost;
			onChange(totalCost, newValue);//Pass by reference error?
		}
	}
	
}
