package im.webuzz.threadpool;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class ChainedRunnable implements Runnable {

	private Runnable task;
	
	private volatile ChainedRunnable next;
	
	private volatile ChainedRunnable lastRun;
	
	private Object owner;

	private volatile boolean done;
	
	private volatile ReentrantLock lock;
	
	public ChainedRunnable(Object owner, Runnable task) {
		super();
		this.owner = owner;
		this.task = task;
		lastRun = null;
		done = false;
	}
	
	public void runTask() {
		if (task != null) {
			task.run();
			task = null;
		}
	}

	public void run() {
		lastRun = runWithRoot(null);
	}
	
	public ChainedRunnable runWithRoot(ChainedRunnable root) {
		runTask();
		// May run into stack overflow!
		//if (next != null) {
		//	next.run();
		//}
		ChainedRunnable last = this;
		ChainedRunnable n = next;
		Queue<ChainedRunnable> queue = new LinkedList<ChainedRunnable>();
		while (n != null) {
			queue.add(n);
			n.runTask();
			last = n;
			n = n.next;
		}
		ChainedRunnable finalTask = null;
		if (root == null) {
			root = this;
		}
		synchronized (root) {
			ReentrantLock lock = this.lock;
			if (lock != null) {
				lock.lock();
			}
			// mark task done one by one
			done = true;
			while (!queue.isEmpty()) {
				ChainedRunnable r = queue.remove();
				if (r != null) r.done = true;
			}
			if (last != null && last.next != null) {
				// new task has been just added!
				finalTask = last.next;
			}
			if (lock != null) {
				lock.unlock();
			}
		}
		if (finalTask != null) {
			return finalTask.runWithRoot(root);
		} else {
			return last;
		}
	}
	
	public boolean isDone() {
		return done;
	}

	public ChainedRunnable getNext() {
		return next;
	}
	
	public Runnable getTask() {
		return task;
	}

	public ChainedRunnable getLastRun() {
		return lastRun;
	}
	
	public void reset() {
		// set to null to avoid memory leak
		lastRun = null;
		owner = null;
		next = null;
	}
	
	// Not thread safe!
	public boolean addNext(ChainedRunnable task) {
		if (lock == null) {
			synchronized (this) {
				if (lock == null) {
					lock = new ReentrantLock();
				}
			}
		}
		ChainedRunnable oThis = this;
		lock.lock();
		while (!oThis.done) {
			if (oThis.next == null) {
				task.lock = oThis.lock;
				oThis.next = task;
				lock.unlock();
				return true;
			}
			oThis = oThis.next;
		}
		lock.unlock();
		return false;
	}

	public Object getOwner() {
		return owner;
	}
	
}
