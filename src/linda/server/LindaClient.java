package linda.server;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Collection;
import linda.Callback;
import linda.Linda;
import linda.Tuple;


/** Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it is connected to.
 * */
public class LindaClient implements Linda {

	private ILindaServer lindaServer;
	private Registry registry;

	/** Initializes the Linda implementation.
	 *  @param serverURI the URI of the server, e.g. "rmi://localhost:4000/LindaServer" or "//localhost:4000/LindaServer".
	 * @throws RemoteException 
	 * @throws NotBoundException 
	 * @throws MalformedURLException 
	 */
	public LindaClient(String serverURI) {
		try {
			try {
				registry = LocateRegistry.createRegistry(4001);
			} catch (ExportException e) {
				registry = LocateRegistry.getRegistry(4001);
			}
			lindaServer = (ILindaServer)Naming.lookup(serverURI);
		} catch (RemoteException | MalformedURLException | NotBoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	// Dépose le tuple dans l'espace partagé
	public void write(Tuple t) {
		try {
			lindaServer.write(t);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	//Extrait de l'espace partagé un tuple correspondant au motif précisé en paramètre
	public Tuple take(Tuple template) {
		try {
			return lindaServer.take(template);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	// Recherche (sans extraire) dans l'espace partagé un tuple correspondant au motif fourni en paramètre
	public Tuple read(Tuple template) {
		try {
			return lindaServer.read(template);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	//Version non bloquante de take
	public Tuple tryTake(Tuple template) {
		try {
			return lindaServer.tryTake(template);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	//Version non bloquante de read
	public Tuple tryRead(Tuple template) {
		try {
			return lindaServer.tryRead(template);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	//Renvoie, extrayant, tous les tuples correspondant au motif (vide si aucun ne correspond)
	public Collection<Tuple> takeAll(Tuple template) {
		try {
			return lindaServer.takeAll(template);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	//Renvoie, sans extraire, tous les tuples correspondant au motif (vide si aucun ne correspond)
	public Collection<Tuple> readAll(Tuple template) {
		try {
			return lindaServer.readAll(template);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	//S’abonner à l’existence/l’apparition d’un tuple correspondant au motif.
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
		try {
			String nomCallback = "Callback" + Math.random();
			registry.bind(nomCallback, new RemoteCallback(callback));
			lindaServer.eventRegister(mode, timing, template, "rmi://localhost:4001/" + nomCallback);
		} catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void debug(String prefix) {
		try {
			lindaServer.debug(prefix);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
