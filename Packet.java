

import java.io.Serializable;

import edu.rit.ds.RemoteEventListener;

/**
 * Class Packet represents a single packet to be routed in the Geographic Package
 * System. When customer approaches a GPSOffice with the destination location 
 * the GPSOffice creates a Packet Object and this object is routed across the 
 * system.
 * 
 * @author Shridhar Bhalekar
 *
 */
public class Packet implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Tracking number of the current packet
	 */
	private long trackingNumber;
	
	/**
	 * X coordinate of the destination
	 */
	private double xValue;
	
	/**
	 * Y coordinate of the destination
	 */
	private double yValue;
	
	/**
	 * Remote event listener of the Customer trying to send the packet
	 */
	private RemoteEventListener<PacketEvent> remoteEventListener;

	/**
	 * Creates a Packet Object
	 * 
	 * @param xValue X coordinate of the destination
	 * @param yValue Y coordinate of the destination
	 * @param trackingNumber tracking number of the packet
	 * @param remoteListener customer event listener
	 */
	public Packet(double xValue, double yValue, long trackingNumber,
			RemoteEventListener<PacketEvent> remoteListener) {
		this.xValue = xValue;
		this.yValue = yValue;
		this.trackingNumber = trackingNumber;
		this.remoteEventListener = remoteListener;
	}

	/**
	 * Getter which returns the tracking number of current packet
	 * @return tracking number
	 */
	public long getTrackingNumber() {
		return trackingNumber;
	}

	/**
	 * Getter which returns the X coordinate of the destination
	 * @return X coordinate
	 */
	public double getxValue() {
		return xValue;
	}

	/**
	 * Getter which returns the Y coordinate of the destination
	 * @return Y coordinate
	 */
	public double getyValue() {
		return yValue;
	}

	/**
	 * Getter which returns the remote event listener of customer
	 * @return remote eent listener
	 */
	public RemoteEventListener<PacketEvent> getListener() {
		return this.remoteEventListener;
	}
}
