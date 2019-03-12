package electricGrid;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import environment.Building;
import environment.Charger;
import environment.Entity;
import environment.Substation;
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
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import utility.Clustering;
import utility.MST;
import utility.MST.Edge;
import utility.Tools;

public class GridBuilder implements ContextBuilder<Object>{

	BufferedImage img = null;
	int width;
	int height;
	
	ContinuousSpace<Object> space;
	Grid<Object> grid;
	
	@Override
	public Context build(Context<Object> context) {
		try {
		    img = ImageIO.read(new File("C:/Users/andrfo/Documents/Git/CAS-City/ElectricGrid/maps/overlays/trondheim_el_nodes.png"));
		} catch (IOException e) {
			System.out.println(e + ": Image file not found!");
		}
		width = img.getWidth();
		height = img.getHeight();
		
		context.setId("ElectricGrid");
		
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("electric network", context, true);
		netBuilder.buildNetwork();
		
		
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
		
		List<Charger> chargers = new ArrayList<Charger>();
		List<Substation> substations = new ArrayList<Substation>();
		List<Building> buildings = new ArrayList<Building>();
		
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
				if(r == 255 && g == 255 && b == 255) {//Nothing
					continue;
				}
				else if(r == 181 && g == 230 && b == 29) {//Road, direction North-East
					Charger charger = new Charger(space, grid);
					context.add(charger);
					space.moveTo(charger, x, y);
					grid.moveTo(charger, x, y);
					chargers.add(charger);
					
				}
				else if(r == 128 && g == 64 && b == 0) {//Building
					//TODO: make buildings be more than one pixel
					Building building = new Building(space, grid);
					context.add(building);
					space.moveTo(building, x, y);
					grid.moveTo(building, x, y);
					buildings.add(building);
				}
				else {
//					System.out.println("r: " + r + " g: " + g + " b: " + b);
				}
				
			}
		}
		List<GridPoint> data = new ArrayList<GridPoint>();
		for(Building b: buildings) {
			data.add(grid.getLocation(b));
		}
		for(Charger s: chargers) {
			data.add(grid.getLocation(s));
		}
		Clustering c = new Clustering(data, 0, 0, width, height, 5);
		for(GridPoint p: c.kMeans()) {
			Substation substation = new Substation(space, grid);
			context.add(substation);
			space.moveTo(substation, p.getX(), p.getY());
			grid.moveTo(substation, p.getX(), p.getY());
			substations.add(substation);
		}
		
		buildGraph(grid, context, c.getClusters());
	}
	private Network<Object> buildGraph(Grid<Object> grid, Context<Object> context, ArrayList<ArrayList<GridPoint>> clusters) {
		//Get network
		Network<Object> net = (Network<Object>)context.getProjection("electric network");
		
		List<Substation> ss = new ArrayList<Substation>();
		
		for(ArrayList<GridPoint> cluster: clusters) {
			GridPoint ps = cluster.remove(0);
			Substation s = (Substation) Tools.getObjectAt(grid, Substation.class, ps.getX(), ps.getY());
			ss.add(s);
			for(GridPoint p: cluster) {
				Entity e = null;
				for(Object o: grid.getObjectsAt(p.getX(), p.getY())) {
					if(!(o instanceof Substation) && o instanceof Entity) {
						e= (Entity) o;
					}
				}
				net.addEdge(s, e);
			}
		}
		int n = ss.size();
		int V = n;
		int E = (n*(n-1))/2;
		MST mst = new MST(V, E);
		for(int i = 0; i < n; i++) {
			for(int j = 0; j < n; j++) {
				if(i == j) {continue;	}
				mst.edge[i + j].src = i; 
				mst.edge[i + j].dest = j; 
				mst.edge[i + j].weight = 
						(int) Math.round(
								Tools.gridDistance(
										ss.get(i).getLocation(), 
										ss.get(j).getLocation()));
			}
		}
		Edge[] tree = mst.KruskalMST();
		for(Edge r: tree) {
			net.addEdge(ss.get(r.src), ss.get(r.dest));
		}
		
		return net;
	}

}
