package electricGrid;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import environment.Substation;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

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
		
		List<Substation> spawnPoints = new ArrayList<Substation>();
		
		
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
					System.out.println("station");
					Substation substation = new Substation(space, grid);
					context.add(substation);
					space.moveTo(substation, x, y);
					grid.moveTo(substation, x, y);
					
				}
				else {
//					System.out.println("r: " + r + " g: " + g + " b: " + b);
				}
				
			}
		}
	}
	

}
