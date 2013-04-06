

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.rit.ds.Lease;
import edu.rit.ds.RemoteEventGenerator;
import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.AlreadyBoundException;
import edu.rit.ds.registry.RegistryEvent;
import edu.rit.ds.registry.RegistryEventFilter;
import edu.rit.ds.registry.RegistryEventListener;
import edu.rit.ds.registry.RegistryProxy;

/**
 * Class GPSOffice represents a distributed GPSOffice in a Geographic Package
 * System. Each GPSOffice will have maximum of three neighbors and it's 
 * responsibility is to deliver the incoming customer package. It will either 
 * forward packet directly to destination address or to one of it's neighbor.
 * The forwarding logic is if destination location is closer to current office
 * as compared to the distance of destination with the neighbors then the 
 * current office will deliver the packet directly to destination, otherwise it
 * will find out which one of it's current neighbors is closest to destination 
 * and will forward packet to that destination.    
 * 
 * <b>This class uses the RIT Computer Science Library<b>
 * 
 * To register a single GPSOffice object with the registry use the following 
 * command
 * <p>
 * Usage : java Start GPSOffice <host> <port> <name> <X> <Y>
 * <p>
 * where <host> - host name of the registry server
 * 		 <port> - port number to which registry server is listening.
 * 		 <name> - name of the GPSOffice
 * 		 <X>    - x coordinate of the current GPS Office
 * 		 <Y>    - y coordinate of the current GPS Office
 * 
 * @author Shridhar Bhalekar
 *
 */
public class GPSOffice implements GPSOfficeRef {
	
	/**
	 * Hostname or IP address of the machine running the Registry Server
	 */
	private String hostName;
	
	/**
	 * Port Number of machine running the Registry Server
	 */
	private int portNumber;
	
	/**
	 * Name of the GPSOffice which will be used to bind this object to Registry
	 * Server
	 */
	private String cityName;
	
	/**
	 * X coordinate of the current GPSOffice
	 */
	private double xValue;
	
	/**
	 * Y coordinate of the current GPSOffice
	 */
	private double yValue;
	
	/**
	 * Proxy for the RIT Computer Science Registry Server. 
	 */
	private RegistryProxy registryProxy;
	
	/**
	 * Event listener on the Registry Server 
	 */
	private RegistryEventListener registryEventListener;
	
	/**
	 * Filter to filter events on reported by event listener 
	 */
	private RegistryEventFilter registryEventFilter;
	
	/**
	 * Event generator on remote event listeners
	 */
	private RemoteEventGenerator<PacketEvent> remoteGenerator;
	
	/**
	 * List which will store maximum of three neighbors of the current GPSOffice
	 */
	private List<NeighborStorage> neighbors;
	
	/**
	 * Maximum allowed neighbors
	 */
	private static final int maxNeighbors = 3;
	
	/**
	 * Thread pool executor for concurrency
	 */
	private ExecutorService executor;

