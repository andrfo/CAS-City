package citySim.agent;

import java.util.List;
import java.util.Random;

import citySim.environment.Building;
import citySim.environment.SideWalk;
import citySim.environment.Spawner;
import repast.simphony.context.Context;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import utils.Tools;

public class Person extends Agent{

	private Building workPlace;
	private Double dailyBudget;
	private Double previousCost;
	private String previousDecision;
	private Double accumulatedCostToday;
	private boolean isInstantiated;
	
	private Spawner spawner;
	
	//0 = car, 1 = bus.
	//pob: car = 1 - x, bus = x
	private Double travelPreference; 
	
	
	
	public Person(ContinuousSpace<Object> space, Grid<Object> grid, Spawner spawner) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		workPlace = null;
		previousCost = null;
		accumulatedCostToday = 0d;
		this.spawner = spawner;
		travelPreference = new Random().nextDouble();
		getTravelChoice();
		// TODO Auto-generated constructor stub
	}



	public Building getWorkPlace() {
		return workPlace;
	}
	
	public String getTravelChoice() {
		if(Tools.isTrigger(travelPreference)) {
			previousDecision = "bus";
			return "bus";
		}
		previousDecision = "car";
		return "car";
	}

	public void setWorkPlace(Building workPlace) {
		this.workPlace = workPlace;
	}
	
	private void updateCostAndChoice(Vehicle v) {
		Double deltaPreference = 0.1d;
		
		accumulatedCostToday = 0d;
		if(v instanceof Car) {
			accumulatedCostToday += ((Car) v).getDistanceMoved();	
			accumulatedCostToday += ((Car) v).getTollCost();	
			accumulatedCostToday -= 20d; //Experience; people like driving
		}
		else if(v instanceof Bus){
			accumulatedCostToday += ((Bus) v).getCost();
			accumulatedCostToday += 20d; //Experience; people don't like bussing
		}
		
		if(previousCost == null) {
			travelPreference = new Random().nextDouble();
		}
		else {//update preference
			if(previousCost < accumulatedCostToday) {
				if(previousDecision.equals("bus") && travelPreference < 1 - deltaPreference) {
					travelPreference += deltaPreference;
				}
				else if(previousDecision.equals("car") && travelPreference > deltaPreference) {
					travelPreference -= deltaPreference;
				}
			}
			else {
				if(previousDecision.equals("bus") && travelPreference > deltaPreference) {
					travelPreference -= deltaPreference;
				}
				else if(previousDecision.equals("car") && travelPreference < 1 - deltaPreference) {
					travelPreference += deltaPreference;
				}
			}
		}
		previousCost = accumulatedCostToday;
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
