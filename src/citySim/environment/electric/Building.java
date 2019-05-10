package citySim.environment.electric;

import java.util.ArrayList;
import java.util.List;

import citySim.agent.Person;
import citySim.environment.Entity;
import citySim.environment.roads.BusStop;
import citySim.environment.roads.Spawn;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import utils.Tools;

public class Building extends ElectricEntity{

	private Double unitLoad;
	private Double totalLoad;
	private List<Person> occupants;
	
	public Building(ContinuousSpace<Object> space, Grid<Object> grid) {
		super(space, grid);
		this.unitLoad = 3d;//TODO: research
		this.totalLoad = 3d;
		this.parent = null;
		this.occupants = new ArrayList<Person>();
		this.grid = grid;
		this.space = space;
		// TODO Auto-generated constructor stub
	}
	
	
	
	public void addOccupants(Person p) {
		occupants.add(p);
		Double newValue = Double.valueOf(totalLoad) + unitLoad;
//		System.out.println("Arrived, propagating update:");
		update(newValue - Double.valueOf(totalLoad));
		totalLoad = newValue;
	}
	
	public void removeOccupants(Person p) {
		if(occupants.contains(p)) {
			occupants.remove(p);
			Double newValue = Double.valueOf(totalLoad) - unitLoad;
			update(newValue - Double.valueOf(totalLoad));
			totalLoad = newValue;
		}
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