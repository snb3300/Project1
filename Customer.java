

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryProxy;

/**
 * 
 * Class Customer represents a customer which would like to send a packet to
 * desired destination. It will approach one of the present GPSOffice in the 
 * system and provide the destination location to the selected GPSOffice.
 * 
 * To start the customer use the following command:
 * <p>
 * Usage: java Customer <host> <port> <name> <X> <Y>
 * <p>
 * 
 where <host> - host name of the registry server
 * 		 <port> - port number to which registry server is listening.
 * 		 <name> - name of the GPSOffice
 * 		 <X>    - x coordinate of the destination
 * 		 <Y>    - y coordinate of the destination
 *  
 * @author Shridhar Bhalekar
 *
 */
public class Customer {

	/**
	 * Hostname of the machine running the Registry Server
	 */
	private String hostName;
	
	/**
	 * Port number on which the registry server is listening
	 */
	private int portNumber;
	
	/**
	 * Originating GPS Office
	 */
	private String cityName;
	
	/**
	 * X coordinate of the destination
	 */
	private double xValue;
	
	/**
	 * Y coordinate of the destination
	 */
	private double yValue;
	
	/**
	 * Proxy to connect to the registry server
	 */
	private RegistryProxy registryProxy;
	
	/**
	 * Tracking number of the Packet created by the Originating GPS Office
	 */
	private long trackNumber;
	
	/**
	 * Event listener to get the status updates from the GPSOffice 
	 */
	private RemoteEventListener<PacketEvent> remoteListner;

	/**
	 * Constructs a new Customer object
	 * 
	 * Command line arguments:
	 * args[0] - registry server host name
	 * args[1] - registry server port number
	 * args[2] - GPSOffice name
	 * args[3] - Destination X coordinate
	 * args[4] - Destination Y coordinate
	 * 
	 * @param args Command Line arguments
	 * 
	 * @exception IllegalArgumentException
	 * 			Thrown if command line arguments are not according to 
	 *  		the requirements.
	 *  
	 *  @exception IOException
	 *  		Thrown if any type of remote exception is thrown 
	 */
	public Customer(String[] args) {

		if (args.length != 5) {
			System.out
					.println("Usage : java Customer <host> <port> <name> <X> <Y>");
			throw new IllegalArgumentException("Invalid number of arguments");
		} else {
			this.hostName = args[0];
			this.cityName = args[2];
			this.portNumber = parseI(args[1], "port");
			this.xValue = parseD(args[3], "xValue");
			this.yValue = parseD(args[4], "yValue");
			trackNumber = -1;
		}

	}

	/**
	 * Parses a string to Integer
	 * 
	 * @param arg value to be parsed as integer
	 * @param name name representing the value
	 * 
	 * @return integer after successful parse
	 * 
	 *  @exception IllegalArgumentException
	 *  				Thrown if parsing results in exception
	 */
	private int parseI(String value, String name) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid argument passed for "
					+ name);
		}
	}

	/**
	 * Parses a string to Double
	 * 
	 * @param arg value to be parsed as double
	 * @param name name representing the value
	 * 
	 * @return double after successful parse
	 * 
	 *  @exception IllegalArgumentException
	 *  				Thrown if parsing results in exception
	 */
	private double parseD(String value, String name) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid argument passed for "
					+ name);
		}
	}

	/**
	 * Get the intended remote object if present in the registry
	 * @return Originating GPSOffice object
	 * @throws IOException
	 * 				Thrown if any remote exception encountered
	 */
	private GPSOfficeRef getObject() throws IOException {
		try {
			registryProxy = new RegistryProxy(hostName, portNumber);
			return (GPSOfficeRef) registryProxy.lookup(cityName);
		} catch (RemoteException e) {
			throw new IllegalArgumentException("No registry server running at "
					+ hostName + " " + portNumber);
		} catch (NotBoundException e) {
			throw new IllegalArgumentException("Object with name " + cityName
					+ " not bound in registry");
		}
	}

	/**
	 * Initialize a remote event listener to get status updates from the offices
	 * 
	 * @return remote event listener
	 * @throws RemoteException
	 * 				Thrown if error encountered in initialization of remote
	 * 				listener
	 */
	public RemoteEventListener<PacketEvent> createListener() throws RemoteException {
		remoteListner = new RemoteEventListener<PacketEvent>() {

			@Override
			public void report(long theSequenceNumber, PacketEvent theEvent)
					throws RemoteException {
				String message = theEvent.getMessage();
				trackNumber = theEvent.getTrackNumber();
				System.out.println(message);
				if (message.contains("delivered") || message.contains("lost")) {
					System.exit(0);
				}
			}
		};

		UnicastRemoteObject.exportObject(remoteListner, 0);

		return remoteListner;
	}

	public static void main(String[] args) {
		Customer c = null;
		try {
			c = new Customer(args);
			GPSOfficeRef office = c.getObject();
			office.createSendPacket(c.xValue, c.yValue, c.createListener());
		} catch (Exception e) {
			// report packet loss if originating Office fails
			if (c != null && c.trackNumber != -1) {
				String msg = "Package number " + c.trackNumber + " lost by "
						+ c.cityName + " office";
				System.out.println(msg);
				System.exit(0);
			} else {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
