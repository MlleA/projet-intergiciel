package linda.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;

import linda.Tuple;

public class EspacePartage extends UnicastRemoteObject implements IEspacePartage  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8661235084159209184L;
	private LinkedList<Tuple> espacePartage;

	protected EspacePartage() throws RemoteException {
		espacePartage = new LinkedList<Tuple>();
	}

	@Override
	public LinkedList<Tuple> getEspacePartage() throws RemoteException {
		return espacePartage;
	}

}
