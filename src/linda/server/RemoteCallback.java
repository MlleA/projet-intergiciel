package linda.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import linda.AsynchronousCallback;

public class RemoteCallback extends UnicastRemoteObject implements IRemoteCallback {

	private AsynchronousCallback asyncCallback;
	
	protected RemoteCallback(AsynchronousCallback acb) throws RemoteException {
		asyncCallback = acb;
	}

}
