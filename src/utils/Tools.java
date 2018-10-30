package utils;

import repast.simphony.space.grid.GridPoint;

public class Tools {

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
	//TODO: fix:(?) change direction?
	public static Vector2D create2DVector(GridPoint a, GridPoint b) {
		Vector2D v =  new Vector2D(a.getX() - b.getX(), a.getY() - b.getY());
		return v;
	}
	
	public static double distance(GridPoint a, GridPoint b) {
		float dx = a.getX() - b.getX();
		float dy = a.getY() - b.getY();
		return Math.sqrt(dx*dx + dy*dy);
		
	}
}
