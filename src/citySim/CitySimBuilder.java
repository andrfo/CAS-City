package citySim;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import citySim.agent.Car;
import citySim.environment.Road;
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
import repast.simphony.space.graph.ShortestPath;
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
	
	Spawner spawner;
	
	@Override
	public Context build(Context<Object> context) {
		context.setId("CitySim");
		//TODO: Change to be directed when 2way is implemented
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("road network", context, false);
		netBuilder.buildNetwork();
		
		ContinuousSpaceFactory spaceFactory = 
				ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", 
				context, 
				new SimpleCartesianAdder<Object>(), 
				new repast.simphony.space.continuous.WrapAroundBorders(), 
				200, 200);
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid(
				"grid", 
				context, 
				new GridBuilderParameters<Object>(
						new WrapAroundBorders(), 
						new SimpleGridAdder<Object>(),//TODO: Change adder?
						true,
						200,
						200));
		
		
		readImage(space, grid, context);
		
		
		
		//TODO: add Entities
		
		//TODO: add Agents
		
		return context;
	}
	
	//black = pixel.r == 0 && pixel.g == 0 && pixel.b == 0;
    // white = pixel.r > 0.5 && pixel.g > 0.5 && pixel.b > 0.5;
    // green = pixel.r < 0.5 && pixel.g > 0.5 && pixel.b < 0.5;
    // red = pixel.r > 0.5 && pixel.g < 0.5 && pixel.b < 0.5;
	
	private void readImage(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context) {
		BufferedImage img = null;
		List<Road> spawnPoints = new ArrayList<Road>();
		List<Road> goals = new ArrayList<Road>();
		try {
		    img = ImageIO.read(new File("maps/3-2.png"));
		} catch (IOException e) {
			System.out.println(e + ": Image file not found!");
		}
		width = img.getWidth();
		height = img.getHeight();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				//get pixel value
				int p = img.getRGB(x,y);
				
				//get alpha
				int a = (p>>24) & 0xff;
				
				//get red
				int r = (p>>16) & 0xff;
				
				//get green
				int g = (p>>8) & 0xff;
				
				//get blue
				int b = p & 0xff;
				if(r >= 200 && g >= 200 && b >= 200) {//Nothing
					continue;
				}
				else if(r < 100 && g < 100 && b < 100) {//Road
					Road road = new Road(space, grid);
					road.setType("simple");
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					
				}
				else if(r <= 150 && g >= 150 && b <= 150) {//Start
					Road road = new Road(space, grid);
					context.add(road);
					road.setType("spawn");
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					spawnPoints.add(road);
				}
				else if(r >= 150 && g <= 150 && b <= 150) {//end
					Road road = new Road(space, grid);
					road.setType("despawn");
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					goals.add(road);
				}
				else {
					System.out.println("r: " + r + " g: " + g + " b: " + b);
				}
				
			}
		}
		buildGraph(grid, context, goals, spawnPoints);
		System.out.println("Setting up Spawner");
		spawner = new Spawner(space, grid, context, spawnPoints, goals);
		context.add(spawner);
		System.out.println("Done");
	}
	
	/**
	 * builds a graph with edges between neighbouring roads
	 * @param grid
	 * @param context
	 * @param goals
	 * @param spawnPoints
	 */
	private Network<Object> buildGraph(Grid<Object> grid, Context<Object> context, List<Road> goals, List<Road> spawnPoints) {
		//Get network
		Network<Object> net = (Network<Object>)context.getProjection("road network");
		//iterate over all roads in sim
		for (Object obj : context) {
			if(obj instanceof Road) {
				//do stuff with start and goals?
				Road r = (Road) obj; //??
				GridPoint pt = grid.getLocation(r);
				
				//use the GridCellNgh class to create GridCells 
				// for the surrounding neighbourhood.
				GridCellNgh<Road> nghCreator = new GridCellNgh<Road>(grid, pt, Road.class, 1, 1);
				List<GridCell<Road>> gridCells = nghCreator.getNeighborhood(true);
				
				for (GridCell<Road> cell : gridCells) {
					if(cell.size() <= 0) {
						continue;
					}
					Road cr = cell.items().iterator().next();
					if(net.getEdge(r, cr) == null) {
						net.addEdge(r, cr);
					}
				}
			}
		}
		return net;
	}

}
