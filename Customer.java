

import java.util.ArrayList;
import java.util.List;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

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

	public Customer(String[] args) {
		this.hostName = args[0];
		this.cityName = args[2];
		try {
			this.portNumber = Integer.parseInt(args[1]);
			this.xValue = Double.parseDouble(args[3]);
			this.yValue = Double.parseDouble(args[4]);
		} catch(NumberFormatException e) {
			e.getMessage();
		}
		packet = new Packet();
	}

	private GPSOfficeRef getObject() {
		try {
			registryProxy = new RegistryProxy(hostName, portNumber);
			return (GPSOfficeRef) registryProxy.lookup(cityName);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} 
		return null;
	}
	
	public static void main(String[] args) {
		Customer c = new Customer(args);
		GPSOfficeRef office = c.getObject();
		c.packet.setxValue(c.xValue);
		c.packet.setyValue(c.yValue);
		try {
			office.packetForward(c.packet);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
//		List<String> list = new ArrayList<String>();
//		try {
//			list = c.getObject().packetForward();
//		} catch (RemoteException e) {
//			e.printStackTrace();
//		}
//		for(String s : list) {
//			System.out.println(s);
//		}
	}

}
