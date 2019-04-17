package linda.test;

import java.util.LinkedList;

import linda.*;

public class TestMultiServer1 {

    public static void main(String[] a) {
                
        //final Linda linda = new linda.shm.CentralizedLinda();
        final Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer0");
        final Linda linda2 = new linda.server.LindaClient("//localhost:4000/LindaServer1");
  
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple t3 = new Tuple(4, "foo2");
                System.out.println("(2) write: " + t3);
                linda.write(t3);
                
                Tuple motif = new Tuple(Integer.class, String.class);
                
                LinkedList<Tuple> res = (LinkedList<Tuple>) linda.readAll(motif);
                System.out.println("(1) Resultat readall:");
                res.forEach(t -> System.out.println("(1)" + t));
                linda.debug("(1)");
                
                res = (LinkedList<Tuple>) linda.readAll(motif);
                System.out.println("(11) Resultat readall:");
                res.forEach(t -> System.out.println("(11)" + t));
                linda.debug("(11)");
            }
        }.start();
                
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1);
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
