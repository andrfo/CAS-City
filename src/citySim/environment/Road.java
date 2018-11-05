package citySim.environment;

import citySim.agent.Car;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import utils.Tools;
import utils.Vector2D;

public class Road extends Entity{
	
	
	private String type;
	private Junction junction;
	private Car car;
	private boolean isOccupied = false;
	private boolean isJunctionEdge;
	private boolean isNewCar;
	GridPoint pt;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Road(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.isJunctionEdge = false;
		this.isNewCar = false;
	}
	
	@Watch(
			watcheeClassName = "citySim.agent.Car", 
			watcheeFieldNames = "moved",
			query = "colocated",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	@ScheduledMethod
	public void onTriggerEnter() {
		
		pt = grid.getLocation(this);
		
		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
			if(obj instanceof Car) {
				Car c = (Car)obj;
				c.addVisited(this);
				
				
				if(junction == null) {
					return;
				}
				//Check if car is entering or leaving junction
				if(isLeavingJunction(c)) {
					junction.carLeft(c);
					return;
				}
				isOccupied = true;
				this.car = c;
				isNewCar = true;
			}
		}
		if(isJunctionEdge && isOccupied && car != null) {
			try {
//				space.moveTo(car, space.getLocation(this).getX(), space.getLocation(this).getY());
//				grid.moveTo(car, grid.getLocation(this).getX(), grid.getLocation(this).getY());
				junction.addCar(car);									
			}
			catch (Exception e) {
				this.car = null;
				System.out.println("Car is dead");
				
			}
		}
		isNewCar = false;
		
		
		Schedule schedule = (Schedule) RunEnvironment.getInstance().getCurrentSchedule();
		int tick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		ScheduleParameters params = ScheduleParameters.createOneTime(tick, ScheduleParameters.FIRST_PRIORITY);
		schedule.schedule(params, this, "update");
	}
	
	public void update() {
		boolean containsCar = false;
		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
			if(obj instanceof Car) {
				containsCar = true;
			}
		}
		if(!containsCar) {
			isOccupied = false;
			this.car = null;
		}
	}
	
	private boolean isLeavingJunction(Car c) {
		Vector2D cDir = c.getDirection();
		if(cDir == null) { 
			c.setInJunction(false);
			return true;
		}
		Vector2D diff = Tools.create2DVector(grid.getLocation(junction), grid.getLocation(this));
		double angle = diff.angle(cDir);
		
		if(angle < Math.PI/2) {
			c.setInJunction(false);
		}
		c.setInJunction(true);
		return false;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public Junction getJunction() {
		return junction;
	}
	
	public void setJunction(Junction junction) {
		this.junction = junction;
	}
	
	public boolean isJunctionEdge() {
		return isJunctionEdge;
	}
	
	public void setJunctionEdge(boolean isJunctionEdge) {
		this.isJunctionEdge = isJunctionEdge;
	}
	
	public Car getCar() {
		return car;
	}
	
	public void setCar(Car car) {
		this.car = car;
	}
	
	public boolean isOccupied() {
		return isOccupied;
	}
	
	public void setOccupied(boolean isOccupied) {
		this.isOccupied = isOccupied;
	}
	
	
	
	
	
	
	
	
	
}
