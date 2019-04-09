package linda.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Tuple;

public class RemoteCallback extends UnicastRemoteObject implements IRemoteCallback {

	private Callback callback;
	
	protected RemoteCallback(Callback acb) throws RemoteException {
		callback = acb;
	}
	
	public void call(final Tuple t) throws RemoteException {
		callback.call(t);
	}

}
