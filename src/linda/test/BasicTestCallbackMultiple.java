
package linda.test;

import linda.Callback;
import linda.Linda;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.Tuple;

public class BasicTestCallbackMultiple {

    private static Linda linda;
    private static Tuple cbmotif;


    private static class MyCallback implements Callback {
        private static int num = 0;

        public void call(Tuple t) {
            int numPrive = MyCallback.num;
            MyCallback.num ++;
            System.out.println(numPrive + " CB got "+t);
            linda.eventRegister(eventMode.TAKE, eventTiming.FUTURE, cbmotif, this);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.out.println(numPrive + " CB done with "+t);
        }
    }

    public static void main(String[] a) {
        linda = new linda.shm.CentralizedLinda();
        //linda.Linda linda = new linda.linda.server.LindaClient("//localhost:4000/MonServeur");

        cbmotif = new Tuple(Integer.class, String.class);
        linda.eventRegister(eventMode.READ, eventTiming.IMMEDIATE, cbmotif, new MyCallback());

        Tuple t1 = new Tuple(4, 5);
        System.out.println("(2) write: " + t1);
        linda.write(t1);

        Tuple t2 = new Tuple("hello", 15);
        System.out.println("(2) write: " + t2);
        linda.write(t2);
        linda.debug("(2)");

        Tuple t3 = new Tuple(4, "foo");
        System.out.println("(2) write: " + t3);
        linda.write(t3);

        linda.debug("(2)");

    }

}
