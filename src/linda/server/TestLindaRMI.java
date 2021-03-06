package linda.server;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import linda.*;

public class TestLindaRMI {

    public static void main(String[] a) throws RemoteException, NotBoundException, MalformedURLException {                
        final LindaClient linda1 = new LindaClient("rmi://localhost:4000/LindaServer");
        final LindaClient linda2 = new LindaClient("rmi://localhost:4000/LindaServer");
  
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, String.class);
                Tuple res = linda1.take(motif);
                System.out.println("(1) Resultat:" + res);
                linda1.debug("(1)");
            }
        }.start();
                
       new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Tuple t1 = new Tuple(4, 5);
                System.out.println("(2) write: " + t1);
                linda2.write(t1);

                Tuple t11 = new Tuple(4, 5);
                System.out.println("(2) write: " + t11);
                linda2.write(t11);

                Tuple t2 = new Tuple("hello", 15);
                System.out.println("(2) write: " + t2);
                linda2.write(t2);

                Tuple t3 = new Tuple(4, "foo");
                System.out.println("(2) write: " + t3);
                linda2.write(t3);
                                
                linda2.debug("(2)");

            }
        }.start();
                
    }
}
