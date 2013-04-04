

import edu.rit.ds.RemoteEvent;

public class GPSOfficeEvent extends RemoteEvent{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String message;
	
	public GPSOfficeEvent(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
//	public void setGPSOffice(GPSOfficeRef office) {
//		this.gpsOffice = office;
//	}
}
