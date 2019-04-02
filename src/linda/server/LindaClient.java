package linda.server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;


/** Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it is connected to.
 * */
public class LindaClient implements Linda {

	private IEspacePartage objectEspacePartage;
	private LinkedList<Tuple> espacePartage;

	private Map<AsynchronousCallback, Object[]> cbRead;
	private Map<AsynchronousCallback, Object[]> cbTake;

	private Lock lock;

	/** Initializes the Linda implementation.
	 *  @param serverURI the URI of the server, e.g. "rmi://localhost:4000/LindaServer" or "//localhost:4000/LindaServer".
	 * @throws RemoteException 
	 * @throws NotBoundException 
	 * @throws MalformedURLException 
	 */
	public LindaClient(String serverURI) throws RemoteException, NotBoundException, MalformedURLException {
		objectEspacePartage = (IEspacePartage)Naming.lookup(serverURI);
		espacePartage = objectEspacePartage.getEspacePartage();
	}

	@Override
	public void write(Tuple t) {
		boolean write = true;

		for(Map.Entry<AsynchronousCallback, Object[]> cbRead : this.cbRead.entrySet()) {
			if (t.matches((Tuple) cbRead.getValue()[0])) {
				if ((eventMode) cbRead.getValue()[1] == eventMode.TAKE)
					write = false;
				cbRead.getKey().call(t);
			}
		}
		for(Map.Entry<AsynchronousCallback, Object[]> cbTake : this.cbTake.entrySet()) {
			if (t.matches((Tuple) cbTake.getValue()[0])) {
				if ((eventMode) cbTake.getValue()[1] == eventMode.TAKE)
					write = false;
				cbTake.getKey().call(t);
			} 
		}

		if (write) {
			espacePartage.add(t);
		}

		//notify.all() pour prévenir tous ceux en wait() qu'une action à eu lieu
		synchronized(lock) {
			lock.notifyAll();
		}
	}

	@Override
	public Tuple take(Tuple template) {
		lock.lock();

		Tuple retour = tryTake(template);
		while (retour == null) {
			synchronized (lock) {
				try {
					lock.wait();			
					retour = tryTake(template);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		lock.unlock();
		return retour;
	}

	@Override
	public Tuple read(Tuple template) {
		Tuple retour = tryRead(template);
		while (retour == null) {
			synchronized(lock) { 
				try {
					lock.wait(); 
					retour = tryRead(template);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return retour;
	}

	@Override
	public Tuple tryTake(Tuple template) {
		for(int i = 0 ; i < espacePartage.size(); i++) {
			if ((espacePartage.get(i)).matches(template)){
				Tuple tuple = espacePartage.get(i);
				espacePartage.remove(i);
				return tuple;
			}
		}
		return null;
	}

	@Override
	public Tuple tryRead(Tuple template) {
		for(int i = 0 ; i < espacePartage.size(); i++) {
			if ((espacePartage.get(i)).matches(template)){
				Tuple tuple = espacePartage.get(i);
				return tuple;
			}
		}
		return null;
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) {
		Collection<Tuple> collectionTuples = new LinkedList<Tuple>();
		for(int i = 0 ; i < espacePartage.size(); i++) {
			if ((espacePartage.get(i)).matches(template)){
				collectionTuples.add(espacePartage.get(i));
				espacePartage.remove(i);
			}
		}
		return collectionTuples;
	}

	@Override
	public Collection<Tuple> readAll(Tuple template) {
		Collection<Tuple> collectionTuples = new LinkedList<Tuple>();
		for(Tuple tuple : espacePartage) {
			if (tuple.matches(template)){
				collectionTuples.add(tuple);
			}
		}
		return collectionTuples;
	}

	@Override
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
		//IMMEDIATE : l'état courrant est considéré
				if (timing == eventTiming.IMMEDIATE) {
					if (mode == eventMode.TAKE) {
						cbTake.put((AsynchronousCallback) callback, new Object[] { template, mode });

						Tuple tuple = new Tuple (tryTake(template));
						if (tuple != null) {
							((AsynchronousCallback) callback).call(tuple);
						}
					}
					else if(mode == eventMode.READ) {
						cbRead.put((AsynchronousCallback) callback, new Object[] { template, mode });

						Tuple tuple = new Tuple (tryRead(template));
						if (tuple != null) {
							((AsynchronousCallback) callback).call(tuple);
						}
					}
				}
				//FUTUR : l'état courrant n'est pas considéré, seuls les tuples ajoutés à présent le sont
				else if (timing == eventTiming.FUTURE) {
					if (mode == eventMode.TAKE) {
						cbRead.put((AsynchronousCallback) callback, new Object[] { template, mode });
					} 
					else if(mode == eventMode.READ) {
						cbRead.put((AsynchronousCallback) callback, new Object[] { template, mode });
					}
				}
	}

	@Override
	public void debug(String prefix) {
		System.out.println("[DEBUT DEBUG] \n");

		//Affiche le prefix donnant la ligne de debug
		System.out.println(prefix + " ");

		System.out.println("---- Affiche l'ensemble de l'espace partagé ---- \n");
		System.out.println("Taille de l'espace partagé : " + espacePartage.size());
		for(Tuple tuple : espacePartage) {
			System.out.println(tuple.toString());
		}

		System.out.println("---- Callback enregistrés ---- \n");
		if (!cbRead.entrySet().isEmpty()) System.out.println("Callback Read : \n");
		for(Map.Entry<AsynchronousCallback, Object[]> cb : cbRead.entrySet()) {
			System.out.println(cb.getKey().toString());
		}

		if (!cbTake.entrySet().isEmpty()) System.out.println("\nCallback Take : \n");
		for(Map.Entry<AsynchronousCallback, Object[]> cb : cbTake.entrySet()) {
			System.out.println(cb.getKey().toString());
		}

		System.out.println("[FIN DEBUG] \n");
	}

	// TO BE COMPLETED

}
