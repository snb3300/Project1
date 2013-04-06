


import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.rit.ds.Lease;
import edu.rit.ds.RemoteEventListener;
/**
 * Remote interface for the RMI to specify that GPSOffice is a distributed 
 * object in the Geographic Package System
 */
public interface GPSOfficeRef extends Remote {

	/**
	 * Forward packet to this Office which will analyze the packet, get the 
	 * next recepient and finally forward the received packet to the neighbor
	 * or destination
	 * 
	 * @param packet Packet to be analyses and forwarded
	 * 
	 * @throws RemoteException
	 * 				Thrown if remote error encountered
	 */
	public void packetForward(final Packet packet) throws RemoteException;

	/**
	 * Returns the X coordinate of current GPS Office
	 * 
	 * @return X coordinate
	 * 
	 * @throws RemoteException
	 * 				Thrown if remote error encountered
	 */
	public double getXValue() throws RemoteException;

	/**
	 * Returns the Y coordinate of current GPS Office
	 * 
	 * @return Y coordinate
	 * 
	 * @throws RemoteException
	 * 				Thrown if remote error encountered
	 */
	public double getYValue() throws RemoteException;

	/**
	 * Returns the name of the current GPS Office
	 * 
	 * @return name of office
	 * 
	 * @throws RemoteException
	 * 				Thrown if remote error encountered
	 */
	public String getCity() throws RemoteException;

	/**
	 * Get the destination location and create a new Packet to be forwarded
	 * 
	 * @param xValue X value of the destination
	 * 
	 * @param yValue Y value of the destination
	 * 
	 * @param remoteListener remote event listener of the customer
	 * 
	 * @throws RemoteException
	 * 				Thrown if remote error encountered
	 */
	public void createSendPacket(double xValue, double yValue,
			RemoteEventListener<PacketEvent> remoteListener)
			throws RemoteException;

	/**
	 * Adds a remote listener to the remote event generator of the current Office
	 * 
	 * @param listener remote event listener
	 * 
	 * @throws RemoteException
	 * 				Thrown if remote error encountered
	 */
	public Lease addListener(RemoteEventListener<PacketEvent> listener)
			throws RemoteException;
}
