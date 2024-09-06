package im.webuzz.example;

import java.util.Properties;

import im.webuzz.threadpool.SimpleThreadPoolExecutor;
import im.webuzz.threadpool.ThreadPoolExecutorConfig;

/*
 * Example of stand-alone configurable thread pool.
 * To customize your own configurable thread pool:
 * 1. Rename class name ExampleThreadPool to your thread pool name
 * 2. Rename configKeyPrefix "examplepool" to a new name, which will result in ####.ini configuration file
 * 3. [Optional] Refactor static field "pool" to a new field name, if necessary
 */
public class ExampleThreadPool {

	public static final String configKeyPrefix = "examplepool"; // examplepool.ini

	/* To be configurable */
	public static ThreadPoolExecutorConfig pool = new ThreadPoolExecutorConfig();
	
	private static SimpleThreadPoolExecutor poolExecutor;
	private static boolean poolInitialized = false;
	private static ThreadPoolExecutorConfig lastConfig = pool;

	/* Callback of SimpleConfig */
	public static void update(Properties props) {
		if (!poolInitialized || poolExecutor == null) return;
		ThreadPoolExecutorConfig sc = pool;
		if (sc == null) return;
		sc.updatePoolWithComparison(poolExecutor, lastConfig);
		lastConfig = sc;
	}

	public static void initializePool() {
		if (poolInitialized) return;
		synchronized (ExampleThreadPool.class) {
			if (poolInitialized) return;
			lastConfig = pool;
			if (lastConfig == null) {
				lastConfig = new ThreadPoolExecutorConfig();
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
		poolExecutor.execute(task);
	}
}
