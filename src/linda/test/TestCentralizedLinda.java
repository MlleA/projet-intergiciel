package linda.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;

public class TestCentralizedLinda {

	private static final Tuple singleIntegerTuple = new Tuple(3);
	private static final Tuple doubleIntegerTuple = new Tuple(4, 5);
	private static final Tuple integerAndStringTuple = new Tuple("test", 2);
	private static final Tuple doubleStringTuple = new Tuple("test", "tuple");
	private static final Tuple[] testData = { singleIntegerTuple, doubleIntegerTuple, integerAndStringTuple, doubleStringTuple };

    public Linda linda;
	public ExecutorService executor;
	public final static Lock lock = new ReentrantLock();
	public final static Condition callbackFinished = lock.newCondition();

	@BeforeEach
	public void setUp() throws Exception {
		this.linda = new linda.shm.CentralizedLinda2();
		this.executor = Executors.newFixedThreadPool(20);
	}

	@AfterEach
	public void tearDown() throws Exception {
		this.linda = null;
		this.executor = null;
	}

	@Test
	public void testTake() throws InterruptedException, ExecutionException {
		this.insertData(Arrays.asList(testData), 1000);

    	Future<Tuple> testTooEarly = this.take(new Tuple(Integer.class, Integer.class), 10);
    	Future<Tuple> testTake = this.take(new Tuple(Integer.class), 2000);
    	Future<Tuple> testAnotherTake = this.take(new Tuple(String.class, Integer.class), 2000);
    	
    	Tuple secondTake = new Tuple(14, 15);
    	this.insertData(Arrays.asList(secondTake), 3000);
    	Future<Tuple> testSecondTake = this.take(new Tuple(Integer.class, Integer.class), 4000);

    	assertTrue(doubleIntegerTuple.matches(testTooEarly.get()));
    	assertTrue(singleIntegerTuple.matches(testTake.get()));
    	assertTrue(integerAndStringTuple.matches(testAnotherTake.get()));
    	assertTrue(secondTake.matches(testSecondTake.get()));
	}

	@Test
	public void testRead() throws InterruptedException, ExecutionException {
		this.insertData(Arrays.asList(testData), 1000);

    	Future<Tuple> testTooEarly = this.read(new Tuple(Integer.class, Integer.class), 10);
    	Future<Tuple> testRead = this.read(new Tuple(Integer.class), 2000);
    	Future<Tuple> testAnotherRead = this.read(new Tuple(String.class, Integer.class), 2000);
    	Future<Tuple> testSecondRead = this.read(new Tuple(Integer.class, Integer.class), 3000);

    	assertTrue(doubleIntegerTuple.matches(testTooEarly.get()));
    	assertTrue(singleIntegerTuple.matches(testRead.get()));
    	assertTrue(integerAndStringTuple.matches(testAnotherRead.get()));
    	assertTrue(doubleIntegerTuple.matches(testSecondRead.get()));
	}

	@Test
	public void testTryTake() throws InterruptedException, ExecutionException {
		this.insertData(Arrays.asList(testData), 1000);

    	Future<Tuple> testTooEarly = this.tryTake(new Tuple(Integer.class, Integer.class), 10);
    	Future<Tuple> testTryTake = this.tryTake(new Tuple(Integer.class, Integer.class), 2000);
    	Future<Tuple> testAnotherTake = this.tryTake(new Tuple(String.class, Integer.class), 2000);
    	Future<Tuple> testSecondTake = this.tryTake(new Tuple(Integer.class, Integer.class), 3000);

    	assertNull(testTooEarly.get());
    	assertTrue(doubleIntegerTuple.matches(testTryTake.get()));
    	assertTrue(integerAndStringTuple.matches(testAnotherTake.get()));
    	assertNull(testSecondTake.get());
	}

	@Test
	public void testTryRead() throws InterruptedException, ExecutionException {
		this.insertData(Arrays.asList(testData), 1000);

    	Future<Tuple> testTooEarly = this.tryRead(new Tuple(Integer.class, Integer.class), 10);
    	Future<Tuple> testTryRead = this.tryRead(new Tuple(Integer.class, Integer.class), 2000);
    	Future<Tuple> testAnotherRead = this.tryRead(new Tuple(String.class, Integer.class), 2000);
    	Future<Tuple> testSecondRead = this.tryRead(new Tuple(Integer.class, Integer.class), 3000);

    	assertNull(testTooEarly.get());
    	assertTrue(doubleIntegerTuple.matches(testTryRead.get()));
    	assertTrue(integerAndStringTuple.matches(testAnotherRead.get()));
    	assertTrue(doubleIntegerTuple.matches(testSecondRead.get()));
	}

