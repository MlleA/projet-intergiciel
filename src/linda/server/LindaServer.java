package linda.server;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LindaServer {

	public static void main(String args[]) throws Exception {
		EspacePartage espacePartage = new EspacePartage();
	    Registry registry = LocateRegistry.createRegistry(4000);
	    registry.bind("EspacePartage",espacePartage);
	}
}
