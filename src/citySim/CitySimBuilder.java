package citySim;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import citySim.environment.Road;
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
	
	//black = pixel.r == 0 && pixel.g == 0 && pixel.b == 0;
    // white = pixel.r > 0.5 && pixel.g > 0.5 && pixel.b > 0.5;
    // green = pixel.r < 0.5 && pixel.g > 0.5 && pixel.b < 0.5;
    // red = pixel.r > 0.5 && pixel.g < 0.5 && pixel.b < 0.5;
	
	private void readImage(ContinuousSpace<Object> space, Grid grid, Context<Object> context) {
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File("maps/3-2.png"));
		} catch (IOException e) {
			System.out.println(e + ": Image file not found!");
		}
		width = img.getWidth();
		height = img.getHeight();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
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
				
				
				if(r >= 255 && g >= 255 && b >= 255) {//Nothing
					continue;
				}
				else if(r == 0 && g == 0 && b == 0) {//Road
					context.add(new Road(location, space, grid))
				}
				else if(r <= 100 && g >= 100 && b <= 100) {//Start
					
				}
				else if(r >= 100 && g <= 100 && b <= 100) {//end
					
				}
				
			}
		}
	}

}
