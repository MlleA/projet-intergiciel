package linda.shm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {
	LinkedList<Tuple> espacePartage = new LinkedList();
	static Semaphore semaphore = new Semaphore(10);
   
	//private Callback cb;
	//AsynchronousCallback callbackAsync = new AsynchronousCallback(cb);
	
    private List<AsynchronousCallback> cb = new ArrayList<AsynchronousCallback>();
    
	
	public CentralizedLinda() {
	}

	@Override
	// Dépose le tuple dans l'espace partagé
	public void write(Tuple t) {
		// Plus rapide d'ajouter en premier avec une linked list
		t.addFirst(espacePartage);
		
		//Prévnir le callback qu'une écriture à eu lieu
		for(AsynchronousCallback cb : this.cb) {
			cb.call(t);
		}	
	}

	@Override
	//Extrait de l'espace partagé un tuple correspondant au motif précisé en paramètre
	public Tuple take(Tuple template) {
		try {
			semaphore.acquire();
			Tuple retour = tryTake(template);
			semaphore.release();
			return retour;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	// Recherche (sans extraire) dans l'espace partagé un tuple correspondant au motif fourni en paramètre
	public Tuple read(Tuple template) {
		try {
			semaphore.acquire();
			Tuple retour = tryRead(template);
			semaphore.release();
			return retour;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		Collection<Tuple> collectionTuples = new LinkedList();
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
		Collection<Tuple> collectionTuples = new LinkedList();

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
		Tuple tuple = new Tuple ();
		
		//callback.call() sera invoqué avec le tuple identifié. Le callback n’est déclenché qu’une fois, puis oublié.
		
		//IMMEDIATE : l'état courrant est considéré
		if (timing == eventTiming.IMMEDIATE) {
			//eventMode = eventMode.TAKE (tuple retiré de l'espace)
			if (mode == eventMode.TAKE) {
				 tuple = take(template);
			} 
			//eventMode = eventMode.READ (tuple laissé dans l'espace)
			else if(mode == eventMode.READ) {
				tuple = read(template);
			}
		}

	}

	@Override
	public void debug(String prefix) {
		//Affiche le prefix donnant la ligne de debug
		System.out.println(prefix + " ");
		
		System.out.println("Affiche l'ensemble de l'espace partagé : \n");
		
		for(Tuple tuple : espacePartage) {
			System.out.println(tuple.toString());
		}
		
		System.out.println("Callback enregistrés : \n");
		//callback
	}

	// TO BE COMPLETED

}