	@Test
	public void testTakeAll() throws InterruptedException, ExecutionException {
		List<Tuple> tuples = Arrays.asList(new Tuple(2, 3), new Tuple(4, 5), new Tuple(6, 7));
		Tuple extraTuple = new Tuple("test", 0);
		this.insertData(tuples, 1000);
		this.insertData(Arrays.asList(extraTuple), 1000);

    	Future<Collection<Tuple>> testTooEarly = this.takeAll(new Tuple(Integer.class, Integer.class), 10);
    	Future<Collection<Tuple>> testTakeAll = this.takeAll(new Tuple(Integer.class, Integer.class), 1500);
    	Future<Tuple> testReadAfterTakeAll = this.read(new Tuple(String.class, Integer.class), 2000);
    	Future<Collection<Tuple>> testSecondTakeAll = this.takeAll(new Tuple(Integer.class, Integer.class), 3000);
 
    	assertTrue(testTooEarly.get().isEmpty());
    	assertTrue(tuples.containsAll(testTakeAll.get()));
    	assertTrue(extraTuple.matches(testReadAfterTakeAll.get()));
    	assertTrue(testSecondTakeAll.get().isEmpty());
	}

	@Test
	public void testReadAll() throws InterruptedException, ExecutionException {
		List<Tuple> tuples = Arrays.asList(new Tuple(2, 3), new Tuple(4, 5), new Tuple(6, 7));
		Tuple extraTuple = new Tuple("test", 0);
		this.insertData(tuples, 1000);
		this.insertData(Arrays.asList(extraTuple), 1000);

    	Future<Collection<Tuple>> testTooEarly = this.readAll(new Tuple(Integer.class, Integer.class), 10);
    	Future<Collection<Tuple>> testReadAll = this.readAll(new Tuple(Integer.class, Integer.class), 1500);
    	Future<Tuple> testReadAfterReadAll = this.read(new Tuple(String.class, Integer.class), 2000);
    	Future<Collection<Tuple>> testSecondReadAll = this.readAll(new Tuple(Integer.class, Integer.class), 3000);
 
    	assertTrue(testTooEarly.get().isEmpty());
    	assertTrue(tuples.containsAll(testReadAll.get()));
    	assertTrue(extraTuple.matches(testReadAfterReadAll.get()));
    	assertTrue(tuples.containsAll(testSecondReadAll.get()));
	}

	@Test
	public void testCallbackTakeFuture() throws InterruptedException, ExecutionException {
		Tuple extraTuple = new Tuple(14, 15);
		
        Tuple motif = new Tuple(Integer.class, Integer.class);
        FakeCallback callback = new FakeCallback(extraTuple);
		this.insertData(Arrays.asList(testData), 0);
		this.eventRegister(eventMode.TAKE, eventTiming.FUTURE, motif, callback, 1000);
		this.insertData(Arrays.asList(extraTuple), 2000);
    	Future<Collection<Tuple>> testRead = this.readAll(motif, 3000);
		
    	lock.lock();
		callbackFinished.await();
    	lock.unlock();

		assertTrue(callback.hasHandleBeenCalled());
		assertTrue(callback.isSuccessful());
		assertTrue(testRead.get().contains(doubleIntegerTuple));
		assertFalse(testRead.get().contains(extraTuple));
	}

	@Test
	public void testCallbackReadFuture() throws InterruptedException, ExecutionException {
		Tuple extraTuple = new Tuple(14, 15);
		
        Tuple motif = new Tuple(Integer.class, Integer.class);
        FakeCallback callback = new FakeCallback(extraTuple);
		this.insertData(Arrays.asList(testData), 0);
		this.eventRegister(eventMode.READ, eventTiming.FUTURE, motif, callback, 1000);
		this.insertData(Arrays.asList(extraTuple), 2000);
    	Future<Collection<Tuple>> testRead = this.readAll(motif, 3000);

    	lock.lock();
		callbackFinished.await();
    	lock.unlock();

		assertTrue(callback.hasHandleBeenCalled());
		assertTrue(callback.isSuccessful());
		assertTrue(testRead.get().contains(doubleIntegerTuple));
		assertTrue(testRead.get().contains(extraTuple));
	}
	
	@Test
	public void testCallbackTakeImmediate() throws InterruptedException, ExecutionException {
		Tuple extraTuple = new Tuple(14, 15);
		
        Tuple motif = new Tuple(Integer.class, Integer.class);
        FakeCallback callback = new FakeCallback(doubleIntegerTuple);
		this.insertData(Arrays.asList(testData), 0);
		this.eventRegister(eventMode.TAKE, eventTiming.IMMEDIATE, motif, callback, 1000);
		this.insertData(Arrays.asList(extraTuple), 2000);
    	Future<Collection<Tuple>> testRead = this.readAll(motif, 3000);
		
    	lock.lock();
		callbackFinished.await();
    	lock.unlock();

		assertTrue(callback.hasHandleBeenCalled());
		assertTrue(callback.isSuccessful());
		assertFalse(testRead.get().contains(doubleIntegerTuple));
		assertTrue(testRead.get().contains(extraTuple));
	}

