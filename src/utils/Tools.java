package utils;

import java.util.ArrayList;
import java.util.List;

import citySim.environment.Road;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.GridPoint;

public class Tools {
	
	public static List<RepastEdge<Object>> aStar(Road startRoad, Road goalRoad, Network<Object> net){
		class Node{
			Double g;
			Double h;
			Double f;
			Road road;
			
			public Node(Road road) {
				this.road = road;
			}
		}
		ArrayList<Node> open = new ArrayList<Node>();
		ArrayList<Node> closed = new ArrayList<Node>();
		ArrayList<RepastEdge<Object>> path = new ArrayList<RepastEdge<Object>>();
		
		Node goal = new Node(goalRoad);
		goal.g = Double.MAX_VALUE;
		goal.f = Double.MAX_VALUE;
		
		Node start = new Node(startRoad);
		start.g = 0d;
		start.h = distance(startRoad.getLocation(), goalRoad.getLocation());
		start.f = start.h; //Reference error?
		open.add(start);
		
		while(open.size() > 0) {
			Node current = open.remove(0); //Sorted by f value
			
			if(current == goal) {
				//TODO: Reconstruct path
			}
			closed.add(current);
			
			for (RepastEdge<Object> neighbour : net.getOutEdges(current.road)) {
				
			}
			
		}
		
		return path;
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
	//TODO: fix:(?) change direction?
	public static Vector2D create2DVector(GridPoint from, GridPoint to) {
		Vector2D v =  new Vector2D(to.getX() - from.getX(), to.getY() - from.getY());
		return v;
	}
	
	public static Double distance(GridPoint a, GridPoint b) {
		float dx = a.getX() - b.getX();
		float dy = a.getY() - b.getY();
		return Math.sqrt(dx*dx + dy*dy);
		
	}
}
