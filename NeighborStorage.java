

public class NeighborStorage implements Comparable<NeighborStorage> {

	private GPSOfficeRef neighbor;
	private double xValue;
	private double yValue;
	private String city;
	private double distance;

	public NeighborStorage(GPSOfficeRef office, String city, double xValue,
			double yValue, double distance) {
		this.neighbor = office;
		this.city = city;
		this.xValue = xValue;
		this.yValue = yValue;
		this.distance = distance;
	}

	public GPSOfficeRef getOffice() {
		return this.neighbor;
	}
	
	public String getCity() {
		return this.city;
	}
	
	public double getX() {
		return this.xValue;
	}
	
	public double getY() {
		return this.yValue;
	}
	
	public double getDistance() {
		return this.distance;
	}
	
		
	@Override
	public int compareTo(NeighborStorage o) {
		if(this.distance < o.distance)
			return -1;
		else if(this.distance > o.distance)
			return 1;
		return 0;
	}
}
