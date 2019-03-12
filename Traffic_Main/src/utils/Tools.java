package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import citySim.agent.Vehicle;
import citySim.environment.Road;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
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
	
	public static boolean isPathIntersect(Vehicle a, Vehicle b, int distance) {
		
		for(	int i = a.getPathIndex(); 
				i < a.getPathIndex() + distance &&
				i < a.getPath().size() - 1; 
				i++) {
			for(	int j = b.getPathIndex(); 
					j < b.getPathIndex() + distance &&
					j < b.getPath().size() - 1; 
					j++) {
				if(a.getPath().get(i).getTarget() == b.getPath().get(j).getTarget()) {
					return true;
				}
			}
			
		}
		return false;
	}
	
	public static List<RepastEdge<Object>> aStar(Road start, Road goal, Network<Object> net){
		
		// Will contain the shortest path
		ArrayList<RepastEdge<Object>> path = new ArrayList<RepastEdge<Object>>();
		

		// The set of nodes already evaluated
		ArrayList<Road> closed = new ArrayList<Road>();
		
		
		// The set of currently discovered nodes that are not evaluated yet.
	    // Initially, only the start node is known
		ArrayList<Road> open = new ArrayList<Road>();
		
		
		// For each node, which node it can most efficiently be reached from.
	    // If a node can be reached from many nodes, cameFrom will eventually contain the
	    // most efficient previous step.
		HashMap<Road, Road> cameFrom = new HashMap<Road, Road>();
		
		
		// For each node, the cost of getting from the start node to that node.
		HashMap<Road, Double> gScore = new HashMap<Road, Double>();
		
		
		// For each node, the total cost of getting from the start node to the goal
	    // by passing by that node. That value is partly known, partly heuristic.
		HashMap<Road, Double> fScore = new HashMap<Road, Double>();
		
		gScore.put(start, 0d);
		fScore.put(start, gridDistance(start.getLocation(), goal.getLocation()));
		
		open.add(start);
		
		while(open.size() > 0) {
			open.sort((o1, o2) -> 
			(fScore.get(o1).compareTo(fScore.get(o2))));
			
			Road current = open.remove(0); //Sorted by f value
			
			if(current == goal) {
				Road child = goal;
				Road parent;
				while(true) {
					if(child == start) {
						break;
					}
					
					parent = cameFrom.get(child);
					
					RepastEdge<Object> edge = net.getEdge(parent, child);
					path.add(0, edge);
					
					child = parent;
				}
				return path;
			}
			
			closed.add(current);
			for (RepastEdge<Object> n : net.getOutEdges(current)) {
				Road neighbour = (Road)n.getTarget();
				if(closed.contains(neighbour)) {
					continue; // Ignore the neighbor which is already evaluated.
				}
				
				Double tentativeGScore = gScore.getOrDefault(current, Double.MAX_VALUE) + n.getWeight();
				
				if(!open.contains(neighbour)) {
					open.add(neighbour);
				}
				else if(tentativeGScore >= gScore.getOrDefault(neighbour, Double.MAX_VALUE)) {
					continue; // Not a better path
				}
				
				//This path is the best until now, record it!
				cameFrom.put(neighbour, current);
				gScore.put(neighbour, tentativeGScore);
				fScore.put(
						neighbour, 
						gScore.getOrDefault(neighbour, Double.MAX_VALUE) + 
							gridDistance(neighbour.getLocation(), goal.getLocation()));
			}
		}
		return path;
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
	//TODO: fix:(?) change direction?
	public static Vector2D create2DVector(GridPoint from, GridPoint to) {
		Vector2D v =  new Vector2D(to.getX() - from.getX(), to.getY() - from.getY());
		return v;
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
	
	
}