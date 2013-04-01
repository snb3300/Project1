

import edu.rit.ds.RemoteEvent;

public class GPSOfficeEvent extends RemoteEvent{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Packet packet;
	private GPSOfficeRef gpsOffice;
	
	public GPSOfficeEvent(Packet packet, GPSOfficeRef office) {
		this.gpsOffice = office;
		this.packet = packet;
	}
	
	public Packet getPacket() {
		return this.packet;
	}
	
	public GPSOfficeRef getGPSOffice() {
		return this.gpsOffice;
	}
	
//	public void setGPSOffice(GPSOfficeRef office) {
//		this.gpsOffice = office;
//	}
}
