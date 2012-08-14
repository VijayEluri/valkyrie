package com.othersonline.kv.test.backends;

import com.othersonline.kv.KeyValueStoreStatus;
import com.othersonline.kv.backends.CachingKeyValueStore;
import com.othersonline.kv.backends.ConcurrentHashMapKeyValueStore;
import com.othersonline.kv.backends.MemcachedKeyValueStore;
import com.othersonline.kv.test.KeyValueStoreBackendTestCase;

public class CachingStoreTestCase extends KeyValueStoreBackendTestCase {

	public void testBackend() throws Exception {
		ConcurrentHashMapKeyValueStore master = new ConcurrentHashMapKeyValueStore();
		master.start();

		MemcachedKeyValueStore cache = new MemcachedKeyValueStore();
		cache.setHosts("dev-db:11211");
		cache.start();

		CachingKeyValueStore store = new CachingKeyValueStore(master, cache);
		doTestBackend(store);

		// set cache to offline - should still pass
		cache.setStatus(KeyValueStoreStatus.Offline);
		store.setStatus(KeyValueStoreStatus.Offline);
		doTestBackend(store);
	}

}
