package im.webuzz.threadpool;

import java.util.Properties;

public class SimpleThreadPool {

	public static final String configKeyPrefix = "simplepool";
	
	public static ThreadPoolExecutorConfig pool = new ThreadPoolExecutorConfig();
	
	private static ThreadPoolExecutorConfig lastConfig = pool;
	private static SimpleThreadPoolExecutor poolExecutor;
	private static boolean poolInitialized = false;
	
	public static void update(Properties props) {
		if (!poolInitialized || poolExecutor == null) return;
		ThreadPoolExecutorConfig sc = pool;
		if (sc == null) return;
		sc.updatePoolWithComparison(poolExecutor, lastConfig);
		lastConfig = sc;
	}

	public static void initializePool() {
		if (poolInitialized) return;
		synchronized (SimpleThreadPool.class) {
			if (poolInitialized) return;
			lastConfig = pool;
			if (lastConfig == null) {
				lastConfig = new ThreadPoolExecutorConfig();
				lastConfig.workerName = "Simple Worker";
			}
			poolExecutor = new SimpleThreadPoolExecutor(lastConfig);
			poolExecutor.allowCoreThreadTimeOut(lastConfig.threadTimeout);
			poolInitialized = true;
		}
	}

	public static SimpleThreadPoolExecutor getPoolExecutor() {
		initializePool();
		return poolExecutor;
	}
	
	public static void execute(Runnable task) {
		initializePool();
		try {
			poolExecutor.execute(task);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
