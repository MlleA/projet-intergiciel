package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

import linda.Tuple;

public interface IEspacePartage extends Remote {
	
	LinkedList<Tuple> getEspacePartage() throws RemoteException;

}
