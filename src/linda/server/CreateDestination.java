package linda.server;

import org.objectweb.joram.client.jms.admin.*;

import org.objectweb.joram.client.jms.*;
import org.objectweb.joram.client.jms.tcp.*;

public class CreateDestination {
	public static void main(String args[]) throws Exception {
		// Connecting to JORAM server:
		AdminModule.connect("root", "root", 60);

		// Creating the JMS administered objects:        
		javax.jms.ConnectionFactory connFactory =
				TcpConnectionFactory.create("localhost", 16010);

		Destination topic = Topic.create(0);

		// Creating an access for user anonymous:
		User.create("anonymous", "anonymous", 0);

		// Setting free access to the destination:
		topic.setFreeReading();
		topic.setFreeWriting();

		// Binding objects in JNDI:
		javax.naming.Context jndiCtx = new javax.naming.InitialContext();
		jndiCtx.bind("ConnFactory", connFactory);
		jndiCtx.bind("TopicGlobal", topic);
		
		for (int i = 0; i < 4; i++) {
	        Destination queue = Queue.create(0);
	        jndiCtx.bind("Queue_" + i, queue);
	        queue.setFreeReading();
	        queue.setFreeWriting();
	    }
	    
	    jndiCtx.close();
	    
	    AdminModule.disconnect();
	}
}
