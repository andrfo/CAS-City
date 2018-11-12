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
	private boolean isJunctionEdge;
	GridPoint pt;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Road(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.space = space;
		this.grid = grid;
		this.isJunctionEdge = false;
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
				c.addVisited(this);//TODO:have in car instead
				if(isJunctionEdge && !type.equals("junction")) {
					if(!isLeavingJunction(c)) {
						junction.addCar(c);							
					}
				}				
			}
		}
		
//		Schedule schedule = (Schedule) RunEnvironment.getInstance().getCurrentSchedule();
//		int tick = (int) schedule.getTickCount();
//		
//		ScheduleParameters params = ScheduleParameters.createOneTime(tick, ScheduleParameters.FIRST_PRIORITY);
//		schedule.schedule(params, this, "update");
	}
	
//	public void update() {
//		boolean containsCar = false;
//		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
//			if(obj instanceof Car) {
//				containsCar = true;
//			}
//		}
//		if(!containsCar) {
//			isOccupied = false;
//		}
//	}
	
	private boolean isLeavingJunction(Car c) {
		Vector2D cDir = c.getDirection();
		if(cDir == null) { 
			return true;
		}
		Vector2D diff = Tools.create2DVector(grid.getLocation(junction), grid.getLocation(this));
		double angle = diff.angle(cDir);
		
		if(angle < Math.PI/2) {
//			System.out.println("Car leaving junction");
			return true;
		}
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
	
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}
	
	public boolean isOccupied() {
		pt = grid.getLocation(this);
		
		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
			if(obj instanceof Car) {	
				return true;
			}
		}
		return false;
	}
	
	
	
	
	
	
	
	
	
}
