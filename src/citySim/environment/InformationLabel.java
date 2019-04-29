package citySim.environment;

import java.math.BigDecimal;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import utils.Tools;

public class InformationLabel extends Entity{
	
	private int time;
	private String label = "";

	public InformationLabel(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context) {
		super(space, grid);
		// TODO Auto-generated constructor stub
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		time = Tools.getTime();
		
		label = timeToString();
		
	}

	private String timeToString() {
		
		
		BigDecimal[] valRem = BigDecimal.valueOf(time).divideAndRemainder(new BigDecimal(360));
		
		int hours = valRem[0].intValue();
		int minutes = (int) (valRem[1].doubleValue()/6);
		
		return hours + ":" + minutes;
	}

	public String label() {
		return label;
	}
}
