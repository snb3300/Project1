

/**
 * Class NeighborStorage represents a single storage storing the GPSOffice and 
 * its attributes. These objects are used in the GPSOffice to store neighbors.
 * 
 * 
 * @author Shridhar Bhalekar
 *
 */
public class NeighborStorage {

	/**
	 * GPSOffice reference which is a neighbor.
	 */
	private GPSOfficeRef neighbor;
	/**
	 * X coordinate of the stored GPSOffice
	 */
	private double xValue;
	/**
	 * Y coordinate of the stored GPSOffice
	 */
	private double yValue;
	
	/**
	 * Name of stored GPSOffice
	 */
	private String city;


	/**
	 * Creates a new NeighborStorage
	 * @param office neighbor office
	 * @param city name of neighbor office
	 * @param xValue x coordinate of neighbor office
	 * @param yValue y coordinate of neighbor office
	 */
	public NeighborStorage(GPSOfficeRef office, String city, double xValue,
			double yValue) {
		this.neighbor = office;
		this.city = city;
		this.xValue = xValue;
		this.yValue = yValue;

	}

	/**
	 * Getter which returns the neighbor reference
	 * @return
	 */
	public GPSOfficeRef getOffice() {
		return this.neighbor;
	}
	
	/**
	 * Getter which returns the neighbor name
	 * @return
	 */
	public String getCity() {
		return this.city;
	}
	
	/**
	 * Getter which returns the X coordinate
	 * @return
	 */
	public double getX() {
		return this.xValue;
	}
	
	/**
	 * Getter which returns the Y coordinate of the neighbor
	 * @return
	 */
	public double getY() {
		return this.yValue;
	}
}
