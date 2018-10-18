package citySim;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.StrictBorders;

/**
 * @author andrfo
 *
 */
public class CitySimBuilder implements ContextBuilder<Object> {

	int width;
	int height;
	
	@Override
	public Context build(Context<Object> context) {
		context.setId("citysim");
		
		
		
		ContinuousSpaceFactory spaceFactory = 
				ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", 
				context, 
				new SimpleCartesianAdder<Object>(), //TODO: Change adder?
				new repast.simphony.space.continuous.StrictBorders(), 
				50, 50);
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid(
				"grid", 
				context, 
				new GridBuilderParameters<Object>(
						new StrictBorders(), 
						new SimpleGridAdder<Object>(),//TODO: Change adder?
						true,
						50,
						50));
		readImage(space, grid);
		
		//TODO: add Entities
		
		//TODO: add Agents
		
		return context;
	}
	
	private void readImage(ContinuousSpace<Object> space, Grid grid) {
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File("maps/3-2.png"));
		} catch (IOException e) {
			System.out.println(e + ": Image file not found!");
		}
		width = img.getWidth();
		height = img.getHeight();
		
		//get pixel value
	    int p = img.getRGB(0,0);

	    //get alpha
	    int a = (p>>24) & 0xff;

	    //get red
	    int r = (p>>16) & 0xff;

	    //get green
	    int g = (p>>8) & 0xff;

	    //get blue
	    int b = p & 0xff;
	}

}
