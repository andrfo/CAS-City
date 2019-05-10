package utils;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Clustering {
	
	private List<GridPoint> data;
	private List<Centroid> centroids;
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;
	private int k;
	
	

	public Clustering(List<GridPoint> data, int minX, int minY, int maxX, int maxY, int k) {
		super();
		this.data = data;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.k = k;
		centroids = new ArrayList<Centroid>();
		generateRandomSeededCentroids();
	}

	static class Centroid {
		
		public GridPoint location;
		public List<GridPoint> cluster;
		public Centroid(GridPoint location) {
			this.location = location;
			cluster = new ArrayList<GridPoint>();
		}
		
	}

	public List<GridPoint> kMeans() {
	    boolean change = true;
	    int runs = 0;
	    while(change && runs < 100) {
	        for(Centroid c: centroids) {
	        	c.cluster = new ArrayList<GridPoint>();
	        }
	    	
	        for(GridPoint d: data) {
	            Centroid closestCentroid = null;
	            Double minDistance = Double.MAX_VALUE;
	            for (Centroid c: centroids) {
//		                #Distance between two points
	                Double distance = Tools.gridDistance(c.location, d);
	                if (distance < minDistance) {
	                    closestCentroid = c;
	                    minDistance = distance;
	                }
	            }
	            closestCentroid.cluster.add(d);
	        }

//		        #Edit centroids to be the mean of their points
	        for (Centroid c: centroids) {
	            if(c.cluster.size() == 0) {
	                continue;
	            }
	            Float sumX = 0f;
	            Float sumY = 0f;
	            for (GridPoint p: c.cluster){
	            	sumX += p.getX();
        			sumY += p.getY();
	            }
	            
	            Float x = sumX/c.cluster.size();
	            Float y = sumY/c.cluster.size();
	            
//		            #Check convergence
	            change = false;
	            if(x != c.location.getX() || y != c.location.getY()) {
	                change = true;
	            }
	            c.location = new GridPoint(Math.round(x), Math.round(y));
	        }
	        runs++;
	    }
	    List<GridPoint> points = new ArrayList<GridPoint>();
	    for(Centroid c: centroids) {
	    	points.add(c.location);
	    }
//	    System.out.println(runs);
	    return points;
	}
	
	/***
	 * Generates a list of lists where each inner list is a cluster and the first element is the centroid
	 * @return
	 */
	public ArrayList<ArrayList<GridPoint>> getClusters(){
		ArrayList<ArrayList<GridPoint>> clusters = new ArrayList<ArrayList<GridPoint>>();
		for(Centroid c: centroids) {
			ArrayList<GridPoint> cluster = new ArrayList<GridPoint>();
			cluster.add(c.location);
			for(GridPoint p: c.cluster) {
				cluster.add(p);
			}
			clusters.add(cluster);
		}
		return clusters;
	}
	
	private void generateRandomCentroids(){
		for(int i = 0; i < k; i++) {
			int x = RandomHelper.nextIntFromTo(minX,  maxX);
			int y = RandomHelper.nextIntFromTo(minY,  maxY);
			centroids.add(new Centroid(new GridPoint(x, y)));
		}
	}
	
	private void generateRandomSeededCentroids() {
		for(int i = 0; i < k; i++) {
			GridPoint p = data.get(RandomHelper.nextIntFromTo(0,  data.size() - 1));
			centroids.add(new Centroid(p));
		}
	}
	
}
