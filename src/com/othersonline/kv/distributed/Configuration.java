package com.othersonline.kv.distributed;

import java.util.concurrent.TimeUnit;

public class Configuration {
	// r: minimum # of nodes that must participate in a successful read
	private int requiredReads = 2;

	// w: minimum # of nodes that must participate in a successful write
	private int requiredWrites = 2;

	// n: number of replicas. r + w should be > n
	private int replicas = 3;

	// max wait time for reads
	private long readOperationTimeout = 1000l;

	// max wait time for writes
	private long writeOperationTimeout = 1000l;

	// max errors in a given period before a node is evicted
	private int maxErrorCount = 10;

	// time period for above error count
	private TimeUnit errorCountPeriod = TimeUnit.MINUTES;

	public int getRequiredReads() {
		return requiredReads;
	}

	public void setRequiredReads(int requiredReads) {
		this.requiredReads = requiredReads;
	}

	public int getRequiredWrites() {
		return requiredWrites;
	}

	public void setRequiredWrites(int requiredWrites) {
		this.requiredWrites = requiredWrites;
	}

	public int getReplicas() {
		return replicas;
	}

	public void setReplicas(int replicas) {
		this.replicas = replicas;
	}

	public long getReadOperationTimeout() {
		return readOperationTimeout;
	}

	public void setReadOperationTimeout(long readOperationTimeout) {
		this.readOperationTimeout = readOperationTimeout;
	}

	public long getWriteOperationTimeout() {
		return writeOperationTimeout;
	}

	public void setWriteOperationTimeout(long writeOperationTimeout) {
		this.writeOperationTimeout = writeOperationTimeout;
	}

	public int getMaxErrorCount() {
		return maxErrorCount;
	}

	public void setMaxErrorCount(int maxErrorCount) {
		this.maxErrorCount = maxErrorCount;
	}

	public TimeUnit getErrorCountPeriod() {
		return errorCountPeriod;
	}

	public void setErrorCountPeriod(TimeUnit errorCountPeriod) {
		this.errorCountPeriod = errorCountPeriod;
	}

}