package citySim;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import citySim.agent.Car;
import utils.Tools;
import utils.Vector2D;
import citySim.environment.Junction;
import citySim.environment.Road;
import citySim.environment.Roundabout;
import citySim.environment.Spawner;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

/**
 * @author andrfo
 *
 */
public class CitySimBuilder implements ContextBuilder<Object> {

	int width;
	int height;
	/*
	 * 		0 1 2
	 * 		3 8 4
	 * 		5 6	7
	 */
	
	
	
	BufferedImage img = null;
	
	ContinuousSpace<Object> space;
	Grid<Object> grid;
	
	Spawner spawner;
	
	@Override
	public Context build(Context<Object> context) {
		
		try {
		    img = ImageIO.read(new File("maps/tondheim.png"));
		} catch (IOException e) {
			System.out.println(e + ": Image file not found!");
		}
		width = img.getWidth();
		height = img.getHeight();
		
		context.setId("CitySim");
		//TODO: Change to be directed when 2way is implemented
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("road network", context, true);
		NetworkBuilder<Object> debugNetBuilder = new NetworkBuilder<Object>("debug network", context, true);
		netBuilder.buildNetwork();
		debugNetBuilder.buildNetwork();
		
		ContinuousSpaceFactory spaceFactory = 
				ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		space = spaceFactory.createContinuousSpace(
				"space", 
				context, 
				new SimpleCartesianAdder<Object>(), 
				new repast.simphony.space.continuous.WrapAroundBorders(), 
				width + 10, 
				height + 10);
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		grid = gridFactory.createGrid(
				"grid", 
				context, 
				new GridBuilderParameters<Object>(
						new WrapAroundBorders(), 
						new SimpleGridAdder<Object>(),//TODO: Change adder?
						true,
						width + 10, 
						height + 10));
		
		
		readImage(space, grid, context);
		
		return context;
	}
	
	private void readImage(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context) {
		
		List<Road> spawnPoints = new ArrayList<Road>();
		List<Road> goals = new ArrayList<Road>();
		
		
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				//flip image in y direction
				int x = j;
				int y = height - 1 - i;
				
				//get pixel value
				int p = img.getRGB(j,i);
				
				//get alpha
				int a = (p>>24) & 0xff;
				
				//get red
				int r = (p>>16) & 0xff;
				
				//get green
				int g = (p>>8) & 0xff;
				
				//get blue
				int b = p & 0xff;
				if(r >= 250 && g >= 250 && b >= 250) {//Nothing
					continue;
				}
				else if(r < 10 && g < 10 && b > 250) {//Road, direction North-East
					Road road = new Road(space, grid);
					road.setType("roadNE");
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					
				}
				else if(r < 10 && g < 10 && b < 10) {//Road, direction South-West
					Road road = new Road(space, grid);
					road.setType("roadSW");
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					
				}
				else if(r <= 10 && g >= 250 && b <= 10) {//Start
					Road road = new Road(space, grid);
					context.add(road);
					road.setType("spawn");
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					spawnPoints.add(road);
				}
				else if(r >= 250 && g <= 10 && b <= 10) {//end
					Road road = new Road(space, grid);
					road.setType("despawn");
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					goals.add(road);
				}
				else if(r >= 250 && g >= 250 && b <= 10) {//junction
					Road road = new Road(space, grid);
					road.setType("junction");
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
				}
				else if(r >= 250 && g <= 10 && b >= 250) {//roundabout
					Road road = new Road(space, grid);
					road.setType("roundabout");
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
				}
				else {
					System.out.println("r: " + r + " g: " + g + " b: " + b);
				}
				
			}
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				
				Object obj = grid.getObjectAt(x, y);
				if(!(obj instanceof Road)) {
					continue;
				}
				Road r = (Road)obj;
				
