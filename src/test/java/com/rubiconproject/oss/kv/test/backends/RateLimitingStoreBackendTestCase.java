package com.rubiconproject.oss.kv.test.backends;

import java.util.concurrent.TimeUnit;

import com.rubiconproject.oss.kv.KeyValueStoreUnavailable;
import com.rubiconproject.oss.kv.backends.MemcachedKeyValueStore;
import com.rubiconproject.oss.kv.backends.RateLimitingKeyValueStore;
import com.rubiconproject.oss.kv.test.KeyValueStoreBackendTestCase;
import com.rubiconproject.oss.kv.util.MemcachedRateLimiter;
import com.rubiconproject.oss.kv.util.RateLimiter;
import com.rubiconproject.oss.kv.util.SimpleRateLimiter;

public class RateLimitingStoreBackendTestCase extends
		KeyValueStoreBackendTestCase {

	public void testBackend() throws Exception {
		MemcachedKeyValueStore mcc = new MemcachedKeyValueStore();
		mcc.setHosts("localhost:11211");
		mcc.start();

		RateLimitingKeyValueStore store = new RateLimitingKeyValueStore();
		store.setMaster(mcc);
		// test behavior w/ no limits
		doTestBackend(store);

		// add a write limit of one per 100ms
		RateLimiter limiter = new SimpleRateLimiter(TimeUnit.MILLISECONDS, 100,
				1);
		store.setWriteRateLimiter(limiter);
		store.set("test.key", "test.value");
		try {
			store.set("test.key2", "test.value2");
			fail("Rate limit exceeded. Should have failed!");
		} catch (KeyValueStoreUnavailable e) {
			assertEquals(limiter.getCounter(), 1);
		}
		// sleep for 100ms - should succeed
		Thread.sleep(100l);
		store.set("test.key2", "test.value2");
		assertEquals(limiter.getCounter(), 1);

		// test a memcached rate limiter (2 per sec.)
		limiter = new MemcachedRateLimiter(mcc);
		limiter.setLimit(TimeUnit.SECONDS, 1, 2);
		store.setWriteRateLimiter(limiter);
		store.set("test.key", "test.value");
		store.set("test.key", "test.value.2");
		try {
			store.set("test.key2", "test.value3");
			fail("Rate limit exceeded. Should have failed!");
		} catch (KeyValueStoreUnavailable e) {
			assertEquals(limiter.getCounter(), 2);
		}
		// sleep for 1000ms - should succeed
		Thread.sleep(1000l);
		store.set("test.key2", "test.value2");
		assertEquals(limiter.getCounter(), 1);
	}

}