	@Test
	public void testCallbackReadImmediate() throws InterruptedException, ExecutionException {
		Tuple extraTuple = new Tuple(14, 15);
		
        Tuple motif = new Tuple(Integer.class, Integer.class);
        FakeCallback callback = new FakeCallback(doubleIntegerTuple);
		this.insertData(Arrays.asList(testData), 0);
		this.eventRegister(eventMode.READ, eventTiming.IMMEDIATE, motif, callback, 1000);
		this.insertData(Arrays.asList(extraTuple), 2000);
    	Future<Collection<Tuple>> testRead = this.readAll(motif, 3000);

    	lock.lock();
		callbackFinished.await();
    	lock.unlock();

		assertTrue(callback.hasHandleBeenCalled());
		assertTrue(callback.isSuccessful());
		assertTrue(testRead.get().contains(doubleIntegerTuple));
		assertTrue(testRead.get().contains(extraTuple));
	}

	public static class FakeCallback implements Callback {
		private Tuple expectedTuple;
	    private boolean wasSuccessful;
	    private boolean handleCalled;
	    
	    public FakeCallback(Tuple t) {
	    	this.expectedTuple = t;
	    }

		@Override
		public void call(Tuple t) {
	        System.out.println("(8) callback: " + t);
	        this.handleCalled = true;
	        this.wasSuccessful = this.expectedTuple.matches(t);
	    	lock.lock();
	        callbackFinished.signalAll();
	    	lock.unlock();
		}
		
		public boolean isSuccessful() {
			return this.wasSuccessful;
		}
		
		public boolean hasHandleBeenCalled() {
			return this.handleCalled;
		}
	}
	
	private Future<Boolean> insertData(List<Tuple> tuples, int sleepTime) {
    	Callable<Boolean> taskInsertData = new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
				for (Tuple tuple : tuples) {
			        System.out.println("(0) write: " + tuple);
			        linda.write(tuple);
				}
		        linda.debug("(0)");
		        return true;
			}
    	};
        return this.executor.submit(taskInsertData);
	}
	
	private Future<Tuple> take(Tuple motif, int sleepTime) {
    	Callable<Tuple> taskTake = new Callable<Tuple>() {
			@Override
			public Tuple call() throws Exception {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
		        System.out.println("(1) take: " + motif);
		        return linda.take(motif);
			}
    	};
        return this.executor.submit(taskTake);
	}
	
	private Future<Tuple> read(Tuple motif, int sleepTime) {
    	Callable<Tuple> taskRead = new Callable<Tuple>() {
			@Override
			public Tuple call() throws Exception {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
		        System.out.println("(2) read: " + motif);
		        return linda.read(motif);
			}
    	};
        return this.executor.submit(taskRead);
	}
	
	private Future<Tuple> tryTake(Tuple motif, int sleepTime) {
    	Callable<Tuple> taskTryTake = new Callable<Tuple>() {
			@Override
			public Tuple call() throws Exception {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
		        System.out.println("(3) tryTake: " + motif);
		        return linda.tryTake(motif);
			}
    	};
        return this.executor.submit(taskTryTake);
	}
	
	private Future<Tuple> tryRead(Tuple motif, int sleepTime) {
    	Callable<Tuple> taskTryRead = new Callable<Tuple>() {
			@Override
			public Tuple call() throws Exception {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
		        System.out.println("(4) tryRead: " + motif);
		        return linda.tryRead(motif);
			}
    	};
        return this.executor.submit(taskTryRead);
	}
	
	private Future<Collection<Tuple>> takeAll(Tuple motif, int sleepTime) {
    	Callable<Collection<Tuple>> taskTakeAll = new Callable<Collection<Tuple>>() {
			@Override
			public Collection<Tuple> call() throws Exception {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
		        System.out.println("(5) takeAll: " + motif);
		        return linda.takeAll(motif);
			}
    	};
        return this.executor.submit(taskTakeAll);
	}
	
	private Future<Collection<Tuple>> readAll(Tuple motif, int sleepTime) {
    	Callable<Collection<Tuple>> taskTryRead = new Callable<Collection<Tuple>>() {
			@Override
			public Collection<Tuple> call() throws Exception {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
		        System.out.println("(6) readAll: " + motif);
		        return linda.readAll(motif);
			}
    	};
        return this.executor.submit(taskTryRead);
	}
	
	private Future<Boolean> eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback, int sleepTime) {
    	Callable<Boolean> taskTryRead = new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
		        System.out.println("(7) eventRegister: using " + template + " in mode " + mode + " and timing " + timing);
		        linda.eventRegister(mode, timing, template, callback);
		        return true;
			}
    	};
        return this.executor.submit(taskTryRead);
	}
}
