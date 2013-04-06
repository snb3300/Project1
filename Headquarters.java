

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryEvent;
import edu.rit.ds.registry.RegistryEventFilter;
import edu.rit.ds.registry.RegistryEventListener;
import edu.rit.ds.registry.RegistryProxy;

/**
 * 
 * Class Headquartes represents a headquarter which would like listen on the
 * status of each and every packet flowing through the system. It will log the
 * details of packet traversal of each and every packet.
 * 
 * To start the customer use the following command:
 * <p>
 * Usage: java Customer <host> <port> <name> <X> <Y>
 * <p>
 * 
 * where <host> - host name of the registry server <port> - port number to which
 * registry server is listening. <name> - name of the GPSOffice
 * 
 * @author Shridhar Bhalekar
 * 
 */
public class Headquarters {

	/**
	 * Hostname of the machine running the Registry Server
	 */
	private String hostName;

	/**
	 * Port number on which the registry server is listening
	 */
	private int portNumber;

	/**
	 * Proxy for the RIT Computer Science Registry Server.
	 */
	private RegistryProxy registry;

	/**
	 * Event listener on the Registry Server
	 */
	private RegistryEventListener registryListener;

	/**
	 * Filter to filter events on reported by event listener
	 */
	private RegistryEventFilter registryFilter;

	/**
	 * Event listener to get the status updates from the GPSOffice
	 */
	private RemoteEventListener<PacketEvent> remoteListener;

	/**
	 * Constructs a new Headquarter object
	 * 
	 * Command line arguments:
	 * args[0] - registry server host name
	 * args[1] - registry server port number
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
	public Headquarters(String[] args) throws IOException {
		if (args.length != 2) {
			throw new IllegalArgumentException(
					"Usage : java Headquarters <host> <port>");
		} else {
			this.hostName = args[0];
			try {
				this.portNumber = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
						"Invalid argument for port number");
			}
		}

	}

	/**
	 * Initializes the registry settings required to execute method on remote 
	 * objects so as to add listener
	 * 
	 * @throws RemoteException
	 * 				Thrown if any remote exception is encountered
	 */
	public void initialize() throws RemoteException {
		try {
		registry = new RegistryProxy(hostName, portNumber);
		} catch(RemoteException e) {
			throw new IllegalArgumentException("Invalid hostname or port");
		}
		registryListener = new RegistryEventListener() {

			@Override
			public void report(long theSequenceNumber, RegistryEvent theEvent)
					throws RemoteException {
				final String name = new String(theEvent.objectName());
				addSingleListener(name);
			}
		};
		UnicastRemoteObject.exportObject(registryListener, 0);

		remoteListener = new RemoteEventListener<PacketEvent>() {
			@Override
			public void report(long theSequenceNumber, PacketEvent theEvent)
					throws RemoteException {
				System.out.println(theEvent.getMessage());
			}
		};
		UnicastRemoteObject.exportObject(remoteListener, 0);
		registryFilter = new RegistryEventFilter().reportType("GPSOfficeRef")
				.reportBound();
		registry.addEventListener(registryListener, registryFilter);
		add();

	}

	/**
	 * Adds the remote event listener to the remote GPSOffice object
	 * @throws RemoteException
	 */
	private void add() throws RemoteException {

		for (String name : registry.list("GPSOfficeRef")) {
			addSingleListener(name);
		}
	}

	/**
	 * Adds listnener to object specified by the object name
	 * @param objectName object to add listener to
	 */
	private void addSingleListener(String objectName) {
		try {
			GPSOfficeRef office = (GPSOfficeRef) registry.lookup(objectName);
			office.addListener(remoteListener);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {

			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			Headquarters headquarter = new Headquarters(args);
			headquarter.initialize();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
