package structures;

public class Trip {
	
	public Double cost;
	public String choice;
	public Trip(Double cost, String choice) {
		super();
		this.cost = cost;
		this.choice = choice;
	}
	public Double getCost() {
		return cost;
	}
	public String getChoice() {
		return choice;
	}
}