	/**
	 * Constructs a new GPSOffice object
	 * 
	 * Command line arguments:
	 * args[0] - registry server host name
	 * args[1] - registry server port number
	 * args[2] - GPSOffice name
	 * args[3] - GPSOffice X coordinate
	 * args[4] - GPSOffice Y coordinate
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
	public GPSOffice(String[] args) throws IOException {
		
		if (args.length != 5) {
			System.out
					.println("Usage: java Start GPSOffice <host> <port> <name> <X> <Y> ");
			throw new IllegalArgumentException("Invalid number of arguments");
		}
		hostName = args[0];
		cityName = args[2];
		portNumber = parseInt(args[1], "portNumber");
		xValue = parseDouble(args[3], "X co-ordinate");
		yValue = parseDouble(args[4], "Y co-ordinate");
		neighbors = new ArrayList<NeighborStorage>();
		
		// initializing the registry proxy
		try {
			registryProxy = new RegistryProxy(hostName, portNumber);
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot connact to " + hostName
					+ ":" + portNumber);
		}
		UnicastRemoteObject.exportObject(this, 0);
		
		// remote event generator for the headquarter
		remoteGenerator = new RemoteEventGenerator<PacketEvent>();
		try {
			registryProxy.bind(cityName, this);
		} catch (AlreadyBoundException abe) {
			try {
				UnicastRemoteObject.unexportObject(this, true);
			} catch (NoSuchObjectException nso1) {
			}
			throw new IllegalArgumentException("GPS Office with " + cityName
					+ " already bound to the registry");
		} catch (RemoteException re) {
			try {
				UnicastRemoteObject.unexportObject(this, true);
			} catch (NoSuchObjectException nso2) {
			}
			throw new IllegalArgumentException(
					"Cannot connect to registry server at " + hostName + ":"
							+ portNumber);
		} catch (Exception e) {
			try {
				UnicastRemoteObject.unexportObject(this, true);
			} catch (NoSuchObjectException nso2) {
			}
			e.printStackTrace();
		}

		updateNeighbors();
		registryEventListener = new RegistryEventListener() {
			@Override
			public void report(long arg0, RegistryEvent event)
					throws RemoteException {

				if (event.objectWasBound()) {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							updateNeighbors();
						}
					});
				}
			}
		};

		UnicastRemoteObject.exportObject(registryEventListener, 0);

		// Event filter on specific object type
		registryEventFilter = new RegistryEventFilter();
		registryEventFilter.reportType("GPSOfficeRef").reportBound();
		registryEventFilter.reportType("GPSOfficeRef").reportUnbound();
		registryProxy.addEventListener(registryEventListener,
				registryEventFilter);
		executor = Executors.newCachedThreadPool();
	}

	/**
	 * Take an object of GPSOffice and add it to the neighbor if it is closer
	 * than other neighbors of the current office.
	 * 
	 * @param office Object of type GPSOffice to be added
	 * 
	 * @throws RemoteException
	 * 			Thrown if remote method execution results in error
	 */
	private void addNewNeighbor(GPSOfficeRef office) throws RemoteException {
		synchronized (neighbors) {
			if (office != null) {
				double cDist = evaluateDistance(this, office);
				NeighborStorage neighbor = new NeighborStorage(office,
						office.getCity(), office.getXValue(),
						office.getYValue());
				// if less than 3 neighbors then add directly to list
				if (neighbors.size() < maxNeighbors) {
					neighbors.add(neighbor);
				} else {
					// calculate the distance of GPSOffice argument with the 
					// present neighbors and replace if near
					for (int i = 0; i < maxNeighbors; i++) {
						double nDist = evaluateDistance(office, neighbors
								.get(i).getOffice());
						if (cDist < nDist) {
							neighbors.set(i, neighbor);
							break;
						}
					}
				}
			}
		}

	}

	/**
	 * Get's the list of names of remote objects registered with the Registry 
	 * Server. For each object name it requests that object from the Registry 
	 * Server and try to add it to the neighbor list.
	 */
	private void updateNeighbors() {
		List<String> names = new ArrayList<String>();
		// list from Registry server
		try {
			names = registryProxy.list("GPSOfficeRef");
		} catch (Exception e1) {
			System.out.println("Error retrieving names of GPSOffice");
			e1.printStackTrace();
		}

		synchronized (neighbors) {
			// look up from the registry and try to add to neighbor list
			neighbors = new ArrayList<NeighborStorage>();
			for (String obj : names) {
				GPSOfficeRef office;
				try {
					office = (GPSOfficeRef) registryProxy.lookup(obj);
					if (office != null && !office.getCity().equals(cityName)) {
						addNewNeighbor(office);
					}

				} catch (Exception e) {
					continue;
				}
			}
		}
	}

	/**
	 * Evaluate Eucledian distance between to points represent by (x,y)
	 * 
	 * @param x1 X coordinate of first point
	 * @param x2 X coordinate of second point
	 * @param y1 Y coordinate of first point
	 * @param y2 Y coordinate of second point
	 * 
	 * @return
	 * 			Distance between the two points
	 */
	private double evaluateEucledian(double x1, double x2, double y1, double y2) {
		double distance = 0.0;
		double x = x2 - x1;
		double y = y2 - y1;
		distance = Math.sqrt((x * x) + (y * y));
		return distance;
	}

	/**
	 * Evaluate the distance between two GPSOffice objects.
	 * 
	 * @param office1 First GPSOffice Object
	 * @param office2 Second GPSOffice Object
	 * @return
	 * @throws RemoteException
	 */
	private double evaluateDistance(GPSOfficeRef office1, GPSOfficeRef office2)
			throws RemoteException {
		double distance = 0.0;
		if (office2 != null) {
			distance = evaluateEucledian(office1.getXValue(),
					office2.getXValue(), office1.getYValue(),
					office2.getYValue());
		}
		return distance;
	}

	/**
	 * Getter to get the X coordinate to current GPSOffice Object
	 */
	public double getXValue() {
		return this.xValue;
	}

