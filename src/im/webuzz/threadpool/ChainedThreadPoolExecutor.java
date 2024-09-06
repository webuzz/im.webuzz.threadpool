package im.webuzz.threadpool;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.concurrent.*;
import java.util.*;

public class ChainedThreadPoolExecutor extends SimpleThreadPoolExecutor {

	public static boolean peekingPoolStatus = false;
	
	private ReentrantLock internalMainLock = null;
	
	private Set<Runnable> internalWorkers = null;
	
	private Field fieldWorkerThread = null;

	private Field fieldWorkerFirstTask = null;

	private Map<Runnable, ChainedRunnable> currentTasks = new ConcurrentHashMap<Runnable, ChainedRunnable>();
	
	private Map<Object, ChainedRunnable> lastTasks = new ConcurrentHashMap<Object, ChainedRunnable>();
	
	@SuppressWarnings("unchecked")
	private void fetchInternalFields() {
		try {
			Field fieldWorkers = ThreadPoolExecutor.class.getDeclaredField("workers");
			if (fieldWorkers != null) {
				fieldWorkers.setAccessible(true);
				Object value = fieldWorkers.get(this);
				if (value instanceof Set) {
					internalWorkers = (Set<Runnable>) value;
				}
			}
			Field fieldMainLock = ThreadPoolExecutor.class.getDeclaredField("mainLock");
			if (fieldMainLock != null) {
				fieldMainLock.setAccessible(true);
				Object value = fieldMainLock.get(this);
				if (value instanceof ReentrantLock) {
					internalMainLock = (ReentrantLock) value;
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
			if (e.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
				System.err.println("For Java 9+, please add VM arguments \"--add-opens java.base/java.util.concurrent=ALL-UNNAMED\" and run again.");
				System.exit(1);
			}
		}
	}
	
	public ChainedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int idlePoolSize, long keepAliveTime,
			TimeUnit unit, int queueSize, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, idlePoolSize, keepAliveTime, unit, queueSize, handler);
		fetchInternalFields();
	}
	public ChainedThreadPoolExecutor(ThreadPoolExecutorConfig config, RejectedExecutionHandler handler) {
		this(config.coreThreads, config.maxThreads, config.idleThreads,
				config.threadIdleSeconds, TimeUnit.SECONDS, config.queueTasks,
				new SimpleNamedThreadFactory(config.workerName),
				handler);
	}

