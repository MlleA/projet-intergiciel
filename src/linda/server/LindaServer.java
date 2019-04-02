package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;

import linda.Tuple;

public class LindaServer extends UnicastRemoteObject {
	
	protected LindaServer() throws RemoteException {
	}

	public static void main(String args[]) throws Exception {
		LinkedList<Tuple> espacePartage = new LinkedList<Tuple>();
	    Registry registry = LocateRegistry.createRegistry(4000);
	    registry.bind("EspacePartage",(Remote) espacePartage);
	}
}
