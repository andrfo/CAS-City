package environment;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Building extends ElectricEntity{

	private int occupants = 0;
	
	public Building(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.unitCost = 3d;//TODO: research
		this.baseCost = unitCost;//lights and the like
		// TODO Auto-generated constructor stub
	}

	//Has base cost
	//Has cost per occupant
	
	public void addOccupants(int n) {
		occupants += n;
		Double newValue = baseCost + occupants*unitCost;
		onChange(totalCost, newValue);//Pass by reference error?
	}
	
	public void removeOccupants(int n) {
		if(occupants >= n) {
			occupants -= n;
		}
		else {
			occupants = 0;
		}
		Double newValue = baseCost + occupants*unitCost;
		onChange(totalCost, newValue);//Pass by reference error?
	}
	
}
