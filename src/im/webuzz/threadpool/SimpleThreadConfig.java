package im.webuzz.threadpool;

import java.util.Properties;

public class SimpleThreadConfig {

	public static ThreadPoolExecutorConfig simpleWorkerPool = new ThreadPoolExecutorConfig();
	
	public static void update(Properties props) {
		SimpleThreadPool.updatePoolConfigurations();
	}
	
}
