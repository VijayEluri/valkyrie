package com.othersonline.kv.backends;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.othersonline.kv.KeyValueStore;
import com.othersonline.kv.KeyValueStoreException;
import com.othersonline.kv.BaseManagedKeyValueStore;
import com.othersonline.kv.transcoder.Transcoder;

public class ConcurrentHashMapKeyValueStore extends BaseManagedKeyValueStore
		implements KeyValueStore {
	public static final String IDENTIFIER = "hashtable";

	private Map<String, Object> map = new ConcurrentHashMap<String, Object>();

	public String getIdentifier() {
		return IDENTIFIER;
	}

	public void stop() {
		map.clear();
		super.stop();
	}

	public boolean exists(String key) throws KeyValueStoreException,
			IOException {
		assertReadable();
		return map.containsKey(key);
	}

	public Object get(String key) throws KeyValueStoreException, IOException {
		assertReadable();
		return map.get(key);
	}

	public Object get(String key, Transcoder transcoder)
			throws KeyValueStoreException, IOException {
		assertReadable();
		return map.get(key);
	}

	public Map<String, Object> getBulk(String... keys)
			throws KeyValueStoreException, IOException {
		assertReadable();
		Map<String, Object> results = new HashMap<String, Object>();
		for (String key : keys) {
			Object obj = get(key);
			if (obj != null)
				results.put(key, obj);
		}
		return results;
	}

	public Map<String, Object> getBulk(final List<String> keys)
			throws KeyValueStoreException, IOException {
		assertReadable();
		Map<String, Object> results = new HashMap<String, Object>();
		for (String key : keys) {
			Object obj = get(key);
			if (obj != null)
				results.put(key, obj);
		}
		return results;
	}

	public Map<String, Object> getBulk(final List<String> keys,
			Transcoder transcoder) throws KeyValueStoreException, IOException {
		assertReadable();
		Map<String, Object> results = new HashMap<String, Object>();
		for (String key : keys) {
			Object obj = get(key, transcoder);
			if (obj != null)
				results.put(key, obj);
		}
		return results;
	}

	public void set(String key, Object value) throws KeyValueStoreException,
			IOException {
		assertWriteable();
		map.put(key, value);
	}

	public void set(String key, Object value, Transcoder transcoder)
			throws KeyValueStoreException, IOException {
		assertWriteable();
		map.put(key, value);
	}

	public void delete(String key) throws KeyValueStoreException, IOException {
		assertWriteable();
		map.remove(key);
	}

}