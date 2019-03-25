package linda.shm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {
	//Choix LinkedList vs ArrayList à justifier dans le compte rendu
	LinkedList<Tuple> espacePartage = new LinkedList<Tuple>();
	
	static Semaphore semaphore = new Semaphore(10);
	//UTILISER LES MONITORS A LA PLACE DES SEMAPHORES
	//Mutex mutex = new Mutex() ;
	Lock monitor;

    private List<AsynchronousCallback> cbRead = new ArrayList<AsynchronousCallback>();
    private List<AsynchronousCallback> cbTake = new ArrayList<AsynchronousCallback>();
	
	public CentralizedLinda() {
	}

	@Override
	// Dépose le tuple dans l'espace partagé
	public void write(Tuple t) {
		// Plus rapide d'ajouter en premier avec une linked list
		t.addFirst(espacePartage);
		
		//Prévnir le callback qu'une écriture à eu lieu
		//notify.all() pour prévenir tous ceux en lock qu'une action à eu lieu
		//Choix du prioritaire : read
		for(AsynchronousCallback cbRead : this.cbRead) {
			cbRead.call(t);
			monitor.notifyAll();
		}
		for(AsynchronousCallback cbWrite : this.cbTake) {
			cbWrite.call(t);
			monitor.notifyAll();
		}
	}

	@Override
	//Extrait de l'espace partagé un tuple correspondant au motif précisé en paramètre
	public Tuple take(Tuple template) {
		Tuple retour = new Tuple();
		
		monitor.lock();
		retour = tryTake(template);
		while (retour != null) {
			try {
				monitor.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		monitor.unlock();
		return retour;
		
//		try {
//			//semaphore.acquire();
//			monitor.lock();
//			retour = tryTake(template);
//			//utiliser notify all si callback => permet de réveiller les processus et de les rebloquer ensuite si tuple non correspondant
//			monitor.unlock();
//			//semaphore.release();
//			return retour;
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		//monitor.wait();
	}

	@Override
	// Recherche (sans extraire) dans l'espace partagé un tuple correspondant au motif fourni en paramètre
	public Tuple read(Tuple template) {
		try {
			semaphore.acquire();
			//monitor.lock();
			Tuple retour = tryRead(template);
			semaphore.release();
			//monitor.unlock();
			return retour;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//monitor.wait();
		return null;
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
				Tuple tuple = new Tuple (tryTake(template));
				cbTake.add((AsynchronousCallback) callback);
			}
			else if(mode == eventMode.READ) {
				Tuple tuple = new Tuple (tryRead(template));
				cbRead.add((AsynchronousCallback) callback);
			}
		} 
		//FUTUR : l'état courrant n'est pas considéré, seuls les tuples ajoutés à présent le sont
		else if (timing == eventTiming.FUTURE) {
			if (mode == eventMode.TAKE) {
				cbTake.add((AsynchronousCallback) callback);
			} 
			else if(mode == eventMode.READ) {
				cbRead.add((AsynchronousCallback) callback);
			}
		}
	}

	@Override
	public void debug(String prefix) {
		//Affiche le prefix donnant la ligne de debug
		System.out.println(prefix + " ");
		
		System.out.println("---- Affiche l'ensemble de l'espace partagé ---- \n");
			for(Tuple tuple : espacePartage) {
				System.out.println(tuple.toString());
			}
		
		System.out.println("---- Callback enregistrés ---- \n");
			System.out.println("Callback Read : \n");
			for(AsynchronousCallback cb : cbRead) {
				System.out.println(cb.toString());
			}
			
			System.out.println("\n Callback Take : \n");
			for(AsynchronousCallback cb : cbTake) {
				System.out.println(cb.toString());
			}
	}

	// TO BE COMPLETED

}
