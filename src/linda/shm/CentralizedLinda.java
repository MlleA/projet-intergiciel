package linda.shm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {
	//Choix LinkedList vs ArrayList à justifier dans le compte rendu
	LinkedList<Tuple> espacePartage = new LinkedList<Tuple>();
	
	private Lock lock;
	private Condition access;

	//Première case du tableau = Template ; Deuxième case = eventMode
	private Map<AsynchronousCallback, Object[]> cbRead;
	private Map<AsynchronousCallback, Object[]> cbTake;

	public CentralizedLinda() {
		cbRead = new LinkedHashMap<AsynchronousCallback, Object[]>();
		cbTake = new LinkedHashMap<AsynchronousCallback, Object[]>();
		lock = new ReentrantLock();
		access  = lock.newCondition();
	}

	@Override
	// Dépose le tuple dans l'espace partagé
	public void write(Tuple t) {
		boolean write = true;
		// Plus rapide d'ajouter en premier avec une linked list

		//Prévnir le callback qu'une écriture à eu lieu
		//notify.all() pour prévenir tous ceux en lock qu'une action à eu lieu
		//Choix du prioritaire : read
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
	//Extrait de l'espace partagé un tuple correspondant au motif précisé en paramètre
	public Tuple take(Tuple template) {
		lock.lock();
		
		Tuple retour = tryTake(template);
		while (retour == null) {
			try {
				synchronized (lock) {
					lock.wait();
				}
				retour = tryTake(template);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		lock.unlock();
		return retour;
	}

	@Override
	// Recherche (sans extraire) dans l'espace partagé un tuple correspondant au motif fourni en paramètre
	public Tuple read(Tuple template) {
		Tuple retour = tryRead(template);
		while (retour == null) {
			try {
				access.await();
				retour = tryRead(template);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