				if(r.getType().equals("junction")) {
					buildJunction(r, context);							
				}
				if(r.getType().equals("roundabout")) {
					buildRoundabout(r, context);							
				}
			}
		}
		buildGraph(grid, context);
		spawner = new Spawner(space, grid, context, spawnPoints, goals);
		context.add(spawner);
	}
	
	/**
	 * builds a graph with edges between neighboring roads
	 * primarily used for path finding
	 * @param grid
	 * @param context
	 * @param goals
	 * @param spawnPoints
	 */
	private Network<Object> buildGraph(Grid<Object> grid, Context<Object> context) {
		//Get network
		Network<Object> net = (Network<Object>)context.getProjection("road network");
		
		
		//iterate over all roads in sim
		for (Object obj : context) {
			if(obj instanceof Road) {
				Road r = (Road) obj;
				GridPoint pt = grid.getLocation(r);
				
				//use the GridCellNgh class to create GridCells 
				// for the surrounding neighborhood.
				GridCellNgh<Road> nghCreator = new GridCellNgh<Road>(grid, pt, Road.class, 1, 1);
				List<GridCell<Road>> gridCells = nghCreator.getNeighborhood(false);
				
				//TODO: Clean up code
				for (GridCell<Road> cell : gridCells) {
					if(cell.size() <= 0) {
						continue;
					}
					Road cr = cell.items().iterator().next();
					
					if(r.getType().equals("roadNE") && cr.getType().equals("roadNE")) {
						int dir = Tools.getMooreDirection(grid.getLocation(r), grid.getLocation(cr));
						if(		dir == Tools.NORTHWEST ||		// points:
								dir == Tools.NORTH	 ||			// x x x
								dir == Tools.NORTHEAST ||		// 0 0 x
								dir == Tools.EAST		 ||		// 0 0 x
								dir == Tools.SOUTHEAST) {
							addEdge(r, cr, net);
						}
					}
					else if(r.getType().equals("roadSW") && cr.getType().equals("roadSW")) {
						int dir = Tools.getMooreDirection(grid.getLocation(r), grid.getLocation(cr));
						if(		dir == Tools.NORTHWEST ||		// points:
								dir == Tools.WEST		 ||		// x 0 0
								dir == Tools.SOUTHWEST ||		// x 0 0
								dir == Tools.SOUTH	 ||			// x x x
								dir == Tools.SOUTHEAST) {
							addEdge(r, cr, net);
						}
					}
					//Connect spawn and despawn to everything around them
					//TODO: Clean up
					if(r.getType().equals("spawn")){
						addEdge(r, cr, net);
					}
					else if(cr.getType().equals("despawn")){
						addEdge(r, cr, net);
					}
					else if(r.getType().equals("junction") || 
							cr.getType().equals("junction")) {
						addEdge(r, cr, net);
					}
					else if(r.getType().equals("roundabout") && 
							cr.getType().equals("roundabout")) {
						
						
						//(a.x - center.x) * (b.y - center.y) - (b.x - center.x) * (a.y - center.y)
						double ax = space.getLocation(r).getX();
						double ay = space.getLocation(r).getY();
						
						double bx = space.getLocation(cr).getX();
						double by = space.getLocation(cr).getY();
						
						double cx = r.getRoundabout().getCenter().getX();
						double cy = r.getRoundabout().getCenter().getY();
						
						double s = ( (ax - cx) * (by - cy) ) - ( (bx - cx)*(ay - cy));
						
						if(s > 0.0) {
							int dir = Tools.getMooreDirection(grid.getLocation(r), grid.getLocation(cr));
							if(		dir == Tools.NORTH	 ||		// points:
									dir == Tools.EAST	 ||		// 0 x 0
									dir == Tools.WEST 	 ||		// x 0 x
									dir == Tools.SOUTH) {		// 0 x 0
								addEdge(r, cr, net);	
							}
						}
					}
					else if(r.getType().equals("roundabout")) {
						int dir = Tools.getMooreDirection(grid.getLocation(r), grid.getLocation(cr));
						if(		dir == Tools.NORTH	 ||		// points:
								dir == Tools.EAST	 ||		// 0 x 0
								dir == Tools.WEST 	 ||		// x 0 x
								dir == Tools.SOUTH) {		// 0 x 0	
							if(cr.isExit()) {
								addEdge(r, cr, net);
							}
							else if(cr.getType().equals("roadNE") || cr.getType().equals("roadSW")) {
								addEdge(cr, r, net);
							}
						}
					}
				}
			}
		}
		return net;
	}
	
	private void buildJunction(Road r, Context<Object> context) {
		if(r.getJunction() == null) {
			Junction junction = new Junction(space, grid);	
			context.add(junction);
			space.moveTo(junction, space.getLocation(r).getX(), space.getLocation(r).getY());
			grid.moveTo(junction, grid.getLocation(r).getX(), grid.getLocation(r).getY());
			recursiveBuildJunction(junction, r);
		}
	}
	
	private void buildRoundabout(Road r, Context<Object> context) {
		if(r.getRoundabout() == null) {
			Roundabout roundabout = new Roundabout(space, grid);	
			context.add(roundabout);
			space.moveTo(roundabout, space.getLocation(r).getX(), space.getLocation(r).getY());
			grid.moveTo(roundabout, grid.getLocation(r).getX(), grid.getLocation(r).getY());
			recursiveBuildRoundabout(roundabout, r);
		}
	}
	
	
	private void recursiveBuildRoundabout(Roundabout roundabout, Road r) {
		GridCellNgh<Road> nghCreator = new GridCellNgh<Road>(grid, grid.getLocation(r), Road.class, 1, 1);
		List<GridCell<Road>> gridCells = nghCreator.getNeighborhood(true);
		
		for (GridCell<Road> gridCell : gridCells) {
			if(gridCell.items().iterator().hasNext()) {
				Road road = gridCell.items().iterator().next();	
				if(!road.getType().equals("roundabout")) {
					roundabout.addEdgeRoad(road);
					road.setJunctionEdge(true);
					road.setRoundabout(roundabout);
					continue;
				}
				if(road.getRoundabout() == null) {
					road.setRoundabout(roundabout);
					roundabout.addRoad(road);
					
					recursiveBuildRoundabout(roundabout, road);
				}
			}
		}
	}
	
	private void recursiveBuildJunction(Junction junction, Road r) {
		GridCellNgh<Road> nghCreator = new GridCellNgh<Road>(grid, grid.getLocation(r), Road.class, 1, 1);
		List<GridCell<Road>> gridCells = nghCreator.getNeighborhood(true);
		
		for (GridCell<Road> gridCell : gridCells) {
			if(gridCell.items().iterator().hasNext()) {
				Road road = gridCell.items().iterator().next();	
				if(!road.getType().equals("junction")) {
					junction.addEdgeRoad(road);
					road.setJunction(junction);
					road.setJunctionEdge(true);
					continue;
				}
				if(road.getJunction() == null) {
					road.setJunction(junction);
					junction.addRoad(road);
					
					recursiveBuildJunction(junction, road);
				}
			}
		}
		
	}
	
	private void addEdge(Object a, Object b, Network<Object> net) {
		if(net.getEdge(a, b) == null) {
			RepastEdge<Object> edge = net.addEdge(a, b);
			int dir = Tools.getMooreDirection(grid.getLocation(a), grid.getLocation(b));
			if(		dir == Tools.NORTH	 ||		// points:
					dir == Tools.EAST	 ||		// 0 x 0
					dir == Tools.WEST 	 ||		// x 0 x
					dir == Tools.SOUTH) {		// 0 x 0
				edge.setWeight(1.0);
			}
			else {
				edge.setWeight(1.3);
			}
		}
	}

}