	/**
	 * Getter to get the Y coordinate of current GPSOffice Object
	 */
	public double getYValue() {
		return this.yValue;
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
	private int parseInt(String arg, String name) {
		try {
			return Integer.parseInt(arg);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid argument for" + name
					+ ":" + arg);
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
	private double parseDouble(String arg, String name) {
		try {
			return Double.parseDouble(arg);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid argumet for " + name
					+ ":" + arg);
		}
	}

	/**
	 * Gets the closest GPSOffice to the destination location among the 
	 * neighbors. It will return null if current GPSOffice is the closest to 
	 * the destination location.
	 * 
	 * @param p Packet received by the GPSOffice
	 * 
	 * @return an GPSOffice object closest to destination location
	 */
	private final NeighborStorage getClosestOffice(Packet p) {
		// distance of destination with current GPSOffice
		double minDist = evaluateEucledian(this.xValue, p.getxValue(),
				this.yValue, p.getyValue());
		NeighborStorage result = null;

		int lim = Math.min(neighbors.size(), maxNeighbors);
		// check each neighbor distance with the desination location
		for (int i = 0; i < lim; i++) {
			double dist = evaluateEucledian(neighbors.get(i).getX(),
					p.getxValue(), neighbors.get(i).getY(), p.getyValue());
			if (dist < minDist) {
				result = neighbors.get(i);
				minDist = dist;
			}
		}
		return result;

	}

	/**
	 * Creates a new remote event to be generated by the remote event generator
	 *  
	 * @param type type of message to be generated
	 * @param p Packet received by current GPSOffice
	 * @param city name of the GPSOffice
	 * 
	 * @return an remote event
	 */
	private PacketEvent createNewPacketEvent(String type, Packet p, String city) {
		PacketEvent event = null;
		String message = "Package number " + p.getTrackingNumber();
		if (type.equals("arrived")) {
			message += " arrived at " + city + " office";
		} else if (type.equals("departed")) {
			message += " departed from " + city + " office";
		} else if (type.equals("lost")) {
			message += " lost by " + city + " office";
		} else if (type.equals("delivered")) {
			message += " delivered from " + city + " office to ("
					+ p.getxValue() + "," + p.getyValue() + ")";
		}
		event = new PacketEvent(message, p.getTrackingNumber());
		return event;
	}

	@Override
	/**
	 * Takes a customer packet analyze the packet, figure out the destination
	 * and finally forward that packet either to destination or to one of the
	 * neighbors.
	 * 
	 *  @param packet Packet received and to be forwarded
	 */
	public void packetForward(final Packet packet) {
		
		// event generator for the customer
		final RemoteEventGenerator<PacketEvent> remoteEventGenerator = 
			new RemoteEventGenerator<PacketEvent>();

		// add listener to the generator
		try {
			remoteEventGenerator.addListener(packet.getListener());
		} catch (RemoteException e1) {
			System.out.println("Failed to add listener for " + cityName);
			e1.printStackTrace();
		}
		
		// report the customer about the receipt of packet
		remoteEventGenerator.reportEvent(createNewPacketEvent("arrived",
				packet, cityName));
		// report the headquarte about the receipt of the packet
		remoteGenerator.reportEvent(createNewPacketEvent("arrived", packet,
				cityName));
		
		// time for processing
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
		
		final NeighborStorage office = getClosestOffice(packet);

		// if null the destination is closer than neighbors
		if (office == null) {
			remoteEventGenerator.reportEvent(createNewPacketEvent("delivered",
					packet, cityName));
			remoteGenerator.reportEvent(createNewPacketEvent("delivered",
					packet, cityName));
		} else {
			final GPSOfficeRef o = office.getOffice();
			// execute the forward in a new thread
			executor.execute(new Runnable() {

				@Override
				public void run() {
					String city = new String(cityName);
					try {
						city = o.getCity();
						o.packetForward(packet);

					} catch (Exception e) {
						remoteEventGenerator.reportEvent(createNewPacketEvent(
								"lost", packet, city));
						remoteGenerator.reportEvent(createNewPacketEvent(
								"lost", packet, city));
					}
				}
			});
			// report to customer about the forward 
			remoteEventGenerator.reportEvent(createNewPacketEvent("departed", packet, cityName));
			// report to headquarter about the forward
			remoteGenerator.reportEvent(createNewPacketEvent("departed", packet, cityName));
		}
	}

	/**
	 * Take the destination coordinates and remote listener
	 * 
	 * @param xVal X coordinate of the destination
	 * @param yVal Y coordinate of the destination
	 * @param remoteListener remote listener of the customer
	 */
	public void createSendPacket(double xVal, double yVal,
			RemoteEventListener<PacketEvent> remoteListener)
			throws RemoteException {
		Packet packet = new Packet(xVal, yVal, System.currentTimeMillis(),
				remoteListener);
		packetForward(packet);

	}

	@Override
	/**
	 * Getter to get the name of current GPSOffice 
	 */
	public String getCity() {
		return this.cityName;
	}

	@Override
	/**
	 * Add a remote event listener on the remote event generator
	 */
	public Lease addListener(RemoteEventListener<PacketEvent> listener)
			throws RemoteException {
		return remoteGenerator.addListener(listener);
	}
}