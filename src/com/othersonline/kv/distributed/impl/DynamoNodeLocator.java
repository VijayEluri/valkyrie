package com.othersonline.kv.distributed.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.othersonline.kv.distributed.HashAlgorithm;
import com.othersonline.kv.distributed.Node;
import com.othersonline.kv.distributed.NodeChangeListener;
import com.othersonline.kv.distributed.NodeLocator;
import com.othersonline.kv.tuple.Tuple;
import com.othersonline.kv.tuple.Tuple2;

/**
 * A node locator roughly comparable to Strategy 3 from the dynamo paper.
 * 
 * @author sam
 *
 */
public class DynamoNodeLocator implements NodeLocator, NodeChangeListener {
	public static final int DEFAULT_TOKENS_PER_NODE = 100;

	private int tokensPerNode = DEFAULT_TOKENS_PER_NODE;

	private HashAlgorithm md5 = new MD5HashAlgorithm();

	private volatile HashRing<Long, Token> outerRing;

	public DynamoNodeLocator(int tokensPerNode) {
		this.tokensPerNode = tokensPerNode;
	}

	public DynamoNodeLocator() {
	}

	public List<Node> getPreferenceList(HashAlgorithm hashAlg, String key,
			int count) {
		if (count > outerRing.getNodeCount())
			throw new IllegalArgumentException(String.format(
					"Requested count (%1$d) is greater than node count (%2$d)",
					count, outerRing.getNodeCount()));

		long hashCode = hashAlg.hash(key);

		List<Node> results = new ArrayList<Node>(count);
		Map.Entry<Long, Token> entry = outerRing.place(hashCode);
		results.add(entry.getValue().node);

		while (results.size() < count) {
			entry = outerRing.lowerEntry(hashCode);
			if (entry == null)
				entry = outerRing.lastEntry();
			Node n = entry.getValue().node;
			if (!results.contains(n))
				results.add(n);
			hashCode = entry.getKey();
		}
		return results;
	}

	/**
	 * Return the primary token for a given key. Provided for unit testing.
	 * 
	 * @param hashAlg
	 * @param key
	 * @return
	 */
	public int getPrimaryNode(HashAlgorithm hashAlg, String key) {
		if (outerRing.getNodeCount() == 0)
			throw new IllegalArgumentException("Ring is currently empty");
		long hashCode = hashAlg.hash(key);
		Map.Entry<Long, Token> entry = outerRing.place(hashCode);
		return entry.getValue().id;

	}

	/**
	 * Callback from the node store when the active node list has changed.
	 * 
	 * @param nodes
	 */
	public void setActiveNodes(List<Node> nodes) {
		rebuild(nodes);
	}

	private void rebuild(List<Node> nodes) {
		HashRing<Long, Token> newRing = new HashRing<Long, Token>(nodes.size());

		// build the outer ring from Long.MIN_VALUE to Long.MAX_VALUE
		int tokenCount = tokensPerNode * newRing.getNodeCount();
		long tokenSize = (Long.MAX_VALUE / tokenCount) * 2;
		for (int i = 1; i <= tokenCount; ++i) {
			long index = Long.MIN_VALUE + (i * tokenSize);
			Token token = new Token(i - 1, null);
			newRing.put(index, token);
		}
		List<Tuple2<Long, Node>> tokens = new ArrayList<Tuple2<Long, Node>>(
				newRing.size());
		for (Node node : nodes) {
			for (int i = 1; i <= tokensPerNode; ++i) {
				// assign T tokens to this node
				String identifier = node.getSalt() + i;
				long hashCode = md5.hash(identifier);
				tokens.add(Tuple2.from(new Long(hashCode), node));
			}
		}
		Collections.sort(tokens, new Comparator<Tuple2<Long, Node>>() {
			public int compare(Tuple2<Long, Node> o1, Tuple2<Long, Node> o2) {
				return Tuple.get1(o1).compareTo(Tuple.get1(o2));
			}
		});
		int i = 0;
		for (Token token : newRing.values()) {
			token.node = Tuple.get2(tokens.get(i));
			++i;
		}
		outerRing = newRing;
	}

	private static class Token {
		private int id;

		private Node node;

		public Token(int id, Node node) {
			this.id = id;
			this.node = node;
		}
	}
}
