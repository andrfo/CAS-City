package citySim.environment;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import utils.Tools;

public class Building extends Entity{

	public Building(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		// TODO Auto-generated constructor stub
	}

	public Double getDistanceToNearestBusStop() {
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		for(Object o: grid.getObjects()){
			if(o instanceof BusStop) {
				dist = Tools.manhattanDistance(grid.getLocation(o), grid.getLocation(this));
				if(dist < minDist) {
					minDist = dist;
				}
			}
		}
		return minDist;
	}
	
	public BusStop getNearestBusStop() {
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		BusStop nearest = null;
		for(Object o: grid.getObjects()){
			if(o instanceof BusStop) {
				dist = Tools.manhattanDistance(grid.getLocation(o), grid.getLocation(this));
				if(dist < minDist) {
					minDist = dist;
					nearest = (BusStop) o;
				}
			}
		}
		return nearest;
	}
	
	public Double getDistanceToNearestSpawn() {
		Double minDist = Double.MAX_VALUE;
		Double dist = 0d;
		for(Object o: grid.getObjects()){
			if(o instanceof Spawn) {
				dist = Tools.manhattanDistance(grid.getLocation(o), grid.getLocation(this));
				if(dist < minDist) {
					minDist = dist;
				}
			}
		}
		return minDist;
	}
	
}