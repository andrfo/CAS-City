package structures;

import java.util.ArrayList;
import java.util.List;

import citySim.environment.Entity;

public class Goals {
	
	private List<Entity> goals;
	private Entity endGoal;

	public Goals() {
		super();
		this.goals = new ArrayList<Entity>();
	}

	public Entity getEndGoal() {
		return endGoal;
	}

	public void setEndGoal(Entity endGoal) {
		this.endGoal = endGoal;
	}

	public Entity getCurrent() {
		if(goals.size() <= 0) {
			goToEnd();
		}
		return goals.get(0);
	}

	public void replaceCurrent(Entity newGoal) {
		if(goals.size() > 0) {
			goals.remove(0);
			goals.add(0, newGoal);
		}
		else {
			goals.add(newGoal);
		}
	}
	
	public void addGoal(Entity goal) {
		goals.add(goal);
	}
	
	public void goToEnd() {
		if(endGoal != null) {
			goals.clear();
			goals.add(endGoal);			
		}
		else {
			System.out.println("No end goal");
		}
	}
	
	public void next() {
		if(goals.size() > 0) {
			goals.remove(0);			
		}
		else {
			goToEnd();
		}
	}
	

}
