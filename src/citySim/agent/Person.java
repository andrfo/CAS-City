package citySim.agent;

import citySim.environment.Building;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Person extends Agent{

	private Building workPlace;
	private Double dailyBudget;
	private Double yesterdaysCost;
	private Double accumulatedCostToday;
	private boolean isInstantiated;
	
	private String travelPreference;
	
	
	
	public Person(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		// TODO Auto-generated constructor stub
	}
	
	
	//TODO: 

}