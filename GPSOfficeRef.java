

import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.rit.ds.Lease;
import edu.rit.ds.RemoteEventListener;

public interface GPSOfficeRef extends Remote {

	public void packetForward(final Packet packet) throws RemoteException;

	public double getXValue() throws RemoteException;

	public double getYValue() throws RemoteException;

	public String getCity() throws RemoteException;

	public void sendPacket(double xValue, double yValue,
			RemoteEventListener<CustomerEvent> remoteListener)
			throws RemoteException;

	public Lease addListener(RemoteEventListener<GPSOfficeEvent> listener)
			throws RemoteException;
}
