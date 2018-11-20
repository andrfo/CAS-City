package utils;

public class Vector2D {

	public double x;
	public double y;
	public Vector2D(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	public double magnitude() {
		return Math.sqrt(x*x + y*y);
	}
	
	public double dot(Vector2D other) {
		return this.x*other.x + this.y*other.y;
	}
	
	public double angle(Vector2D other) {
		
		double cosTheta = (dot(other))/(magnitude()*other.magnitude());
		return Math.acos(cosTheta);
		
	}
	
	public double signArea(Vector2D other) {
		return (other.x  - x)*(other.y - y);
	}
	
}
