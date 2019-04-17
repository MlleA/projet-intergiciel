package linda.server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

import linda.shm.*;

public class LindaMultiServer extends UnicastRemoteObject implements ILindaServer {

	private static String name;
	private static InitialContext ic;
	private  MessageProducer producerTopic;
	private  MessageConsumer consumerTopic;
	private  Session sessionPT;
	private  Session sessionST;
	private  Connection connection;
	private  Map<Integer, Tuple> demandesFaites;
	private  Map<Tuple, Object[]> demandesRecues;
	private  Map<Integer, Tuple> reponses;
	private  Map<Integer, LinkedList<Tuple>> reponsesAll;
	private  Map<Integer, LinkedList<String>> serveursRepondant;

	private CentralizedLinda centralizedLinda;

	protected LindaMultiServer(String myName) throws RemoteException {
		centralizedLinda = new linda.shm.CentralizedLinda();
		name = myName;
		demandesFaites = new LinkedHashMap<Integer, Tuple>();
		demandesRecues = new LinkedHashMap<Tuple, Object[]>();
		reponses = new LinkedHashMap<Integer, Tuple>();
		reponsesAll = new LinkedHashMap<Integer, LinkedList<Tuple>>();
		serveursRepondant = new LinkedHashMap<Integer, LinkedList<String>>();
		connectionToDestinations();
	}

	private  void connectionToDestinations() {
		try {
			ic = new InitialContext ();

			ConnectionFactory connectionFactory = (ConnectionFactory)ic.lookup("ConnFactory");
			//Connexion au Topic global
			Destination destination = (Destination)ic.lookup("TopicGlobal");

			connection = connectionFactory.createConnection();
			connection.start();

			sessionPT = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			sessionST = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);

			producerTopic = sessionPT.createProducer(destination);
			consumerTopic = sessionST.createConsumer(destination);

			//lecture d'un message sur le topic
			consumerTopic.setMessageListener(new ReadTopicListener());

			//ecoute sur sa queue
			Destination destination_privee = (Destination)ic.lookup(name);

			Session sessionCPrivée = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			MessageConsumer consumerPrivé = sessionCPrivée.createConsumer(destination_privee);

			consumerPrivé.setMessageListener(new ReadQueueListener());

		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
	}

	public static void main(String args[]) throws Exception {	
		//Récupération d'une queue libre
		ic = new InitialContext ();
		Destination serverLibre = null;
		int i = 0;

		while (serverLibre == null) {
			try {
				serverLibre = (Destination)ic.lookup("Queue_" + i);
			} catch (NamingException e) {
				i = i+1%4;
			}
		}
		ic.unbind("Queue_" + i);
		ic.bind("Server_" + i, serverLibre);

		//Création de l'instance du server et mise en disposition à distance
		LindaMultiServer server = new LindaMultiServer("Server_" + i);
		Registry registry;
		try {
			registry = LocateRegistry.createRegistry(4000);
		} catch (ExportException e) {
			registry = LocateRegistry.getRegistry(4000);
		}

		registry.bind("LindaServer" + i,server);
		System.out.println("LindaServer" + i);
	}

	@Override
	public void write(Tuple t) throws RemoteException{
		centralizedLinda.write(t);

		//On vérifie si on a pas reçu précédemment une demande correspondant à ce tuple
		for(Map.Entry<Tuple, Object[]> demandes : demandesRecues.entrySet()) {
			Tuple template = demandes.getKey();

			if (t.matches(template)) {
				String nom = (String) demandes.getValue()[0];
				int nbDemande = (int) demandes.getValue()[1];
				String method = (String) demandes.getValue()[2];

				Tuple result = null;
				if (method.contains("TAKE"))
					result = centralizedLinda.tryTake(template);
				else
					result = centralizedLinda.tryRead(template);

				//Informer que l'on possède le tuple correspondant au motif recherché
				if (result != null) {
					responseToServer(nom, result, null, nbDemande, method);
					demandesRecues.remove(template,demandes.getValue());
				}
			}
		}
	}

