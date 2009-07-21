package com.othersonline.kv.distributed.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.othersonline.kv.distributed.Configuration;
import com.othersonline.kv.distributed.Context;
import com.othersonline.kv.distributed.ContextFilter;
import com.othersonline.kv.distributed.ContextFilterResult;
import com.othersonline.kv.distributed.DistributedKeyValueStoreException;
import com.othersonline.kv.distributed.Node;
import com.othersonline.kv.distributed.Operation;
import com.othersonline.kv.distributed.OperationStatus;

/**
 * A simple context filter that orders results by node rank from the preference
 * list. Other nodes that provide a null value will be updated with this value.
 * 
 * @author Sam Tingleff <sam@tingleff.com>
 * 
 * @param <V>
 */
public class NodeRankContextFilter<V> implements ContextFilter<V> {
	private Configuration config;

	private Comparator<Context<V>> comparator = new NodeRankComparator<V>();

	public NodeRankContextFilter(Configuration config) {
		this.config = config;
	}

	public ContextFilterResult<V> filter(List<Context<V>> contexts)
			throws DistributedKeyValueStoreException {
		Collections.sort(contexts, comparator);

		// Select the first non-null value and call set() on any null nodes.
		Context<V> lowestNonNullValueContext = null;

		List<Node> nodesRequiringUpdate = new LinkedList<Node>();
		for (Context<V> context : contexts) {
			if (lowestNonNullValueContext == null)
				lowestNonNullValueContext = context;
			if ((lowestNonNullValueContext.getValue() == null)
					&& (context.getValue() != null)) {
				lowestNonNullValueContext = context;
			}
		}
		if (lowestNonNullValueContext.getValue() != null) {
			for (Context<V> context : contexts) {
				if (context.getValue() == null) {
					OperationStatus status = context.getResult().getStatus();
					if ((status.equals(OperationStatus.NullValue))
							&& (config.getFillNullGetResults()))
						nodesRequiringUpdate.add(context.getSourceNode());
					else if ((OperationStatus.Error.equals(status))
							&& (config.getFillErrorGetResults()))
						nodesRequiringUpdate.add(context.getSourceNode());
				}
			}
		}

		List<Operation<V>> ops = null;
		if ((lowestNonNullValueContext != null)
				&& (nodesRequiringUpdate.size() > 0)) {
			ops = new LinkedList<Operation<V>>();
			for (Node node : nodesRequiringUpdate) {
				Operation<V> op = new SetOperation<V>(null,
						lowestNonNullValueContext.getKey(),
						lowestNonNullValueContext.getValue());
				op.setNode(node);
				ops.add(op);
			}
		}
		return new DefaultContextFilterResult<V>(lowestNonNullValueContext, ops);
	}

	private static class NodeRankComparator<V> implements Comparator<Context<V>> {
		public int compare(Context<V> o1, Context<V> o2) {
			return new Integer(o1.getNodeRank()).compareTo(new Integer(o2
					.getNodeRank()));
		}
	}
}
