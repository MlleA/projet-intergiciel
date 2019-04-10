package linda.server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.LinkedHashMap;
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

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class LindaMultiServer extends UnicastRemoteObject implements ILindaServer {

	private static String name;
	private  MessageProducer producerTopic;
	private  MessageConsumer consumerTopic;
	private  Session sessionPT;
	private  Session sessionST;
	private  InitialContext ic;
	private  Connection connection;
	private  Map<Integer, Tuple> demandes;
	private  Map<Integer, Tuple> reponses;

	protected LindaMultiServer(String myName) throws RemoteException {
		centralizedLinda = new linda.shm.CentralizedLinda();
		name = myName;
		demandes = new LinkedHashMap<Integer, Tuple>();
		reponses = new LinkedHashMap<Integer, Tuple>();
		connectionToDestinations();
	}

	private Linda centralizedLinda;

	public static void main(String args[]) throws Exception {	
		LindaMultiServer server = new LindaMultiServer(args[1]);
		Registry registry = LocateRegistry.createRegistry(4000);
		registry.bind("LindaServer",server);
		System.out.println("TEST " + name);
	}

	@Override
	public void write(Tuple t) throws RemoteException{
		centralizedLinda.write(t);
	}

	@Override
	public Tuple take(Tuple template) throws RemoteException{
		//Recherche locale du tuple correspondant au template
		Tuple tupleServeurCentral = centralizedLinda.tryTake(template);

		//Envoie sur le topic du template demandé
		if (tupleServeurCentral == null) {
			TextMessage txtMsg;
			try {
				Integer nbDemande = (int) Math.random();
				demandes.put(nbDemande, template);

				txtMsg = sessionPT.createTextMessage();
				txtMsg.setText(name + "::" + nbDemande + "::" + template.toString() + "::TAKE");
				producerTopic.send(txtMsg);
				
				//attente de la réponse d'un des autres serveurs
				Tuple result = null;
				do {
					result = reponses.get(nbDemande);
				} while (result == null);
				
				return result;
			} catch (JMSException e) {
				e.printStackTrace();
				return null;
			}
		} else 
			return tupleServeurCentral;
	}

	@Override
	public Tuple read(Tuple template)throws RemoteException {
		return centralizedLinda.tryRead(template);

		//if()
	}

	@Override
	public Tuple tryTake(Tuple template)throws RemoteException {
		return centralizedLinda.tryTake(template);
	}

	@Override
	public Tuple tryRead(Tuple template)throws RemoteException {
		return centralizedLinda.tryRead(template);
	}

	@Override
	public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
		return centralizedLinda.takeAll(template);
	}

	@Override
	public Collection<Tuple> readAll(Tuple template) throws RemoteException {
		return centralizedLinda.readAll(template);
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


	private  class ReadTopicListener implements MessageListener {
		public void onMessage(Message msg)  {
			try {
				TextMessage txt = (TextMessage) msg;
				String[] tabSplit = txt.getText().split("::");

				String nom = tabSplit[0];
				int nbDemande = Integer.parseInt(tabSplit[1]);
				Tuple template = linda.Tuple.valueOf(tabSplit[2]);
				String method = tabSplit[3];

				Tuple reception = null;
				if (method.equals("TAKE"))
					reception = centralizedLinda.tryTake(template);
				else
					reception = centralizedLinda.tryRead(template);

				//Informer que l'on possède le tuple correspondant au motif recherché
				if (reception != null) {
					responseToServer(nom, reception, nbDemande, method);
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

				String nom = tabSplit[0];
				Integer nbDemande = Integer.parseInt(tabSplit[1]);
				Tuple tuple = linda.Tuple.valueOf(tabSplit[2]);
				String method = tabSplit[3];

				//On reçoit une réponse mais la demande est déjà clôturée
				if (demandes.get(nbDemande) == null && method.equals("TAKE")) {
					//Si la méthode est Take le serveur répondant a supprimé
					//inutilement le tuple donc on le recrée
					write(tuple);
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}
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

	private  void responseToServer(String nameServer, Tuple reception, int nbDemande, String method) {
		Destination queueServerDest;
		try {
			//Envoie du tuple correspondant trouvé au serveur demandant
			queueServerDest = (Destination)ic.lookup(nameServer);
			Session sessionP = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			MessageProducer producer = sessionP.createProducer(queueServerDest);

			TextMessage message = sessionP.createTextMessage();
			message.setText(name + "::" + nbDemande + "::" + reception.toString() + "::" + method);
			producer.send(message);
		} catch (NamingException | JMSException e) {
			e.printStackTrace();
		}
	}


}