	private Tuple lectureMethods(Tuple template, String method, boolean withTry) {
		//Recherche locale du tuple correspondant au template
		Tuple tupleServeurCentral = null;
		if (method.contains("TAKE")) 
			tupleServeurCentral = centralizedLinda.tryTake(template);
		else
			tupleServeurCentral = centralizedLinda.tryRead(template);

		//Envoie sur le topic du template demandé
		if (tupleServeurCentral == null) {
			TextMessage txtMsg;
			try {
				Integer nbDemande = (int) Math.random();
				demandesFaites.put(nbDemande, template);

				txtMsg = sessionPT.createTextMessage();
				method = withTry ? "TRY" + method : method;
				txtMsg.setText(name + "::" + nbDemande + "::" + template.toString() + "::" + method);
				producerTopic.send(txtMsg);

				//attente de la réponse d'un des autres serveurs
				Tuple result = null;
				//Methode take/read
				if (!withTry) {
					do {
						result = reponses.get(nbDemande);
					} while (result == null);
					//Methode tryTake/tryRead
				} else {
					boolean getResponse = false;
					do {
						getResponse = reponses.containsKey(nbDemande);
					} while (!getResponse);

					//suppression de la demande et de la réponse
					result = reponses.get(nbDemande);
				}

				//suppression de la demande et de la réponse
				demandesFaites.remove(nbDemande);
				reponses.remove(nbDemande);

				return result;
			} catch (JMSException e) {
				e.printStackTrace();
				return null;
			}

		} else 
			return tupleServeurCentral;
	}

	@Override
	public Tuple take(Tuple template) throws RemoteException{
		return lectureMethods(template, "TAKE", false);
	}

	@Override
	public Tuple read(Tuple template)throws RemoteException {
		return lectureMethods(template, "READ", false);
	}

	@Override
	public Tuple tryTake(Tuple template)throws RemoteException {
		return lectureMethods(template, "TAKE", true);
	}

	@Override
	public Tuple tryRead(Tuple template)throws RemoteException {
		return lectureMethods(template, "READ", true);
	}
	
