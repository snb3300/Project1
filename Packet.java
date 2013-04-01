

import java.io.Serializable;

import edu.rit.ds.RemoteEventListener;

public class Packet implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long trackingNumber;
	private double xValue;
	private double yValue;
	private RemoteEventListener<CustomerEvent> remoteEventListener;

	public Packet(double xValue, double yValue, long trackingNumber,
			RemoteEventListener<CustomerEvent> remoteListener) {
		this.xValue = xValue;
		this.yValue = yValue;
		this.trackingNumber = trackingNumber;
		this.remoteEventListener = remoteListener;
	}

	public long getTrackingNumber() {
		return trackingNumber;
	}

	public double getxValue() {
		return xValue;
	}

	public double getyValue() {
		return yValue;
	}

	public RemoteEventListener<CustomerEvent> getListener() {
		return this.remoteEventListener;
	}
}
