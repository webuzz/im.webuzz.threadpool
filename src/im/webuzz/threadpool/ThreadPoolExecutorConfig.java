package im.webuzz.threadpool;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorConfig {
	
	/**
	 * Core thread number. Core threads will be kept in thread pool to
	 * make server more responsible.
	 */
	public int coreThreads = 20;

	/**
	 * Max thread number. Server will allow this number of threads at the
	 * peak. Default number is 128. If set to -1, if there is no limit.
	 */
	public int maxThreads = 128;

	/**
	 * Idle thread number. Server will keep this number of idle threads if
	 * possible. Default number is 10.
	 */
	public int idleThreads = 10;

	/**
	 * If a thread is idle for given seconds, and thread number is greater
	 * than maxThreads, this thread will be recycled.
	 */
	public long threadIdleSeconds = 60L;
	
	/**
	 * Allow thread to time out or not.
	 */
	public boolean threadTimeout = false;

	/**
	 * Queue task number. Server will keep this number of tasks waiting in
	 * Queue. Default is 100.
	 */
	public int queueTasks = 100;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + coreThreads;
		result = prime * result + idleThreads;
		result = prime * result + maxThreads;
		result = prime * result + queueTasks;
		result = prime * result + (int) (threadIdleSeconds ^ (threadIdleSeconds >>> 32));
		result = prime * result + (threadTimeout ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ThreadPoolExecutorConfig other = (ThreadPoolExecutorConfig) obj;
		if (coreThreads != other.coreThreads)
			return false;
		if (idleThreads != other.idleThreads)
			return false;
		if (maxThreads != other.maxThreads)
			return false;
		if (queueTasks != other.queueTasks)
			return false;
		if (threadIdleSeconds != other.threadIdleSeconds)
			return false;
		if (threadTimeout != other.threadTimeout)
			return false;
		return true;
	}

	public void updatePoolWithComparison(ThreadPoolExecutor pool, ThreadPoolExecutorConfig lastConfig) {
		if (lastConfig.coreThreads != coreThreads) {
			int core = coreThreads;
			if (core <= 0) {
				core = 1;
			}
			pool.setCorePoolSize(core);
		}
		if (lastConfig.maxThreads != maxThreads) {
			int max = maxThreads;
			if (max <= 0) {
				max = Integer.MAX_VALUE;
			}
			pool.setMaximumPoolSize(max);
		}
		if (pool instanceof SimpleThreadPoolExecutor) {
			if (lastConfig.idleThreads != idleThreads) {
				int idle = idleThreads;
				if (idle < 0) {
					idle = 0;
				}
				((SimpleThreadPoolExecutor) pool).setIdlePoolSize(idle);
			}
			if (lastConfig.queueTasks != queueTasks) {
				int queue = queueTasks;
				if (queue <= 0) {
					queue = 1;
				}
				((SimpleThreadPoolExecutor) pool).setQueueSize(queue);
			}
		}
		if (lastConfig.threadIdleSeconds != threadIdleSeconds) {
			long idle = threadIdleSeconds;
			if (idle <= 0) {
				idle = 1; // 1 second
			}
			pool.setKeepAliveTime(idle, TimeUnit.SECONDS);
		}
		if (lastConfig.threadTimeout != threadTimeout) {
			pool.allowCoreThreadTimeOut(threadTimeout);
		}
	}
	
}
