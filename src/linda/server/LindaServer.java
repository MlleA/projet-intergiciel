package linda.server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class LindaServer extends UnicastRemoteObject implements ILindaServer {

	protected LindaServer() throws RemoteException {
		centralizedLinda = new linda.shm.CentralizedLinda();
	}

	private static Linda centralizedLinda;

	public static void main(String args[]) throws Exception {	
		LindaServer server = new LindaServer();
	    Registry registry = LocateRegistry.createRegistry(4000);
	    registry.bind("LindaServer",server);
	}
	
	@Override
	public void write(Tuple t) throws RemoteException{
		centralizedLinda.write(t);
	}
	
	@Override
	public Tuple take(Tuple template) throws RemoteException{
		return centralizedLinda.take(template);
	}
	
	@Override
	public Tuple read(Tuple template)throws RemoteException {
		return centralizedLinda.read(template);
	}
	
	@Override
	public Tuple tryTake(Tuple template)throws RemoteException {
		return centralizedLinda.tryTake(template);
	}
	
	@Override
	public Tuple tryRead(Tuple template)throws RemoteException {
		return centralizedLinda.tryRead(template);
	}
	
	@Override
	public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
		return centralizedLinda.takeAll(template);
	}
	
	@Override
	public Collection<Tuple> readAll(Tuple template) throws RemoteException {
		return centralizedLinda.readAll(template);
	}
	
	@Override
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, String remoteCallback) throws RemoteException {	
		IRemoteCallback rc;
		try {
			rc = (IRemoteCallback)Naming.lookup(remoteCallback);
			centralizedLinda.eventRegister(mode, timing, template, new RemoteCallbackToCallback(rc));
		} catch (MalformedURLException | NotBoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void debug(String prefix) throws RemoteException {
		centralizedLinda.debug(prefix);
	}
}
