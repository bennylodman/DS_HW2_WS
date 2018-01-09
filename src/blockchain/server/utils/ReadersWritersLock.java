package blockchain.server.utils;

public class ReadersWritersLock {
	private int numReaders = 0;
	private int numWriters = 0;
	private int numWaitedWriters = 0;

	public synchronized void acquireRead() {
		while (numWriters > 0 || numWaitedWriters > 0) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}
		numReaders++;
	}

	public synchronized void releaseRead() {
		numReaders--;
		notifyAll();
	}

	public synchronized void acquireWrite() {
		numWaitedWriters++;
		while (numWriters > 0 || numReaders > 0) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}
		numWriters++;
		numWaitedWriters--;
	}

	public synchronized void releaseWrite() {
		numWriters--;
		notifyAll();
	}

	public int getNumReaders() {
		return numReaders;
	}

	public int getNumWriters() {
		return numWriters;
	}
}
