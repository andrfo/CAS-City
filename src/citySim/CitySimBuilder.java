package citySim;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.media.protocol.SourceTransferHandler;

import citySim.agent.Bus;
import citySim.agent.Vehicle;
import utils.Tools;
import utils.Vector2D;
import citySim.environment.*;
import citySim.environment.roads.BusStop;
import citySim.environment.roads.Despawn;
import citySim.environment.roads.NorthEastRoad;
import citySim.environment.roads.ParkingSpace;
import citySim.environment.roads.Road;
import citySim.environment.roads.RoundaboutRoad;
import citySim.environment.roads.SideWalk;
import citySim.environment.roads.SouthWestRoad;
import citySim.environment.roads.Spawn;
import citySim.environment.electric.*;
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
import utils.*;
import utils.Kruskal.EDGE;

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
	
	
	//Images
	BufferedImage cityImg = null;
	BufferedImage gridImg = null;
	BufferedImage roadImg = null;
	
	RegionalGridNode globalNode;
	
	
	ContinuousSpace<Object> space;
	Grid<Object> grid;
	
	Spawner spawner;
	
	/**
	 * Builds the context for the Repast framework. This is where all the initialization happens
	 */
	@Override
	public Context build(Context<Object> context) {
		
		//Reads the images
		try {
		    cityImg = ImageIO.read(new File("C:/Users/andrfo/Documents/Git/CAS-City/maps/smallTrondheimSmallPark.png"));
		} catch (IOException e) {
			System.out.println("There was an error while loading the city traffic map: " + e);
		}
		try {
		    gridImg = ImageIO.read(new File("C:/Users/andrfo/Documents/Git/CAS-City/maps/overlays/smallTrondheimEL.png"));
		} catch (IOException e) {
			System.out.println("There was an error while loading the city electric grid map: " + e);
		}
		try {
			roadImg = ImageIO.read(new File("C:/Users/andrfo/Documents/Git/CAS-City/maps/overlays/smallTrondheimRoads.png"));
		} catch (IOException e) {
			System.out.println("There was an error while loading the city road weight overlay map: " + e);
		}
		width = cityImg.getWidth();
		height = cityImg.getHeight();
		
		
		context.setId("CitySim");

		//Setting up the networks
		NetworkBuilder<Object> roadNetBuilder = new NetworkBuilder<Object>("road network", context, true);
		NetworkBuilder<Object> debugNetBuilder = new NetworkBuilder<Object>("debug network", context, true);
		NetworkBuilder<Object> gridNetBuilder = new NetworkBuilder<Object>("electric network", context, true);
		roadNetBuilder.buildNetwork();
		gridNetBuilder.buildNetwork();
		debugNetBuilder.buildNetwork();
		
		
		//Setting up the Euclidean space and grid
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
		
		//This is the clock in the simulatino
		InformationLabel info = new InformationLabel(space, grid, context);
		context.add(info);
		space.moveTo(info, width - 15, height - 15);
		grid.moveTo(info, width - 15, height - 15);
		
		//This is the Electric meter in the simulation
		globalNode = new RegionalGridNode(space, grid);
		context.add(globalNode);
		space.moveTo(globalNode, width - 50, height - 15);
		grid.moveTo(globalNode, width - 50, height - 15);
		
		//Read the images and do stuff with the pixels
		readCityImage(space, grid, context);
		readGridImage(space, grid, context);
		
		//Initialize all the electric entities and have it propagate
		for(Object o: context.getObjects(ElectricEntity.class)) {
			((ElectricEntity)o).init();
		}
		
		return context;
	}
	
	/**
	 * Reads the city image pixel by pixel and places the appropriate objects into the simulation based on pixel colors. 
	 * Then proceeds to build the road network and other stuff
	 * @param space
	 * @param grid
	 * @param context
	 */
	private void readCityImage(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context) {
		
		//Lists
		Road[] spawnPoints = new Road[4];
		List<Road> despawnPoints = new ArrayList<Road>();
		List<Road> parkingSpaces = new ArrayList<Road>();
		List<Road> sideWalks = new ArrayList<Road>();
		List<Road> parkingNexi = new ArrayList<Road>();
		List<Road> parkingNexiRoads = new ArrayList<Road>();
		List<BusStop> busStops = new ArrayList<BusStop>();
		List<Building> buildings = new ArrayList<Building>();
		
		//Reading the image and creating objects
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				//flip image in y direction
				int x = j;
				int y = height - 1 - i;
				
				//get pixel value
				int p = cityImg.getRGB(j,i);
				Double roadWeight = getRoadOverlayWeight(j, i);
				
				//get alpha
				int a = (p>>24) & 0xff;
				
				//get red
				int r = (p>>16) & 0xff;
				
				//get green
				int g = (p>>8) & 0xff;
				
				//get blue
				int b = p & 0xff;
				if(r == 255 && g == 255 && b == 255) {//Nothing
					continue;
				}
				else if(r == 0 && g == 0 && b == 255) {//Road, direction North-East
					NorthEastRoad road = new NorthEastRoad(space, grid);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					road.setWeight(roadWeight); //Set the weight according to the road overlay image
					
				}
				else if(r == 0 && g == 0 && b == 0) {//Road, direction South-West
					SouthWestRoad road = new SouthWestRoad(space, grid);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					road.setWeight(roadWeight); //Set the weight according to the road overlay image
					
				}
				
//				else if(r == 0 && g == 255 && b == 0) {//Start
//					Spawn road = new Spawn(space, grid, context);
//					context.add(road);
//					space.moveTo(road, x, y);
//					grid.moveTo(road, x, y);
//					spawnPoints.add(road);
//				}
				
				//=======================================================
				//Experiment specific spawn points in order to number them and vary load distributions
				
				else if(r == 1 && g == 255 && b == 0) {//Start 1/4
					Spawn road = new Spawn(space, grid, context);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					spawnPoints[0] = road;
				}
				else if(r == 2 && g == 255 && b == 0) {//Start 2/4
					Spawn road = new Spawn(space, grid, context);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					spawnPoints[1] = road;
				}
				else if(r == 3 && g == 255 && b == 0) {//Start 3/4
					Spawn road = new Spawn(space, grid, context);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					spawnPoints[2] = road;
				}
				else if(r == 4 && g == 255 && b == 0) {//Start 4/4
					Spawn road = new Spawn(space, grid, context);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					spawnPoints[3] = road;
				}
				
				
				//=======================================================
				
				
				else if(r == 255 && g == 0 && b == 0) {//end
					Despawn road = new Despawn(space, grid);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					despawnPoints.add(road);
				}
				else if(r >= 250 && g <= 10 && b >= 250) {//roundabout
					RoundaboutRoad road = new RoundaboutRoad(space, grid);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					road.setWeight(roadWeight);
				}
				else if(r == 0 && g == 255 && b == 255) {//Parking Space
					ParkingSpace road = new ParkingSpace(space, grid);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					parkingSpaces.add(road);
				}
				else if(r == 0 && g == 64 && b == 0) {//Bus stop
					BusStop road = new BusStop(space, grid);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					busStops.add(road);
				}
				else if(r == 255 && g == 128 && b == 0) {//Side Walk
					SideWalk road = new SideWalk(space, grid);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					sideWalks.add(road);
				}
				else if(r == 128 && g == 64 && b == 0) {//Building
					//TODO: make buildings be more than one pixel
					Building building = new Building(space, grid);
					context.add(building);
					space.moveTo(building, x, y);
					grid.moveTo(building, x, y);
					buildings.add(building);
				}
				else if(r == 0 && g == 162 && b == 232) {//Parking nexus
					Road road = new Road(space, grid);
					context.add(road);
					space.moveTo(road, x, y);
					grid.moveTo(road, x, y);
					parkingNexi.add(road);
				}
				else {
					System.out.println("r: " + r + " g: " + g + " b: " + b);
				}
				
			}
		}
		//Builds the structure of the roundabouts once all the other roads have been placed
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				
				Object obj = grid.getObjectAt(x, y);
				if(!(obj instanceof Road)) {
					continue;
				}
				Road r = (Road)obj;
				if(r instanceof RoundaboutRoad) {
					buildRoundabout(r, context);							
				}
			}
		}
		
		//Sets up the parking nexi(Places cars go to look for parking) and removes them from the simulation 
		//as they need not be visible
		for(Road r : parkingNexi) {
			parkingNexiRoads.add(buildParkingNexus(r));
			context.remove(r);
		}
		
		//Builds the road network
		buildCityGraph(grid, context);
		
		//Sets up the spawner agent(handles all the spawning).
		spawner = new Spawner(
				space, 
				grid, 
				context, 
				spawnPoints, 
				despawnPoints, 
				parkingSpaces, 
				buildings, 
				busStops, 
				parkingNexiRoads);
		context.add(spawner);
		context.add(spawner.getReporter());
	}
	
	/**
	 * Reads the electric grid overlay image and adds the appropriate objects to the simulation, the proceeds to
	 * do the clustering and tree building
	 * @param space
	 * @param grid
	 * @param context
	 */
	private void readGridImage(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context) {
		
		//Temp lists
		List<Charger> chargers = new ArrayList<Charger>();
		List<Substation> substations = new ArrayList<Substation>();
		List<Building> buildings = new ArrayList<Building>();
		
		
		//Reading the image
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				//flip image in y direction
				int x = j;
				int y = height - 1 - i;
				
				//get pixel value
				int p = gridImg.getRGB(j,i);
				
				//get alpha
				int a = (p>>24) & 0xff;
				
				//get red
				int r = (p>>16) & 0xff;
				
				//get green
				int g = (p>>8) & 0xff;
				
				//get blue
				int b = p & 0xff;
				if(r == 255 && g == 255 && b == 255) {//Nothing
					continue;
				}
				else if(r == 181 && g == 230 && b == 29) {//Charger
					Charger charger = new Charger(space, grid);
					context.add(charger);
					space.moveTo(charger, x, y);
					grid.moveTo(charger, x, y);
					chargers.add(charger);
					
				}
				else if(r == 128 && g == 64 && b == 0) {//Building
					//Gets the building already added by the city reader
					Building building = (Building) Tools.getObjectAt(grid, Building.class, x, y);
					buildings.add(building);
				}
				else {
//					System.out.println("r: " + r + " g: " + g + " b: " + b);
				}
				
			}
		}
		
		//Gathers the entities into a single list for clustering
		List<GridPoint> data = new ArrayList<GridPoint>();
		for(Building b: buildings) {
			data.add(grid.getLocation(b));
		}
		for(Charger s: chargers) {
			data.add(grid.getLocation(s));
		}
		
		//Creating clusters for the placement of substations and adding members to them
		Clustering c = new Clustering(data, 0, 0, width, height, 5);
		for(GridPoint p: c.kMeans()) {
			Substation substation = new Substation(space, grid);
			context.add(substation);
			space.moveTo(substation, p.getX(), p.getY());
			grid.moveTo(substation, p.getX(), p.getY());
			substations.add(substation);
		}
		
		buildElectricGraph(grid, context, c.getClusters());
	}
	
	/**
	 * builds a graph with edges between neighboring roads
	 * primarily used for path finding
	 * @param grid
	 * @param context
	 * @param goals
	 * @param spawnPoints
	 */
	private Network<Object> buildCityGraph(Grid<Object> grid, Context<Object> context) {
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
					
					if(r instanceof NorthEastRoad && cr instanceof NorthEastRoad) {
						int dir = Tools.getMooreDirection(grid.getLocation(r), grid.getLocation(cr));
						if(		dir == Tools.NORTH	 ||			// x x x
								dir == Tools.EAST) {
							addEdge(r, cr, net);
						}
					}
					else if(r instanceof SouthWestRoad && cr instanceof SouthWestRoad) {
						int dir = Tools.getMooreDirection(grid.getLocation(r), grid.getLocation(cr));
						if(		dir == Tools.WEST		 ||		// x 0 0
								dir == Tools.SOUTH) {
							addEdge(r, cr, net);
						}
					}
					//Connect spawn and despawn to everything around them
					//TODO: Clean up
					if(r instanceof Spawn){
						addEdge(r, cr, net).setWeight(50);
					}
					else if(cr instanceof Despawn){
						addEdge(r, cr, net).setWeight(50);
					}
					else if(r instanceof ParkingSpace &&
							(!(cr instanceof ParkingSpace) && !(cr instanceof SideWalk))){
						addEdge(r, cr, net).setWeight(50);
						addEdge(cr, r, net).setWeight(50);
					}
					else if(r instanceof BusStop &&
							!(cr instanceof BusStop)){
						addEdge(r, cr, net).setWeight(50);
						addEdge(cr, r, net).setWeight(50);
					}
					else if(r instanceof RoundaboutRoad && 
							cr instanceof RoundaboutRoad) {
						
						
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
					else if(r instanceof RoundaboutRoad) {
						int dir = Tools.getMooreDirection(grid.getLocation(r), grid.getLocation(cr));
						if(		dir == Tools.NORTH	 ||		// points:
								dir == Tools.EAST	 ||		// 0 x 0
								dir == Tools.WEST 	 ||		// x 0 x
								dir == Tools.SOUTH) {		// 0 x 0	
							if(cr.isExit()) {
								addEdge(r, cr, net);
							}
							else if(cr instanceof NorthEastRoad || cr instanceof SouthWestRoad) {
								addEdge(cr, r, net);
							}
						}
					}
				}
			}
		}
		return net;
	}
	
	/**
	 * Builds a tree structure from the clusters.
	 * @param grid
	 * @param context
	 * @param clusters
	 * @return
	 */
	private Network<Object> buildElectricGraph(Grid<Object> grid, Context<Object> context, ArrayList<ArrayList<GridPoint>> clusters) {
		//Get network
		Network<Object> net = (Network<Object>)context.getProjection("electric network");
		
		//Substations
		ArrayList<ElectricEntity> subs = new ArrayList<ElectricEntity>();
		
		//Goes through the clusters(which are grid points) and find the objects at their members' locations
		//Creates spanning trees within and of these cluster and connects them together.
		for(ArrayList<GridPoint> cluster: clusters) {
			ArrayList<ElectricEntity> clusterEntities = new ArrayList<ElectricEntity>();
			
			//The location of the centroid of the cluster
			GridPoint ps = cluster.remove(0); 
			
			//The Substation located at the centroid
			Substation s = (Substation) Tools.getObjectAt(grid, Substation.class, ps.getX(), ps.getY());
			subs.add(s);
			
			
			ElectricEntity closest = null;
			double minDist = Double.MAX_VALUE;
			//Goes through all the entities in the cluster and creates a minimal spanning tree of them based on distance
			for(GridPoint p: cluster) {
				ElectricEntity e = null;
				for(Object o: grid.getObjectsAt(p.getX(), p.getY())) {
					if(!(o instanceof Substation) && o instanceof ElectricEntity) {
						e = (ElectricEntity) o;
					}
				}
				double distance = Tools.gridDistance(p, ps);
				if(distance < minDist) {
					closest = e;
					minDist = distance;
				}
				clusterEntities.add(e);
			}
			net.addEdge(s, closest);
			closest.setParent(s);
			spanningTree(clusterEntities, net, closest);
		}
		spanningTree(subs, net, subs.get(0));
		
		subs.get(0).setParent(globalNode);
//		net.addEdge(globalNode, subs.get(0));
		
		return net;
	}
	
	/**
	 * Creates a minimum spanning tree of the entities from the root based on euclidean distance
	 * Creates the edges in the network corresponding to the tree
	 * @param entities
	 * @param net
	 * @param root
	 */
	private void spanningTree(ArrayList<ElectricEntity> entities, Network<Object> net, ElectricEntity root) {
		
		//Creates an adjacency matrix as a helpful data structure for later
		int [][] adjacencyMatrix = new int[entities.size()][entities.size()];
		for(int i = 0; i < entities.size(); i++) {
			for(int j = 0; j < entities.size(); j++) {
				adjacencyMatrix[i][j] = 0;
			}
		}
		
		//Set up nodes and edges for MST
		char[] vertices = new char[entities.size()];
		ArrayList<EDGE> edges = new ArrayList<EDGE>();
		for(int i = 0; i < entities.size(); i++) {
			vertices[i] = (char) i;
		}
		for(int i = 0; i < entities.size(); i++) {
			for(int j = 0; j < entities.size(); j++) {
				edges.add(new EDGE((char)i, (char)j, (int) Math.ceil(
								Tools.gridDistance(
										entities.get(i).getLocation(), 
										entities.get(j).getLocation()))));
			}
		}
		//Call Kruskal Algorithm
		ArrayList<EDGE> mst = Kruskal.kruskal(vertices, edges.toArray(new EDGE[entities.size()]));
		
		//Fill the adjacency matrix with the edges provided from Kruskals
		for(EDGE e: mst) {
			adjacencyMatrix[e.from][e.to] = 1;
			adjacencyMatrix[e.to][e.from] = 1;
		}
		
		//Traversing the tree depth first using the adjacency matrix
		//Setting edges in the network and setting parents along the way
		int source = entities.indexOf(root);
		boolean[] visited = new boolean[adjacencyMatrix.length];
        visited[source] = true;
        Queue<Integer> queue = new LinkedList<>();
        queue.add(source);
        while(!queue.isEmpty()){
            int x = queue.poll();
            for(int i=0; i<adjacencyMatrix.length;i++){
                if(adjacencyMatrix[x][i] != 0 && visited[i] == false){
                    queue.add(i);
                    visited[i] = true;
                    if(entities.get(x) == null) {
                    	System.out.println("x is null");
                    }
                    if(entities.get(i) == null) {
                    	System.out.println("i is null");
                    }
                    net.addEdge(entities.get(x), entities.get(i));
                    entities.get(i).setParent(entities.get(x));
                }
            }
        }
	}
	
	/**
	 * Takes in a Road and builds a roundabout if it is not already a member of one
	 * @param r
	 * @param context
	 */
	private void buildRoundabout(Road r, Context<Object> context) {
		if(r.getRoundabout() == null) {
			Roundabout roundabout = new Roundabout(space, grid);	
			context.add(roundabout);
			space.moveTo(roundabout, space.getLocation(r).getX(), space.getLocation(r).getY());
			grid.moveTo(roundabout, grid.getLocation(r).getX(), grid.getLocation(r).getY());
			recursiveBuildRoundabout(roundabout, r);
		}
	}
	
	/**
	 * sets the nearest road to be a goal when looking for parking
	 * @param road
	 * @return returns the road chosen
	 */
	private Road buildParkingNexus(Road road) {
		GridPoint pt = grid.getLocation(road);
		GridCellNgh<Road> roadNghCreator = new GridCellNgh<Road>(grid, pt, Road.class, 4, 4);
		List<GridCell<Road>> roadGridCells = roadNghCreator.getNeighborhood(true);
		for (GridCell<Road> gridCell : roadGridCells) {
			if(gridCell.items().iterator().hasNext()) {
				Road r = gridCell.items().iterator().next();
				if(r instanceof SouthWestRoad || r instanceof NorthEastRoad) {
					return r;
				}
			}
		}
		return null;
	}
	
	/**
	 * Crawls around finding adjacent roundabout roads to add to the roundabout object.
	 * In a later init the roundabout object will connect up its roads properly
	 * @param roundabout
	 * @param r
	 */
	private void recursiveBuildRoundabout(Roundabout roundabout, Road r) {
		GridCellNgh<Road> nghCreator = new GridCellNgh<Road>(grid, grid.getLocation(r), Road.class, 1, 1);
		List<GridCell<Road>> gridCells = nghCreator.getNeighborhood(true);
		
		for (GridCell<Road> gridCell : gridCells) {
			if(gridCell.items().iterator().hasNext()) {
				Road road = gridCell.items().iterator().next();	
				if(!(road instanceof RoundaboutRoad)) {
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
	
	/**
	 * Gets the defined weight based on the color in the overlay image for the roads
	 * @param x
	 * @param y
	 * @return Double, the weight 
	 */
	private Double getRoadOverlayWeight(int x, int y) {
		int p = roadImg.getRGB(x,y);
		
		//get alpha
		int a = (p>>24) & 0xff;
		
		//get red
		int r = (p>>16) & 0xff;
		
		//get green
		int g = (p>>8) & 0xff;
		
		//get blue
		int b = p & 0xff;
		
		if(r == 34 && g == 177 && b == 76) {//GREEN, main road
			return Tools.MAIN_ROAD_WEIGHT;
		}
		else if(r == 255 && g == 242 && b == 0) {//YELLOW, standard road
			return Tools.STD_ROAD_WEIGHT;
		}
		else if(r == 255 && g == 127 && b == 39) {//ORANGE, small road
			return Tools.SMALL_ROAD_WEIGHT;
		}
		else if(r == 237 && g == 28 && b == 36) {//RED, alley road
			return Tools.ALLEY_ROAD_WEIGHT;
		}
		return null;
		
	}
	
	/**
	 * Ads an edge in the road network from a to b and sets the correct weight
	 * @param a
	 * @param b
	 * @param net
	 * @return The edge that is created
	 */
	private RepastEdge<Object> addEdge(Object a, Object b, Network<Object> net) {
		if(net.getEdge(a, b) == null) {
			RepastEdge<Object> edge = net.addEdge(a, b);
			int dir = Tools.getMooreDirection(grid.getLocation(a), grid.getLocation(b));
			if(		dir == Tools.NORTH	 ||		// points:
					dir == Tools.EAST	 ||		// 0 x 0
					dir == Tools.WEST 	 ||		// x 0 x
					dir == Tools.SOUTH) {		// 0 x 0
				
				if(((Road) a).getWeight() == null && ((Road) b).getWeight() == null) {
					edge.setWeight(1.0d);
				}
				else {
					edge.setWeight(Tools.max(((Road) a).getWeight(), ((Road) b).getWeight()));
				}
			}
			else {
				if(((Road) a).getWeight() == null && ((Road) b).getWeight() == null) {
					edge.setWeight(1.3); //More expensive to travel diagonally
				}
				else {
					edge.setWeight(Tools.max(((Road) a).getWeight(), ((Road) b).getWeight()) * 1.3);
				}
			}
			return edge;
		}
		return null;
	}

}
