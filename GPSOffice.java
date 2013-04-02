

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import edu.rit.ds.RemoteEventGenerator;
import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.AlreadyBoundException;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryEvent;
import edu.rit.ds.registry.RegistryEventFilter;
import edu.rit.ds.registry.RegistryEventListener;
import edu.rit.ds.registry.RegistryProxy;

public class GPSOffice implements GPSOfficeRef {

	private String hostName;
	private int portNumber;
	private String cityName;
	private double xValue;
	private double yValue;
	private RegistryProxy registryProxy;
	private RegistryEventListener registryEventListener;
	private RegistryEventFilter registryEventFilter;
	private List<GPSOfficeRef> neighbors;
	private int totalOfficeObjects;
	private static final int maxNeighbors = 3;
	private ExecutorService executor = Executors.newCachedThreadPool();

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
		neighbors = new ArrayList<GPSOfficeRef>(maxNeighbors);
		Collections.synchronizedList(neighbors);
		registryProxy = new RegistryProxy(hostName, portNumber);
		UnicastRemoteObject.exportObject(this, 0);

		try {
			registryProxy.bind(cityName, this);
		} catch (AlreadyBoundException abe) {
			try {
				UnicastRemoteObject.unexportObject(this, true);
			} catch (NoSuchObjectException nso1) {
			}
			throw new IllegalArgumentException("GPS Office with " + cityName
					+ " already bounded to the registry");
		} catch (RemoteException re) {
			try {
				UnicastRemoteObject.unexportObject(this, true);
			} catch (NoSuchObjectException nso2) {
			}
			throw re;
		}

		totalOfficeObjects = 0;
		updateNeighbors();
		registryEventListener = new RegistryEventListener() {
			@Override
			public void report(long arg0, RegistryEvent event)
					throws RemoteException {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						updateNeighbors();
					}
				});
			}
		};

		UnicastRemoteObject.exportObject(registryEventListener, 0);

		registryEventFilter = new RegistryEventFilter();
		registryEventFilter.reportType("GPSOffice").reportBound();
		registryEventFilter.reportType("GPSOffice").reportUnbound();
		registryProxy.addEventListener(registryEventListener,
				registryEventFilter);
	}

	public void updateNeighbors() {

		List<GPSOfficeRef> list = new ArrayList<GPSOfficeRef>();
		list = getAllNeighbors();

		if (list.size() < totalOfficeObjects) {
			checkForDelete(list);
		}

		totalOfficeObjects = list.size();
		getNearestNeighbors(list);
		for (GPSOfficeRef office : neighbors) {
			try {
				System.out.print(office.getCity() + " ");
			} catch (RemoteException e) {
				System.err.println(e.getMessage());
			}
		}
		System.out.println();

	}

	private void checkForDelete(List<GPSOfficeRef> list) {
		for (GPSOfficeRef neighbor : neighbors) {
			if (!list.contains(neighbor))
				neighbors.remove(neighbor);
		}
	}

	private double calculateEucledian(double x1, double x2, double y1, double y2) {
		double distance = 0.0;
		double x = x2 - x1;
		double y = y2 - y1;
		distance = Math.sqrt((x * x) + (y * y));
		return distance;
	}

	private double getDistance(GPSOfficeRef office1, GPSOfficeRef office2)
			throws IOException {
		double distance = 0.0;
		if (office2 != null) {
			distance = calculateEucledian(office1.getXValue(),
					office2.getXValue(), office1.getYValue(),
					office2.getYValue());
		}
		return distance;
	}

	private void getNearestNeighbors(List<GPSOfficeRef> allNeighbors) {
		for (GPSOfficeRef office : allNeighbors) {
			try {
				if ((office != null) && (!neighbors.contains(office))) {

					if (neighbors.size() < maxNeighbors)
						neighbors.add(office);
					else {
						for (GPSOfficeRef neighbor : neighbors) {
							double l1 = getDistance(this, neighbor);
							double l2 = getDistance(this, office);
							if (l1 > l2) {
								neighbors.remove(neighbor);
								neighbors.add(office);
								break;
							}
						}
					}
				}
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private List<GPSOfficeRef> getAllNeighbors() {
		List<GPSOfficeRef> l = new ArrayList<GPSOfficeRef>();
		List<String> names = new ArrayList<String>();
		try {
			names = registryProxy.list("GPSOffice");
		} catch (RemoteException e1) {
			System.err.print("Error retrieving names of GPSOffices");
			System.out.println(e1.getMessage());
		}

		for (String obj : names) {
			GPSOfficeRef office;
			try {
				office = (GPSOfficeRef) registryProxy.lookup(obj);
				if (office != null && !this.getCity().equals(office.getCity()))
					l.add(office);
			} catch (NotBoundException e) {
				System.out
						.println("Requested GPSOffice object not found in registry");
			} catch (RemoteException e) {
				System.out
						.println("Requested GPSOffice object unbounded from the registry");
				continue;
			}
		}
		return l;
	}

	public double getXValue() {
		return this.xValue;
	}

	public double getYValue() {
		return this.yValue;
	}

	private int parseInt(String arg, String name) {
		try {
			return Integer.parseInt(arg);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid " + name + ":" + arg);
		}
	}

	private double parseDouble(String arg, String name) {
		try {
			return Double.parseDouble(arg);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid " + name + ":" + arg);
		}
	}

	private final GPSOfficeRef getClosestOffice(Packet p) {

		double minDist = calculateEucledian(this.xValue, p.getxValue(),
				this.yValue, p.getyValue());
		GPSOfficeRef result = null;

		try {
			for (GPSOfficeRef office : neighbors) {
				double dist = calculateEucledian(office.getXValue(),
						p.getxValue(), office.getYValue(), p.getyValue());
				if (dist < minDist) {
					result = office;
					minDist = dist;
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return result;

	}

	@Override
	public void packetForward(final Packet packet) {

		executor.execute(new Runnable() {
			@Override
			public void run() {
				RemoteEventGenerator<CustomerEvent> remoteEventGenerator = new RemoteEventGenerator<CustomerEvent>();
				try {
					remoteEventGenerator.addListener(packet.getListener());
				} catch (RemoteException e1) {
					System.err.println(e1.getMessage());
				}

				String message = "Package number " + packet.getTrackingNumber()
						+ " arrived at " + cityName + " office";
				System.out.println(message);
				remoteEventGenerator.reportEvent(new CustomerEvent(message));
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				final GPSOfficeRef office = getClosestOffice(packet);

				if (office == null) {
					message = "Package number " + packet.getTrackingNumber()
							+ " delivered from " + cityName + " office to ("
							+ packet.getxValue() + "," + packet.getyValue()
							+ ")";
					System.out.println(message);
					remoteEventGenerator
							.reportEvent(new CustomerEvent(message));
				} else {
					try {
						office.packetForward(packet);
					} catch (RemoteException e) {
						System.err.println(e.getMessage());
					}
					message = "Package number " + packet.getTrackingNumber()
							+ " departed from " + cityName + " office";
					remoteEventGenerator
							.reportEvent(new CustomerEvent(message));
				}

			}
		});

	}

	public void sendPacket(double xVal, double yVal,
			RemoteEventListener<CustomerEvent> remoteListener)
			throws RemoteException {
		Packet packet = new Packet(xVal, yVal, System.currentTimeMillis(),
				remoteListener);
		packetForward(packet);
	}

	@Override
	public String getCity() throws RemoteException {
		return this.cityName;
	}
}