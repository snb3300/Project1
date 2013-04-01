

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GPSOfficeRef extends Remote{

	public void packetForward(Packet packet) throws RemoteException;

	public double getXValue() throws RemoteException;
	public double getYValue() throws RemoteException;
	public String getCity() throws RemoteException;
}
