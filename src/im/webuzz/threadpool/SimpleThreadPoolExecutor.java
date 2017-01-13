package im.webuzz.threadpool;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Only increasing threads on command and keep enough idle threads.
 */
public class SimpleThreadPoolExecutor extends ThreadPoolExecutor {

	static Runnable DUMMY_TASK = new Runnable() {
		
		public void run() {			
		}
		
	};
	
	public static class SimpleBlockingQueue extends LinkedBlockingQueue<Runnable> {

		private static final long serialVersionUID = -8871990009355540255L;

		private SimpleThreadPoolExecutor executor;
		
		public SimpleBlockingQueue() {
			super();
		}

		public SimpleBlockingQueue(Collection<? extends Runnable> c) {
			super(c);
		}

		public SimpleBlockingQueue(int capacity) {
			super(capacity);
		}

		public SimpleThreadPoolExecutor getExecutor() {
			return executor;
		}

		public void setExecutor(SimpleThreadPoolExecutor executor) {
			this.executor = executor;
		}

		@Override
		public boolean offer(Runnable e) {
			if (executor != null) {
				int poolSize = executor.getPoolSize();
				if (poolSize < executor.getMaximumPoolSize()) {
					if (poolSize - executor.runningTasks.get() < executor.getIdlePoolSize()) { // not enough idle threads
						// Thread pool executor should create new thread to run given task.
						return false;
					}
				}
				if (size() >= executor.getQueueSize()) {
					return false;
				}
			}
			return super.offer(e);
		}

