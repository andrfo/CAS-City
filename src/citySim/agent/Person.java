package citySim.agent;

import java.util.List;

import citySim.environment.Building;
import citySim.environment.SideWalk;
import repast.simphony.context.Context;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Person extends Agent{

	private Building workPlace;
	private Double dailyBudget;
	private Double yesterdaysCost;
	private Double accumulatedCostToday;
	private boolean isInstantiated;
	
	private String travelPreference;
	
	
	
	public Person(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		workPlace = null;
		// TODO Auto-generated constructor stub
	}



	public Building getWorkPlace() {
		return workPlace;
	}



	public void setWorkPlace(Building workPlace) {
		this.workPlace = workPlace;
	}
	
	public void setReachedGoal(Vehicle v, boolean isEndGoal) {
		if(isEndGoal) {
			return;
		}
		@SuppressWarnings("unchecked")
		Context<Object> context = ContextUtils.getContext(this);
		GridPoint pt = grid.getLocation(this);
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
