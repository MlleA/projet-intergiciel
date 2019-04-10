package linda.shm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda2 implements Linda {
	//Choix LinkedList vs ArrayList à justifier dans le compte rendu
	private LinkedList<Tuple> espacePartage;

	private final ReadWriteLock readWriteLock;
    private final Lock readLock;
    private final Lock writeLock;

	//Première case du tableau = Template ; Deuxième case = eventMode
	private Map<AsynchronousCallback, Object[]> cbRead;
	private Map<AsynchronousCallback, Object[]> cbTake;

	public CentralizedLinda2() {
		espacePartage = new LinkedList<Tuple>();
		cbRead = new LinkedHashMap<AsynchronousCallback, Object[]>();
		cbTake = new LinkedHashMap<AsynchronousCallback, Object[]>();
		
		readWriteLock = new ReentrantReadWriteLock();
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();
	}

	@Override
	// Dépose le tuple dans l'espace partagé
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
		synchronized(readLock) {
			readLock.notifyAll();
		}
		synchronized(writeLock) {
			writeLock.notifyAll();
		}
	}

	@Override
	//Extrait de l'espace partagé un tuple correspondant au motif précisé en paramètre
	public Tuple take(Tuple template) {
		writeLock.lock();
		Tuple retour = tryTake(template);
		while (retour == null) {
			synchronized (writeLock) {
				try {
					writeLock.wait();			
					retour = tryTake(template);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		writeLock.unlock();
		return retour;
	}

	@Override
	// Recherche (sans extraire) dans l'espace partagé un tuple correspondant au motif fourni en paramètre
	public Tuple read(Tuple template) {
		readLock.lock();
		Tuple retour = tryRead(template);
		while (retour == null) {
			synchronized(readLock) { 
				try {
					readLock.wait(); 
					retour = tryRead(template);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		readLock.unlock();
		return retour;
	}

	@Override
	//Version non bloquante de take
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
	//Version non bloquante de read
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
	//Renvoie, extrayant, tous les tuples correspondant au motif (vide si aucun ne correspond)
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
	//Renvoie, sans extraire, tous les tuples correspondant au motif (vide si aucun ne correspond)
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
	//S’abonner à l’existence/l’apparition d’un tuple correspondant au motif.
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {

		//callback.call() sera invoqué avec le tuple identifié. Le callback n’est déclenché qu’une fois, puis oublié.

		//IMMEDIATE : l'état courrant est considéré
		if (timing == eventTiming.IMMEDIATE) {
			if (mode == eventMode.TAKE) {
				cbTake.put(new AsynchronousCallback(callback), new Object[] { template, mode });

				Tuple tuple = tryTake(template);
				if (tuple != null) {
					(new AsynchronousCallback(callback)).call(tuple);
				}
			}
			else if(mode == eventMode.READ) {
				cbRead.put(new AsynchronousCallback(callback), new Object[] { template, mode });

				Tuple tuple = tryRead(template);
				if (tuple != null) {
					(new AsynchronousCallback(callback)).call(tuple);
				}
			}
		}
		//FUTUR : l'état courrant n'est pas considéré, seuls les tuples ajoutés à présent le sont
		else if (timing == eventTiming.FUTURE) {
			if (mode == eventMode.TAKE) {
				cbRead.put(new AsynchronousCallback(callback), new Object[] { template, mode });
			} 
			else if(mode == eventMode.READ) {
				cbRead.put(new AsynchronousCallback(callback), new Object[] { template, mode });
			}
		}
	}

	@Override
	public void debug(String prefix) {
		System.out.println(prefix + " [DEBUT DEBUG] \n");

		System.out.println(prefix + " ---- Affiche l'ensemble de l'espace partagé ---- \n");
		System.out.println(prefix + " Taille de l'espace partagé : " + espacePartage.size());
		for(Tuple tuple : espacePartage) {
			System.out.println(prefix + " " + tuple.toString());
		}

		System.out.println(prefix + " ---- Callback enregistrés ---- \n");
		if (!cbRead.entrySet().isEmpty()) System.out.println(prefix + " Callback Read : \n");
		for(Map.Entry<AsynchronousCallback, Object[]> cb : cbRead.entrySet()) {
			Tuple template = (Tuple) cb.getValue()[0];
			eventMode eventMode = (eventMode) cb.getValue()[1];
			
			System.out.println(prefix + " eventMode : " + eventMode.name() + " template : " + template.toString());
		}

		if (!cbTake.entrySet().isEmpty()) System.out.println("\n" + prefix + " Callback Take : \n");
		for(Map.Entry<AsynchronousCallback, Object[]> cb : cbTake.entrySet()) {
			Tuple template = (Tuple) cb.getValue()[0];
			eventMode eventMode = (eventMode) cb.getValue()[1];
			
			System.out.println(prefix + " eventMode : " + eventMode.name() + " template : " + template.toString());
		}

		System.out.println(prefix + " [FIN DEBUG] \n");

	}

}
