package linda.server;

import org.objectweb.joram.client.jms.Destination;
import org.objectweb.joram.client.jms.Topic;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

public class CreateDestination {
	public static void main(String args[]) throws Exception {
		// Connecting to JORAM server:
	    AdminModule.connect("root", "root", 60);

	    // Creating the JMS administered objects:        
	    javax.jms.ConnectionFactory connFactory =
	      TcpConnectionFactory.create("localhost", 16010);

	    Destination destination = Topic.create(0);
	}
}
