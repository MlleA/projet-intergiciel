package linda.test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import linda.*;

public class PerformanceTest {

    public static void main(String[] a) {

        final Linda linda = new linda.shm.CentralizedLinda();
        // final Linda linda = new linda.server.LindaClient("//localhost:4000/aaa");
        
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if (!threadMXBean.isThreadCpuTimeSupported())
            throw new UnsupportedOperationException("JVM does not support measuring thread CPU-time");

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                long startTime = threadMXBean.getThreadCpuTime(Thread.currentThread().getId());
                for (int i = 0; i < 500000; i++) {
                    Tuple motif = new Tuple(Integer.class, Integer.class);
                    Tuple res = linda.take(motif);
                }
                long finishTime = threadMXBean.getThreadCpuTime(Thread.currentThread().getId());
                long elapsedTime = (finishTime - startTime) / 1000000;
                System.out.println("(1) Elapsed time for 500000 take : " + elapsedTime + " ms");
                
                linda.debug("(1)");
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                long startTime = threadMXBean.getThreadCpuTime(Thread.currentThread().getId());
                for (int i = 0; i < 1000000; i++) {
                    Tuple t1 = new Tuple(4, 5);
                    linda.write(t1);
                }
                long finishTime = threadMXBean.getThreadCpuTime(Thread.currentThread().getId());
                long elapsedTime = (finishTime - startTime) / 1000000;
                System.out.println("(3) Elapsed time for 1000000 write : " + elapsedTime + " ms");
                                
                linda.debug("(3)");

            }
        }.start();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        final List<Future<Long>> results = new ArrayList<>();
        for (int threadNumber = 0; threadNumber < 20; threadNumber++) {
        	final int number = threadNumber;
        	Callable<Long> task = new Callable<Long>() {
				@Override
				public Long call() throws Exception {
	                try {
	                    Thread.sleep(2 * (number + 1));
	                } catch (InterruptedException e) {
	                    e.printStackTrace();
	                }
	                
	                long startTime = threadMXBean.getThreadCpuTime(Thread.currentThread().getId());
	                for (int i = 0; i < 25000; i++) {
	                    Tuple motif = new Tuple(Integer.class, Integer.class);
	                    Tuple res = linda.read(motif);
	                }
	                long finishTime = threadMXBean.getThreadCpuTime(Thread.currentThread().getId());
	                long elapsedTime = (finishTime - startTime) / 1000000;
					return elapsedTime;
				}
        	};
        	Future<Long> futureResult = executor.submit(task);
        	results.add(futureResult);
        }

    	try {
	        long totalTime = 0;
	        for (int i = 0; i < 20; i++) {
				totalTime += results.get(i).get().longValue();
	        }
            System.out.println("(2) Elapsed time for 500000 read (over 20 threads): " + totalTime + " ms");
            
            linda.debug("(2)");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
    	executor.shutdown();
    }
}
