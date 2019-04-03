package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import linda.Callback;

public interface IRemoteCallback extends Remote {

	public Callback getCallback() throws RemoteException;
}
