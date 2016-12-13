package im.webuzz.threadpool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ThreadPoolExecutor;

public class SimpleThreadPool {

	private static SimpleThreadPoolExecutor poolExecutor;

	private static boolean poolInitialized = false;
	
	private static ThreadPoolExecutorConfig lastConfig = SimpleThreadConfig.simpleWorkerPool;

	public static void initializePool() {
		if (poolInitialized) {
			return;
		}
		synchronized (SimpleThreadPool.class) {
			if (poolInitialized) {
				return;
			}
			lastConfig = SimpleThreadConfig.simpleWorkerPool;
			if (lastConfig == null) {
				lastConfig = new ThreadPoolExecutorConfig();
			}
			poolExecutor = new SimpleThreadPoolExecutor(lastConfig, "Simple Worker");
			poolExecutor.allowCoreThreadTimeOut(lastConfig.threadTimeout);
			poolInitialized = true;
		}
		
		if (poolInitialized && poolExecutor != null) {
			String baseClassName = "net.sf.j2s.ajax.SimpleThreadHelper";
			try {
				Class<?> sthClass = Class.forName(baseClassName);
				Method sPEMethod = sthClass.getMethod("setPoolExecutor", ThreadPoolExecutor.class);
				if (sPEMethod != null && (sPEMethod.getModifiers() & Modifier.STATIC) != 0) {
					sPEMethod.invoke(sthClass, poolExecutor);
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	public static void updatePoolConfigurations() {
		if (!poolInitialized || poolExecutor == null) {
			return;
		}
		ThreadPoolExecutorConfig sc = SimpleThreadConfig.simpleWorkerPool;
		if (sc == null) {
			return;
		}
		sc.updatePoolWithComparison(poolExecutor, lastConfig);
		lastConfig = sc;
	}

}
