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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class LindaMultiServer extends UnicastRemoteObject implements ILindaServer {

	protected LindaMultiServer() throws RemoteException {
		centralizedLinda = new linda.shm.CentralizedLinda();
	}

	private static Linda centralizedLinda;

	public static void main(String args[]) throws Exception {	
		LindaServer server = new LindaServer();
	    Registry registry = LocateRegistry.createRegistry(4000);
	    registry.bind("LindaServer",server);
	}
	
	@Override
	public void write(Tuple t) throws RemoteException{
		centralizedLinda.write(t);
	}
	
	@Override
	public Tuple take(Tuple template) throws RemoteException{
		return centralizedLinda.tryTake(template);
	}
	
	@Override
	public Tuple read(Tuple template)throws RemoteException {
		return centralizedLinda.tryRead(template);
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
	
	private void connectionToTopic() {
		try {
            InitialContext ic = new InitialContext ();

            ConnectionFactory connectionFactory = (ConnectionFactory)ic.lookup("ConnFactory");
            Destination destination = (Destination)ic.lookup("MonTopic");

            System.out.println("Bound to ConnFactory and MonTopic");

            Connection connection = connectionFactory.createConnection();
            connection.start();

            System.out.println("Created connection");

            System.out.println("Creating sessions: not transacted, auto ack");
            Session sessionP = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
            Session sessionS = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);

            MessageProducer producer = sessionP.createProducer(destination);
            MessageConsumer consumer = sessionS.createConsumer(destination);

            consumer.setMessageListener(new MessageListener() {
                    public void onMessage(Message msg)  {
                        try {
                            TextMessage textmsg = (TextMessage)msg;
                            System.out.println("I have received : " + textmsg.getText());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        
                    }});

            System.out.println("Ready");

            TextMessage textmsg = sessionP.createTextMessage();
            textmsg.setText("Hello World !!!");
            producer.send(textmsg);

        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
}