	private LinkedList<Tuple> lectureAllMethods(Tuple template, String method, LinkedList<Tuple> myTuples) {
		TextMessage txtMsg;
		try {
			Integer nbDemande = (int) Math.random();
			serveursRepondant.put(nbDemande, new LinkedList<String>());
			reponsesAll.put(nbDemande, new LinkedList<Tuple>());

			txtMsg = sessionPT.createTextMessage();
			txtMsg.setText(name + "::" + nbDemande + "::" + template.toString() + "::" + method);
			producerTopic.send(txtMsg);
			System.out.println("TAKEALL ENVOYE");

			//laisser le temps à tous les serveurs de répondre
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			LinkedList<String> serveurs; 
			do {
				serveurs = serveursRepondant.get(nbDemande);
			} while (!serveurs.isEmpty());

			myTuples.addAll(reponsesAll.get(nbDemande));
			return myTuples;
		} catch (JMSException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
		LinkedList<Tuple> myTuples = (LinkedList<Tuple>) centralizedLinda.takeAll(template);

		return lectureAllMethods(template, "TAKEALL", myTuples);
	}

	@Override
	public Collection<Tuple> readAll(Tuple template) throws RemoteException {
		LinkedList<Tuple> myTuples = (LinkedList<Tuple>) centralizedLinda.readAll(template);

		return lectureAllMethods(template, "READALL", myTuples);
	}

	@Override
	public void eventRegister(eventMode mode, eventTiming timing, Tuple template, String remoteCallback) throws RemoteException {	
		IRemoteCallback rc;
		try {
			rc = (IRemoteCallback)Naming.lookup(remoteCallback);
			centralizedLinda.eventRegister(mode, timing, template, new RemoteCallbackToCallback(rc));
		} catch (MalformedURLException | NotBoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void debug(String prefix) throws RemoteException {
		centralizedLinda.debug(prefix);
	}

	private  void responseToServer(String nameServer, Tuple result, LinkedList<Tuple> results, int nbDemande, String method) {
		Destination queueServerDest;
		try {
			//Envoie du tuple correspondant trouvé au serveur demandant
			queueServerDest = (Destination)ic.lookup(nameServer);
			Session sessionP = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			MessageProducer producer = sessionP.createProducer(queueServerDest);

			TextMessage message = sessionP.createTextMessage();
			if (method.contains("ALL")) {
				for (int i = 0; i < results.size(); i++) {
					message.setText(name + "::" + nbDemande + "::" + results.get(i).toString() + "::" + method);
					producer.send(message);
				}				
				message.setText(name + "::" + nbDemande + ":: ::" + method + "END");
				producer.send(message);
				System.out.println("REPONSE AU TAKEALL");

			} else {
				String tuple = result == null ? " " : result.toString();
				message.setText(name + "::" + nbDemande + "::" + tuple + "::" + method);
				producer.send(message);
			}

		} catch (NamingException | JMSException e) {
			e.printStackTrace();
		}
	}

	private  class ReadTopicListener implements MessageListener {
		public void onMessage(Message msg)  {
			try {
				TextMessage txt = (TextMessage) msg;
				String[] tabSplit = txt.getText().split("::");

				//découpe du message reçu
				String nom = tabSplit[0];
				int nbDemande = Integer.parseInt(tabSplit[1]);
				Tuple template = linda.Tuple.valueOf(tabSplit[2]);
				String method = tabSplit[3];

				Tuple result = null;
				LinkedList<Tuple> results = new LinkedList<Tuple>();

				if (!nom.equals(name)) {
					if (method.contains("TAKE")) {
						//takeAll
						if (method.contains("ALL")) {
							results = (LinkedList<Tuple>) centralizedLinda.takeAll(template);
							System.out.println("TAKEALL RECU");
						} else
							result = centralizedLinda.tryTake(template);

					} else {
						//readAll
						if (method.contains("ALL")) 
							results = (LinkedList<Tuple>) centralizedLinda.readAll(template);
						else
							result = centralizedLinda.tryRead(template);
					}

					//Informer que l'on possède ou pas le tuple correspondant au motif recherché
					if (result == null && !method.contains("TRY") && !method.contains("ALL")) {					
						//on enregistre la demande pour pouvoir y répondre plus tard
						Object[] infosDemande = {nom, nbDemande, method};
						demandesRecues.put(template, infosDemande);
					}
					responseToServer(nom, result, results, nbDemande, method);
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private  class ReadQueueListener implements MessageListener {
		public void onMessage(Message msg)  {
			try {
				TextMessage txt = (TextMessage) msg;
				String[] tabSplit = txt.getText().split("::");

				//découpe du message
				String nom = tabSplit[0];
				Integer nbDemande = Integer.parseInt(tabSplit[1]);
				Tuple tuple = null;
				if (!tabSplit[2].equals(" ")) tuple = linda.Tuple.valueOf(tabSplit[2]);
				String method = tabSplit[3];

				//On reçoit une réponse mais la demande est déjà clôturée
				if (demandesFaites.get(nbDemande) == null) {
					//Si la méthode est Take le serveur répondant a supprimé
					//inutilement le tuple donc on le recrée
					if(method.contains("TAKE") && !method.contains("ALL") && tuple != null)
						write(tuple);

					//on reçoit les réponses des takeAll/readAll
				}

				if (method.contains("ALL") && !method.contains("END")) {
					serveursRepondant.get(nbDemande).add(nom);
					reponsesAll.get(nbDemande).add(tuple);
				} else if (method.contains("ALL") && method.contains("END")) {
					serveursRepondant.get(nbDemande).remove(nom);
				} else
					reponses.put(nbDemande, tuple);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
