package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import linda.Callback;
import linda.Tuple;

public interface IRemoteCallback extends Remote {
	public void call(final Tuple t) throws RemoteException;
}
