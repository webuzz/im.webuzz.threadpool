/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.threadpool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadFactory;

/**
 * A Thread factory so we can tell what are those threads for.
 * 
 * @author zhourenjian
 *
 */
public class SimpleNamedThreadFactory implements ThreadFactory {

	final AtomicInteger threadNumber = new AtomicInteger(1);
	protected String namePrefix;

	public SimpleNamedThreadFactory(String prefix) {
		updatePrefix(prefix);
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(null, r, namePrefix + threadNumber.getAndIncrement(), 0);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}

	public void updatePrefix(String prefix) {
		if (prefix == null || prefix.length() == 0) {
			namePrefix = "Thread-";
		} else {
			namePrefix = prefix + "-";
		}
	}
}
