

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryProxy;

public class Customer {

	private String hostName;
	private int portNumber;
	private String cityName;
	private double xValue;
	private double yValue;
	private RegistryProxy registryProxy;
	private Packet packet;
	private RemoteEventListener<CustomerEvent> remoteListner;

	public Customer(String[] args) throws IOException {

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
		}

	}

	private int parseI(String value, String name) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid value for " + name);
		}
	}

	private double parseD(String value, String name) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid value for " + name);
		}
	}

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

	public RemoteEventListener<CustomerEvent> createListener() {
		remoteListner = new RemoteEventListener<CustomerEvent>() {

			@Override
			public void report(long theSequenceNumber, CustomerEvent theEvent)
					throws RemoteException {
				String message = theEvent.getMessage();
				System.out.println(message);
				if (message.contains("delivered from")) {
					System.exit(0);
				}
			}
		};
		try {
			UnicastRemoteObject.exportObject(remoteListner, 0);
		} catch (RemoteException e) {
		}
		return remoteListner;
	}

	public static void main(String[] args) {
		try {
			Customer c = new Customer(args);
			GPSOfficeRef office = c.getObject();
			office.createPacket(c.xValue, c.yValue, c.createListener());
			office.packetForward(c.packet);
		} catch (IOException e) {
			e.getMessage();
		}
	}
}