		@Override
		public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
			if (executor != null) {
				if (timeout < 0) {
					return super.offer(e);
				}
				int poolSize = executor.getPoolSize();
				if (poolSize < executor.getMaximumPoolSize()) {
					if (poolSize - executor.runningTasks.get() < executor.getIdlePoolSize()) { // not enough idle threads
						return false;
					}
				}
				if (size() >= executor.getQueueSize()) {
					return false;
				}
			}
			return super.offer(e, timeout, unit);
		}
		
	}

    AtomicInteger runningTasks = new AtomicInteger(0);
    int idlePoolSize;
    int queueSize;
    
	public SimpleThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			int idlePoolSize, long keepAliveTime, TimeUnit unit,
			int queueSize, RejectedExecutionHandler handler) {
		super(Math.max(corePoolSize, 1),
				maximumPoolSize <= 0 ? Integer.MAX_VALUE : maximumPoolSize,
				Math.max(keepAliveTime, 1), unit,
				new SimpleBlockingQueue(), handler);
		this.idlePoolSize = Math.max(0, idlePoolSize);
		this.queueSize = Math.max(1, queueSize);
		((SimpleBlockingQueue) getQueue()).setExecutor(this);
	}
    public SimpleThreadPoolExecutor(ThreadPoolExecutorConfig config, RejectedExecutionHandler handler) {
		this(config.coreThreads, config.maxThreads, config.idleThreads,
				config.threadIdleSeconds, TimeUnit.SECONDS, config.queueTasks,
				new SimpleNamedThreadFactory(config.workerName),
				handler);
    }

	public SimpleThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			int idlePoolSize, long keepAliveTime, TimeUnit unit,
			int queueSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(Math.max(corePoolSize, 1),
				maximumPoolSize <= 0 ? Integer.MAX_VALUE : maximumPoolSize,
				Math.max(keepAliveTime, 1), unit,
				new SimpleBlockingQueue(),
				threadFactory, handler);
		this.idlePoolSize = Math.max(0, idlePoolSize);
		this.queueSize = Math.max(1, queueSize);
		((SimpleBlockingQueue) getQueue()).setExecutor(this);
	}
    public SimpleThreadPoolExecutor(ThreadPoolExecutorConfig config, ThreadFactory threadFactory,
    		RejectedExecutionHandler handler) {
		this(config.coreThreads, config.maxThreads, config.idleThreads,
				config.threadIdleSeconds, TimeUnit.SECONDS, config.queueTasks,
				threadFactory, handler);
		if (threadFactory instanceof SimpleNamedThreadFactory) {
			((SimpleNamedThreadFactory) threadFactory).updatePrefix(config.workerName);
		}
    }

	public SimpleThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			int idlePoolSize, long keepAliveTime, TimeUnit unit,
			int queueSize, ThreadFactory threadFactory) {
		super(Math.max(corePoolSize, 1),
				maximumPoolSize <= 0 ? Integer.MAX_VALUE : maximumPoolSize,
				Math.max(keepAliveTime, 1), unit,
				new SimpleBlockingQueue(),
				threadFactory);
		this.idlePoolSize = Math.max(0, idlePoolSize);
		this.queueSize = Math.max(1, queueSize);
		((SimpleBlockingQueue) getQueue()).setExecutor(this);
	}
    public SimpleThreadPoolExecutor(ThreadPoolExecutorConfig config, ThreadFactory threadFactory) {
		this(config.coreThreads, config.maxThreads, config.idleThreads,
				config.threadIdleSeconds, TimeUnit.SECONDS, config.queueTasks,
				threadFactory);
		if (threadFactory instanceof SimpleNamedThreadFactory) {
			((SimpleNamedThreadFactory) threadFactory).updatePrefix(config.workerName);
		}
    }

	public SimpleThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			int idlePoolSize, long keepAliveTime, TimeUnit unit, int queueSize) {
		super(Math.max(corePoolSize, 1),
				maximumPoolSize <= 0 ? Integer.MAX_VALUE : maximumPoolSize,
				Math.max(keepAliveTime, 1), unit,
				new SimpleBlockingQueue());
		this.idlePoolSize = Math.max(0, idlePoolSize);
		this.queueSize = Math.max(1, queueSize);
		((SimpleBlockingQueue) getQueue()).setExecutor(this);
	}
	public SimpleThreadPoolExecutor(ThreadPoolExecutorConfig config) {
		this(config.coreThreads, config.maxThreads, config.idleThreads,
				config.threadIdleSeconds, TimeUnit.SECONDS, config.queueTasks, config.workerName);
    }

	public SimpleThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			int idlePoolSize, long keepAliveTime, TimeUnit unit, int queueSize, String poolName) {
		super(Math.max(corePoolSize, 1),
				maximumPoolSize <= 0 ? Integer.MAX_VALUE : maximumPoolSize,
				Math.max(keepAliveTime, 1), unit,
				new SimpleBlockingQueue(),
				new SimpleNamedThreadFactory(poolName));
		this.idlePoolSize = Math.max(0, idlePoolSize);
		this.queueSize = Math.max(1, queueSize);
		((SimpleBlockingQueue) getQueue()).setExecutor(this);
	}

	public int getIdlePoolSize() {
		return idlePoolSize;
	}

	public void setIdlePoolSize(int idlePoolSize) {
		this.idlePoolSize = Math.max(0, idlePoolSize);
	}

	public int getQueueSize() {
		return queueSize;
	}
	
	public void setQueueSize(int queueSize) {
		this.queueSize = Math.max(1, queueSize);
	}
	
	public void updateWorkerName(String workerName) {
		ThreadFactory threadFactory = getThreadFactory();
		if (threadFactory instanceof SimpleNamedThreadFactory) {
			((SimpleNamedThreadFactory) threadFactory).updatePrefix(workerName);
		}
	}
	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		runningTasks.incrementAndGet();
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		runningTasks.decrementAndGet();
	}
	
	@Override
	public void execute(Runnable command) {
		int poolSize = getPoolSize();
		if (poolSize > 0) {
			int activeTasks = runningTasks.get();
			if (poolSize - activeTasks > idlePoolSize) { // has enough idle threads
				BlockingQueue<Runnable> q = getQueue();
				try {
					if (q.offer(command, -1, null)) { // -1: invoke #offer direct without refusing
						return;
					}
				} catch (InterruptedException e) {
				}
			} else if (0 < activeTasks && activeTasks < poolSize) {
				BlockingQueue<Runnable> q = getQueue();
				try {
					if (q.offer(command, -1, null)) { // -1: invoke #offer direct without refusing
						super.execute(DUMMY_TASK); // Increase an idle thread
						return;
					}
				} catch (InterruptedException e) {
				}
			}
		}
		super.execute(command);
	}
	
}