package citySim;

public class Reporter {

	
	//General
	//===============================================================
	private int nGeneralTime = 0; //Number of addages, may be better to use a list and use .size() later
	private int nGeneralCost = 0;
	private int nGeneralDistance = 0;
	private Double averageTravelTime = 0d;
	private Double averageTravelCost = 0d;
	private Double averageTravelDistance = 0d; //Just cars for now
	
	//Bus
	//===============================================================
	private int nBusTime = 0; //Number of addages, may be better to use a list and use .size() later
	private int nBusCost = 0;
	private int nBusDistance = 0;
	private Double averageBusCost = 0d;
	private Double averageBusTravelTime = 0d;
	private Double averageBusTravelDistance = 0d; //From bus stop to destination("walking")
	
	
	//Car
	//===============================================================
	private int nCarTime = 0; //Number of addages, may be better to use a list and use .size() later
	private int nCarCost = 0;
	private int nCarDistance = 0;
	private Double AverageCarCost = 0d;
	private Double AverageCarTravelTime = 0d;
	private Double averageCarTravelDistance = 0d;
	
	
	//===============================================================
	//Getters
	//===============================================================
	public Double getAverageTravelTime() {
		return averageTravelTime;
	}
	public Double getAverageTravelCost() {
		return averageTravelCost;
	}
	public Double getAverageTravelDistance() {
		return averageTravelDistance;
	}
	public Double getAverageBusCost() {
		return averageBusCost;
	}
	public Double getAverageBusTravelTime() {
		return averageBusTravelTime;
	}
	public Double getAverageBusTravelDistance() {
		return averageBusTravelDistance;
	}
	public Double getAverageCarCost() {
		return AverageCarCost;
	}
	public Double getAverageCarTravelTime() {
		return AverageCarTravelTime;
	}
	public Double getAverageCarTravelDistance() {
		return averageCarTravelDistance;
	}
	
	
	
	//===============================================================
	//Adders
	//===============================================================
	
	//General
	//===============================================================
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageTravelTime(Double val) {
		nGeneralTime++;
		Double oldVal = Double.valueOf(averageTravelTime);
		Double newVal = oldVal + (val - oldVal) / nGeneralTime;
		
		averageTravelTime = newVal;
		
	}
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageTravelCost(Double val) {
		nGeneralCost++;
		Double oldVal = Double.valueOf(averageTravelCost);
		Double newVal = oldVal + (val - oldVal) / nGeneralCost;
		
		averageTravelCost = newVal;
	}
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageTravelDistance(Double val) {
		nGeneralDistance++;
		Double oldVal = Double.valueOf(averageTravelDistance);
		Double newVal = oldVal + (val - oldVal) / nGeneralDistance;
		
		averageTravelDistance = newVal;
	}
	
	
	//Bus
	//===============================================================	
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageBusCost(Double val) {
		addToAverageTravelCost(val);
		nBusCost++;
		Double oldVal = Double.valueOf(averageBusCost);
		Double newVal = oldVal + (val - oldVal) / nBusCost;
		
		averageBusCost = newVal;
		
	}
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageBusTravelTime(Double val) {
		addToAverageTravelTime(val);
		nBusTime++;
		Double oldVal = Double.valueOf(averageBusTravelTime);
		Double newVal = oldVal + (val - oldVal) / nBusTime;
		
		averageBusTravelTime = newVal;
	}
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageBusTravelDistance(Double val) {
		addToAverageTravelDistance(val);
		nBusDistance++;
		Double oldVal = Double.valueOf(averageBusTravelDistance);
		Double newVal = oldVal + (val - oldVal) / nBusDistance;
		
		averageBusTravelDistance = newVal;
	}
	
	
	//Car
	//===============================================================
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageCarCost(Double val) {
		addToAverageTravelCost(val);
		nCarCost++;
		Double oldVal = Double.valueOf(AverageCarCost);
		Double newVal = oldVal + (val - oldVal) / nCarCost;
		
		AverageCarCost = newVal;
	}
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageCarTravelTime(Double val) {
		addToAverageTravelTime(val);
		nCarTime++;
		Double oldVal = Double.valueOf(AverageCarTravelTime);
		Double newVal = oldVal + (val - oldVal) / nCarTime;
		
		AverageCarTravelTime = newVal;
	}
	/**
	 * @param val the value to add to the average
	 */
	public void addToAverageCarTravelDistance(Double val) {
		addToAverageTravelDistance(val);
		nCarDistance++;
		Double oldVal = Double.valueOf(averageCarTravelDistance);
		Double newVal = oldVal + (val - oldVal) / nCarDistance;
		
		averageCarTravelDistance = newVal;
	}
	
	
	
	
	
	
	
	
	
	

}
