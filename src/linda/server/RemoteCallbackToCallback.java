package linda.server;

import java.rmi.RemoteException;

import linda.Callback;
import linda.Tuple;

public class RemoteCallbackToCallback implements Callback {

	private IRemoteCallback cb;

    public RemoteCallbackToCallback (IRemoteCallback cb) { this.cb = cb; }
	
	@Override
	public void call(Tuple t) {
		try {
			cb.call(t);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
