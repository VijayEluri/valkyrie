package com.othersonline.kv.backends;

import java.io.IOException;

import com.othersonline.kv.KeyValueStore;
import com.othersonline.kv.KeyValueStoreUnavailable;

public interface ConnectionFactory {
	public KeyValueStore getStore(String uri) throws IOException,
			KeyValueStoreUnavailable;
}