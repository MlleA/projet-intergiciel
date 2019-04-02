package linda.server;

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

public class LindaServer extends UnicastRemoteObject {

	protected LindaServer() throws RemoteException {
		centralizedLinda = new linda.shm.CentralizedLinda();
	}

	private static Linda centralizedLinda;

	public static void main(String args[]) throws Exception {	
		LindaServer server = new LindaServer();
	    Registry registry = LocateRegistry.createRegistry(4000);
	    registry.bind("LindaServer",server);
	}
	
	public void write(Tuple t) {
		centralizedLinda.write(t);
	}
	
	public Tuple take(Tuple template) {
		return centralizedLinda.take(template);
	}
	
	public Tuple read(Tuple template) {
		return centralizedLinda.read(template);
	}
	
	public Tuple tryTake(Tuple template) {
		return centralizedLinda.tryTake(template);
	}
	
	public Tuple tryRead(Tuple template) {
		return centralizedLinda.tryRead(template);
	}
	
	public Collection<Tuple> takeAll(Tuple template) {
		return centralizedLinda.takeAll(template);
	}
	
	public Collection<Tuple> readAll(Tuple template) {
		return centralizedLinda.readAll(template);
	}
	
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
		centralizedLinda.eventRegister(mode, timing, template, callback);
	}
	
	public void debug(String prefix) {
		centralizedLinda.debug(prefix);
	}
}