	public ChainedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int idlePoolSize, long keepAliveTime,
			TimeUnit unit, int queueSize, String poolName) {
		super(corePoolSize, maximumPoolSize, idlePoolSize, keepAliveTime, unit, queueSize, poolName);
		fetchInternalFields();
	}

	public ChainedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int idlePoolSize, long keepAliveTime,
			TimeUnit unit, int queueSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, idlePoolSize, keepAliveTime, unit, queueSize, threadFactory, handler);
		fetchInternalFields();
	}
	public ChainedThreadPoolExecutor(ThreadPoolExecutorConfig config, ThreadFactory threadFactory,
			RejectedExecutionHandler handler) {
		this(config.coreThreads, config.maxThreads, config.idleThreads,
				config.threadIdleSeconds, TimeUnit.SECONDS, config.queueTasks,
				threadFactory, handler);
		if (threadFactory instanceof SimpleNamedThreadFactory) {
			((SimpleNamedThreadFactory) threadFactory).updatePrefix(config.workerName);
		}
	}

	public ChainedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int idlePoolSize, long keepAliveTime,
			TimeUnit unit, int queueSize, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, idlePoolSize, keepAliveTime, unit, queueSize, threadFactory);
		fetchInternalFields();
	}
	public ChainedThreadPoolExecutor(ThreadPoolExecutorConfig config, ThreadFactory threadFactory) {
		this(config.coreThreads, config.maxThreads, config.idleThreads,
				config.threadIdleSeconds, TimeUnit.SECONDS, config.queueTasks,
				threadFactory);
		if (threadFactory instanceof SimpleNamedThreadFactory) {
			((SimpleNamedThreadFactory) threadFactory).updatePrefix(config.workerName);
		}
	}

	public ChainedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int idlePoolSize, long keepAliveTime,
			TimeUnit unit, int queueSize) {
		super(corePoolSize, maximumPoolSize, idlePoolSize, keepAliveTime, unit, queueSize);
		fetchInternalFields();
	}
	public ChainedThreadPoolExecutor(ThreadPoolExecutorConfig config) {
		this(config.coreThreads, config.maxThreads, config.idleThreads,
				config.threadIdleSeconds, TimeUnit.SECONDS, config.queueTasks, config.workerName);
	}
	
	private boolean addIfInQueue(ChainedRunnable task) {
		Object owner = task.getOwner();
		final ReentrantLock mainLock = this.internalMainLock;
		mainLock.lock();
		try {
			//System.out.println("Worker size = " + internalWorkers.size());
			for (Iterator<Runnable> itr = internalWorkers.iterator(); itr.hasNext();) {
				Runnable worker = (Runnable) itr.next();
				ChainedRunnable runningTask = currentTasks.get(worker);
				if (runningTask != null && runningTask.getOwner() == owner) {
					//System.out.println("Appending task " + task + " to running task " + runningTask + " on worker " + worker);
					if (runningTask.addNext(task)) {
						lastTasks.put(owner, task);
						return true;
					} else { // runningTask just finished, continue to execute
						return false;
					}
				}
				ChainedRunnable firstTask = null; // worker.firstTask;
				try {
					if (fieldWorkerFirstTask == null) {
						Field f = worker.getClass().getDeclaredField("firstTask");
						if (f != null) {
							f.setAccessible(true);
						}
						fieldWorkerFirstTask = f;
					}
					if (fieldWorkerFirstTask != null) {
						Object value = fieldWorkerFirstTask.get(worker);
						if (value instanceof ChainedRunnable) {
							firstTask = (ChainedRunnable) value;
						}
						//System.out.println("Checking firstTask " + value + " for worker " + worker);
					}
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				if (firstTask != null && firstTask.getOwner() == owner) {
					//System.out.println("Appending task " + task + " to first task " + runningTask + " on worker " + worker);
					if (firstTask.addNext(task)) {
						lastTasks.put(owner, task);
						return true;
					} else { // runningTask just finished, continue to execute
						return false;
					}
				}
			}
			for (Iterator<Runnable> itr = getQueue().iterator(); itr.hasNext();) {
				Runnable next = itr.next();
				if (next instanceof ChainedRunnable) {
					ChainedRunnable r = (ChainedRunnable) next;
					if (r.getOwner() == owner) {
						//System.out.println("Appending task " + task + " to queued task " + r);
						if (r.addNext(task)) {
							lastTasks.put(owner, task);
							return true;
						} else { // runningTask just finished, continue to execute
							return false;
						}
					}
				}
			}
			ChainedRunnable last = lastTasks.get(owner);
			if (last != null && !last.isDone()) {
				//System.out.println("Appending task " + task + " to last task " + last);
				if (last.addNext(task)) {
					lastTasks.put(owner, task);
					return true;
				} else { // runningTask just finished, continue to execute
					return false;
				}
			}
			
			//System.out.println("Not in queue, starting new worker for " + task);
			lastTasks.put(owner, task);
			return false;
		} finally {
			mainLock.unlock();
		}
	}

	public void execute(Object owner, Runnable command) {
		ChainedRunnable task = new ChainedRunnable(owner, command);
		execute(task);
	}

	@Override
	public void execute(Runnable command) {
		if (command == null)
			throw new NullPointerException();
		if (!(command instanceof ChainedRunnable)) {
			throw new RuntimeException("Not a chained runnable task");
		}
		ChainedRunnable chainCommand = (ChainedRunnable) command;
		if (addIfInQueue(chainCommand)) {
			return;
		}
		super.execute(command);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		final ReentrantLock mainLock = this.internalMainLock;
		boolean removeError = false;
		mainLock.lock();
		try {
			for (Map.Entry<Runnable, ChainedRunnable> entry : currentTasks.entrySet()) {
				if (entry.getValue() == r) {
					currentTasks.remove(entry.getKey());
					break;
				}
			}
			if (r instanceof ChainedRunnable) {
				ChainedRunnable task = (ChainedRunnable) r;
				Object owner = task.getOwner();
				ChainedRunnable lastTask = lastTasks.get(owner);
				ChainedRunnable lastRun = task.getLastRun();
				if (lastTask == lastRun) {
					task.reset();
					ChainedRunnable last = lastTasks.remove(owner);
					removeError = last != lastRun;
				}
			}
		} finally {
			mainLock.unlock();
		}
		if (removeError) {
			System.out.println("Removed updated last task " + r);
		}
		if (peekingPoolStatus) {
			System.out.println("Current tasks size: " + currentTasks.size() + ", last tasks size: " + lastTasks.size());
		}
		super.afterExecute(r, t);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		final ReentrantLock mainLock = this.internalMainLock;
		mainLock.lock();
		try {
			for (Runnable worker : internalWorkers) {
				try {
					if (fieldWorkerThread == null) {
						Field f = worker.getClass().getDeclaredField("thread");
						if (f != null) {
							f.setAccessible(true);
						}
						fieldWorkerThread = f;
					}
					if (fieldWorkerThread != null) {
						Object value = fieldWorkerThread.get(worker);
						if (t == value) {
							if (r instanceof ChainedRunnable) {
								currentTasks.put(worker, (ChainedRunnable) r);
								break;
							} // else dummy task
						}
					}
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		} finally {
			mainLock.unlock();
		}
		super.beforeExecute(t, r);
	}
	
}
