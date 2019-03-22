package linda.shm;

import java.util.Collection;
import java.util.LinkedList;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {
	LinkedList<Tuple> espacePartage = new LinkedList();

	public CentralizedLinda() {
		// Constructeur sans paramètre
	}

	@Override
	// Dépose le tuple dans l'espace partagé
	public void write(Tuple t) {
		// Plus rapide d'ajouter en premier avec une linked list
		t.addFirst(espacePartage);
	}

	@Override
	//Extrait de l'espace partagé un tuple correspondant au motif précisé en paramètre
	public Tuple take(Tuple template) {
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
	// Recherche (sans extraire) dans l'espace partagé un tuple correspondant au
	// motif fourni en paramètre
	public Tuple read(Tuple template) {
		for(int i = 0 ; i < espacePartage.size(); i++) {
			if ((espacePartage.get(i)).matches(template)){
				Tuple tuple = espacePartage.get(i);
				return tuple;
			}
		 }
		 return null;
	}

	@Override
	//Version non bloquante de take
	public Tuple tryTake(Tuple template) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tuple tryRead(Tuple template) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Tuple> readAll(Tuple template) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(String prefix) {
		// TODO Auto-generated method stub

	}

	// TO BE COMPLETED

}
