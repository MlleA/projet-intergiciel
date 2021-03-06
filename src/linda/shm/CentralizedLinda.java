package linda.shm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {
	//Choix LinkedList vs ArrayList à justifier dans le compte rendu
	public LinkedList<Tuple> espacePartage;

	private Lock lock;

	//Première case du tableau = Template ; Deuxième case = eventMode
	private Map<AsynchronousCallback, Object[]> cbLecture;

	public CentralizedLinda() {
		espacePartage = new LinkedList<Tuple>();
		cbLecture = new LinkedHashMap<AsynchronousCallback, Object[]>();
		lock = new ReentrantLock();
	}

	@Override
	// Dépose le tuple dans l'espace partagé
	public void write(Tuple t) {
		boolean write = true;
		
		for(Map.Entry<AsynchronousCallback, Object[]> cbRead : this.cbLecture.entrySet()) {
			if (t.matches((Tuple) cbRead.getValue()[0])) {
				if ((eventMode) cbRead.getValue()[1] == eventMode.TAKE)
					write = false;
				cbRead.getKey().call(t);
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
	// Recherche (sans extraire) dans l'espace partagé un tuple correspondant au motif fourni en paramètre
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
		//IMMEDIATE : l'état courrant est considéré
		if (timing == eventTiming.IMMEDIATE) {
			if (mode == eventMode.TAKE) {
				Tuple tuple = tryTake(template);
				if (tuple != null) {
					(new AsynchronousCallback(callback)).call(tuple);
				}
			}
			else if(mode == eventMode.READ) {
				Tuple tuple = tryRead(template);
				if (tuple != null) {
					(new AsynchronousCallback(callback)).call(tuple);
				}
			}
		}
		cbLecture.put(new AsynchronousCallback(callback), new Object[] { template, mode });
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
		if (!cbLecture.entrySet().isEmpty()) System.out.println(prefix + " Callbacks : \n");
		for(Map.Entry<AsynchronousCallback, Object[]> cb : cbLecture.entrySet()) {
			Tuple template = (Tuple) cb.getValue()[0];
			eventMode eventMode = (eventMode) cb.getValue()[1];
			
			System.out.println(prefix + " eventMode : " + eventMode.name() + " template : " + template.toString());
		}

		System.out.println(prefix + " [FIN DEBUG] \n");

	}
}
