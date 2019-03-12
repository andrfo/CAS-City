package utility;


import environment.Substation;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Tools {
	
	public static final int TICKS_PER_DAY = 8640;
	
	public static final int NORTHWEST = 0;
	public static final int NORTH = 1;
	public static final int NORTHEAST = 2;
	
	public static final int WEST = 3;
	public static final int EAST = 4;

	public static final int SOUTHWEST = 5;
	public static final int SOUTH = 6;
	public static final int SOUTHEAST = 7;
	
	
	public static int getTime() {
		double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		int time = (int) (currentTick % TICKS_PER_DAY);
		return time;
	}
	
	

	
	/**
	 * Randomly returns true based on the probability x [ x >= 0]
	 * @param x
	 * @return True if triggered
	 */
	public static boolean isTrigger(Double x) {
		if(x < 0) {
			throw new IllegalArgumentException("Cannot have a negative probablity");
		}
		return x - Math.random() > 0;
	}
	
	public static int getMooreDirection(GridPoint a, GridPoint b) {
		
		/*	Grid Directions:
		 * 
		 * 		0 1 2
		 * 		3 8 4
		 * 		5 6	7
		 */
		
		
		int dx = b.getX() - a.getX();
		int dy = b.getY() - a.getY();
		
		if(dy > 0) {//Northward
			if(dx < 0) { //Northwest
				return 0;
			}
			if(dx == 0) {//North
				return 1;
			}
			if(dx > 0) {//Northeast
				return 2;
			}
		}
		if(dy == 0) {//East or West
			if(dx < 0) { //West
				return 3;
			}
			if(dx == 0) {//Center
				return 8;
			}
			if(dx > 0) {//East
				return 4;
			}
		}
		if(dy < 0) {//Southward
			if(dx < 0) { //Southwest
				return 5;
			}
			if(dx == 0) {//South
				return 6;
			}
			if(dx > 0) {//Southeast
				return 7;
			}
		}
		return 9; //Should not get here.
	}
	
	public static Double gridDistance(GridPoint a, GridPoint b) {
		Double dx = (double) (b.getX() - a.getX());
		Double dy = (double) (b.getY() - a.getY());
		return Math.sqrt((dx*dx) + (dy*dy));
	}
	
	public static Double spaceDistance(NdPoint a, NdPoint b) {
		Double dx = (double) (b.getX() - a.getX());
		Double dy = (double) (b.getY() - a.getY());
		return Math.sqrt((dx*dx) + (dy*dy));
	}
	
	public static Object getObjectAt(Grid<Object> grid, Class<?> c, int x, int y) {
		for(Object o: grid.getObjectsAt(x, y)) {
			if(o.getClass().equals(c)) {
				return (Substation) o;
			}
		}
		return null;
		
	}
	
	
}
