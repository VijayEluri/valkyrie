package com.othersonline.kv.distributed;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import com.othersonline.kv.backends.ConnectionFactory;

public interface OperationQueue {
	public void setConnectionFactory(ConnectionFactory factory);

	public void start();

	public void stop();

	public int getQueueSize();

	public <V> Future<OperationResult<V>> submit(Operation<V> operation)
			throws RejectedExecutionException;
}
