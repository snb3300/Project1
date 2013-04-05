

import edu.rit.ds.RemoteEvent;

public class CustomerEvent extends RemoteEvent{

	private String message;
	private long trackNumber;
	
	public CustomerEvent(String message, long trackNumber) {
		this.message = message;
		this.trackNumber = trackNumber;
	}
	
	public long getTrackNumber() {
		return this.trackNumber;
	}
	
	public String getMessage() {
		return this.message;
	}
}
