

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryEvent;
import edu.rit.ds.registry.RegistryEventFilter;
import edu.rit.ds.registry.RegistryEventListener;
import edu.rit.ds.registry.RegistryProxy;

public class Headquarters {

	private String hostName;
	private int portNumber;
	private RegistryProxy registry;
	private RegistryEventListener registryListener;
	private RegistryEventFilter registryFilter;
	private RemoteEventListener<GPSOfficeEvent> remoteListener;
	private ExecutorService executor;

	public Headquarters(String[] args) throws RemoteException {
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
		executor = Executors.newCachedThreadPool();
	}

	public void initializeRegistry() throws RemoteException {
		registry = new RegistryProxy(hostName, portNumber);
		registryListener = new RegistryEventListener() {

			@Override
			public void report(long theSequenceNumber, RegistryEvent theEvent)
					throws RemoteException {
				final String str = new String(theEvent.objectName());
				executor.execute(new Runnable() {

					@Override
					public void run() {
						addSingleListener(str);
					}
				});
			}
		};

		UnicastRemoteObject.exportObject(registryListener, 0);
	}

	public void initializeRemoteListener() throws RemoteException {
		remoteListener = new RemoteEventListener<GPSOfficeEvent>() {

			@Override
			public void report(long theSequenceNumber, GPSOfficeEvent theEvent)
					throws RemoteException {
				System.out.println(theEvent.getMessage());
			}
		};
		UnicastRemoteObject.exportObject(remoteListener, 0);
	}

	public void initializeRegistryFilter() throws RemoteException {
		registryFilter = new RegistryEventFilter().reportType("GPSOfficeRef")
				.reportBound();
		registry.addEventListener(registryListener, registryFilter);
	}

	public void addListeners() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					for (String name : registry.list("GPSOfficeRef")) {
						GPSOfficeRef office = (GPSOfficeRef) registry
								.lookup(name);
						office.addListener(remoteListener);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (NotBoundException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void addSingleListener(String objectName) {
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
			headquarter.initializeRegistry();
			headquarter.initializeRemoteListener();
			headquarter.initializeRegistryFilter();
			headquarter.addListeners();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
