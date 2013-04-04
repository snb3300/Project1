

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
	private RemoteEventGenerator<GPSOfficeEvent> remoteGenerator;
	private List<NeighborStorage> neighbors;
	// private int totalOfficeObjects;
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
		neighbors = new ArrayList<NeighborStorage>();
		// Collections.synchronizedList(neighbors);
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

		// totalOfficeObjects = 0;
		updateNeighbors();
		registryEventListener = new RegistryEventListener() {
			@Override
			public void report(long arg0, RegistryEvent event)
					throws RemoteException {
				final String name = new String(event.objectName());
				if (event.objectWasBound()) {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								GPSOfficeRef office = (GPSOfficeRef) registryProxy
										.lookup(name);
								addNewNeighbor(office);
							} catch (RemoteException e) {
								e.printStackTrace();
							} catch (NotBoundException e) {
								e.printStackTrace();
							}
							printNeighbor();
						}
					});
				} else {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							deleteNeighbor(name);
							printNeighbor();
						}
					});
				}
			}
		};

		UnicastRemoteObject.exportObject(registryEventListener, 0);

		registryEventFilter = new RegistryEventFilter();
		registryEventFilter.reportType("GPSOffice").reportBound();
		registryEventFilter.reportType("GPSOffice").reportUnbound();
		registryProxy.addEventListener(registryEventListener,
				registryEventFilter);
		remoteGenerator = new RemoteEventGenerator<GPSOfficeEvent>();
		printNeighbor();
	}

	private void addNewNeighbor(GPSOfficeRef office) {

		try {

			synchronized (neighbors) {
				if (office != null) {
					double dist = getDistance(this, office);
					NeighborStorage neighbor = new NeighborStorage(office,
							office.getCity(), office.getXValue(),
							office.getYValue(), dist);
					if (neighbors.size() < maxNeighbors) {
						neighbors.add(neighbor);
					} else {
						for (int i = 0; i < maxNeighbors; i++) {
							if (dist < neighbors.get(i).getDistance()) {
								neighbors.set(i, neighbor);
								break;
							}
						}
					}
				}
			}
			Collections.sort(neighbors);
		} catch (RemoteException e) {
			System.out.println("Remote exp in add New");
		}
	}

	private void deleteNeighbor(String name) {
		synchronized (neighbors) {
			Iterator<NeighborStorage> iterator = neighbors.iterator();
			while (iterator.hasNext()) {
				if (iterator.next().getCity().equals(name)) {
					iterator.remove();
					break;
				}
			}
		}
		updateNeighbors();
	}

	private boolean isPresent(GPSOfficeRef office) {
		boolean ret = false;
		int lim = Math.min(neighbors.size(), maxNeighbors);
		for (int i = 0; i < lim; i++) {
			if (neighbors.get(i).getOffice().equals(office))
				return true;
		}
		return ret;
	}

	private void printNeighbor() {
		int lim = Math.min(maxNeighbors, neighbors.size());
		for (int i = 0; i < lim; i++) {
			System.out.print(neighbors.get(i).getCity() + " ");
		}
		System.out.println();
	}

	private void updateNeighbors() {
		List<String> names = new ArrayList<String>();
		try {
			names = registryProxy.list("GPSOfficeRef");
		} catch (RemoteException e1) {
			System.err.print("Error retrieving names of GPSOffices");
			System.out.println(e1.getMessage());
		}

		synchronized (neighbors) {
			for (String obj : names) {
				GPSOfficeRef office;
				try {
					office = (GPSOfficeRef) registryProxy.lookup(obj);
					if (office != null
							&& !this.getCity().equals(office.getCity())
							&& !isPresent(office)) {
						addNewNeighbor(office);
					}

				} catch (RemoteException e) {
					System.out
							.println("Remote exception in getting all neighbors");
					continue;
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					System.out.println("Not bound");
				}
			}
		}
	}

	// private void checkForDelete(List<GPSOfficeRef> list) {
	// synchronized (neighbors) {
	// Iterator<GPSOfficeRef> it = neighbors.iterator();
	// while (it.hasNext()) {
	// if (!list.contains(it.next())) {
	// it.remove();
	// }
	// }
	// // for (GPSOfficeRef neighbor : neighbors) {
	// // if (!list.contains(neighbor))
	// // neighbors.remove(neighbor);
	// // }
	// }
	// }

	private double calculateEucledian(double x1, double x2, double y1, double y2) {
		double distance = 0.0;
		double x = x2 - x1;
		double y = y2 - y1;
		distance = Math.sqrt((x * x) + (y * y));
		return distance;
	}

	private double getDistance(GPSOfficeRef office1, GPSOfficeRef office2)
			throws RemoteException {
		double distance = 0.0;
		if (office2 != null) {
			distance = calculateEucledian(office1.getXValue(),
					office2.getXValue(), office1.getYValue(),
					office2.getYValue());
		}
		return distance;
	}

	// private void getNearestNeighbors(List<GPSOfficeRef> allNeighbors) {
	// for (GPSOfficeRef office : allNeighbors) {
	// if ((office != null) && (!neighbors.contains(office))) {
	// addNewNeighbor(office);
	// }
	// }
	// }

	// private void getAllNeighbors() {
	//
	// }

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

	// private final GPSOfficeRef getClosestOffice(Packet p) {
	// synchronized (neighbors) {
	// double minDist = calculateEucledian(this.xValue, p.getxValue(),
	// this.yValue, p.getyValue());
	// GPSOfficeRef result = null;
	//
	// try {
	// for (GPSOfficeRef office : neighbors) {
	// double dist = calculateEucledian(office.getXValue(),
	// p.getxValue(), office.getYValue(), p.getyValue());
	// if (dist < minDist) {
	// result = office;
	// minDist = dist;
	// }
	// }
	// } catch (IOException e) {
	// System.err.println(e.getMessage());
	// }
	// return result;
	// }
	// }

	@Override
	public void packetForward(final Packet packet) throws RemoteException {

		RemoteEventGenerator<CustomerEvent> remoteEventGenerator = new RemoteEventGenerator<CustomerEvent>();

		// executor.execute(new Runnable() {
		// @Override
		// public void run() {

		try {
			remoteEventGenerator.addListener(packet.getListener());
		} catch (RemoteException e1) {
			System.err.println(e1.getMessage());
		}

		String message = "Package number " + packet.getTrackingNumber()
				+ " arrived at " + cityName + " office";
		System.out.println(message);
		// remoteGenerator.reportEvent(new GPSOfficeEvent(message));
		remoteEventGenerator.reportEvent(new CustomerEvent(message));
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			System.out.println("Thread interuupted");
			throw new IllegalArgumentException("Office shutdown");
		}

		if (neighbors.size() <= 0) {
			message = "Package number " + packet.getTrackingNumber()
					+ " delivered from " + cityName + " office to ("
					+ packet.getxValue() + "," + packet.getyValue() + ")";
			System.out.println(message);
			remoteEventGenerator.reportEvent(new CustomerEvent(message));
		} else {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					GPSOfficeRef office = neighbors.get(0).getOffice();
					try {
						office.packetForward(packet);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			message = "Package number " + packet.getTrackingNumber()
					+ " departed from " + cityName + " office";
			remoteEventGenerator.reportEvent(new CustomerEvent(message));
			// remoteGenerator.reportEvent(new GPSOfficeEvent(message));
		}

	}

	// });
	// }

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

	@Override
	public void addListener(RemoteEventListener<GPSOfficeEvent> listener)
			throws RemoteException {
		remoteGenerator.addListener(listener);
	}

}