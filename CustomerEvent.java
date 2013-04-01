

import edu.rit.ds.RemoteEvent;

public class CustomerEvent extends RemoteEvent{

	private String message;
	
	public CustomerEvent(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
}
