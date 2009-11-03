package com.othersonline.kv.backends;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.othersonline.kv.BaseManagedKeyValueStore;
import com.othersonline.kv.KeyValueStore;
import com.othersonline.kv.KeyValueStoreException;
import com.othersonline.kv.annotations.Configurable;
import com.othersonline.kv.annotations.Configurable.Type;
import com.othersonline.kv.transcoder.SerializableTranscoder;
import com.othersonline.kv.transcoder.Transcoder;
import com.sleepycat.db.Cursor;
import com.sleepycat.db.Database;
import com.sleepycat.db.DatabaseConfig;
import com.sleepycat.db.DatabaseEntry;
import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.DatabaseType;
import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;
import com.sleepycat.db.LockMode;
import com.sleepycat.db.OperationStatus;
import com.sleepycat.db.Transaction;

public class BDBKeyValueStore extends BaseManagedKeyValueStore implements
		KeyValueStore, IterableKeyValueStore, LocalKeyValueStore {

	public static final String IDENTIFIER = "bdb";

	public static final String BTREE = "btree";

	public static final String HASH = "hash";

	public static final String QUEUE = "queue";

	public static final String RECNO = "recno";

	private static Log log = LogFactory.getLog(BDBKeyValueStore.class);

	private static Transcoder defaultTranscoder = new SerializableTranscoder();

	private static LockMode LOCK_MODE = LockMode.DEFAULT;

	private boolean transactional = EnvironmentConfig.DEFAULT
			.getTransactional();

	private boolean enableLogging = EnvironmentConfig.DEFAULT
			.getInitializeLogging();

	private boolean flushTransactions = false;

	private long cacheSize = EnvironmentConfig.DEFAULT.getCacheSize();

	private int maxLogFileSize = EnvironmentConfig.DEFAULT.getMaxLogFileSize();

	private String dataDirectory;

	private String dbType = null;

	private String filename = "data.db";

	private EnvironmentConfig environmentConfig;

	private Environment environment;

	private DatabaseConfig databaseConfig;

	private Database db;

	private File bdbdir;

	public BDBKeyValueStore() {
	}

	public BDBKeyValueStore(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}

	public void setEnvironmentConfig(EnvironmentConfig cfg) {
		this.environmentConfig = cfg;
	}

	@Configurable(name = "transactional", accepts = Type.BooleanType)
	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}

	@Configurable(name = "enableLogging", accepts = Type.BooleanType)
	public void setEnableLogging(boolean enableLogging) {
		this.enableLogging = enableLogging;
	}

	@Configurable(name = "flushTransactions", accepts = Type.BooleanType)
	public void setFlushTransactions(boolean flushTransactions) {
		this.flushTransactions = flushTransactions;
	}

	@Configurable(name = "cacheSize", accepts = Type.LongType)
	public void setCacheSize(long cacheSize) {
		this.cacheSize = cacheSize;
	}

	@Configurable(name = "maxLogFileSize", accepts = Type.IntType)
	public void setMaxLogFileSize(int maxLogFileSize) {
		this.maxLogFileSize = maxLogFileSize;
	}

	@Configurable(name = "dataDirectory", accepts = Type.StringType)
	public void setDataDirectory(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	@Configurable(name = "dbType", accepts = Type.StringType)
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	@Configurable(name = "filename", accepts = Type.StringType)
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void start() throws IOException {
		log.trace("start()");
		try {
			environmentConfig = getEnvironmentConfig();
			databaseConfig = new DatabaseConfig();
			databaseConfig.setAllowCreate(true);
			databaseConfig.setSortedDuplicates(false);
			databaseConfig.setTransactional(transactional);
			databaseConfig.setType(getDatabaseType(dbType));
			bdbdir = new File(dataDirectory);
			if (!bdbdir.exists()) {
				log.info("Creating BDB data directory '"
						+ bdbdir.getAbsolutePath() + "'.");
				bdbdir.mkdirs();
			}
			environment = new Environment(bdbdir, environmentConfig);
			if (transactional) {
				Transaction dbTx = environment.beginTransaction(null, null);
				db = environment.openDatabase(dbTx, filename, null,
						databaseConfig);
				dbTx.commit();
			} else {
				db = new Database(filename, null, databaseConfig);
			}
			super.start();
		} catch (DatabaseException e) {
			log.error("DatabaseException inside start()", e);
			throw new IOException(e);
		}
	}

	private EnvironmentConfig getEnvironmentConfig() {
		if (environmentConfig != null)
			return environmentConfig;
		environmentConfig = new EnvironmentConfig();
		environmentConfig.setTransactional(transactional);
		environmentConfig.setCacheSize(cacheSize);
		environmentConfig.setInitializeCache(true);
		environmentConfig.setInitializeLocking(true);
		environmentConfig.setInitializeLogging(enableLogging);
		if (transactional && flushTransactions) {
			environmentConfig.setTxnNoSync(false);
			environmentConfig.setTxnWriteNoSync(false);
		} else if (transactional && !flushTransactions) {
			environmentConfig.setTxnNoSync(false);
			environmentConfig.setTxnWriteNoSync(true);
		} else {
			environmentConfig.setTxnNoSync(true);
		}
		environmentConfig.setAllowCreate(true);
		environmentConfig.setMaxLogFileSize(maxLogFileSize);
		return environmentConfig;
	}

	public void stop() {
		log.trace("stop()");
		try {
			db.close();
		} catch (DatabaseException e) {
			log.error("DatabaseException calling db.close()", e);
		}
		try {
			environment.close();
		} catch (DatabaseException e) {
			log.error("DatabaseException calling environment.close()", e);
		}
		super.stop();
	}

	public boolean exists(String key) throws KeyValueStoreException,
			IOException {
		assertReadable();
		Transaction tx = null;
		try {
			tx = openTransaction();
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes());
			OperationStatus status = db.exists(tx, keyEntry);
			commitTransaction(tx);
			return (OperationStatus.SUCCESS.equals(status)) ? true : false;
		} catch (DatabaseException e) {
			log.error("DatabaseException inside get()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		} catch (Exception e) {
			log.error("Exception inside get()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		}
	}

	public Object get(String key) throws KeyValueStoreException, IOException {
		return get(key, defaultTranscoder);
	}

	public Object get(String key, Transcoder transcoder)
			throws KeyValueStoreException, IOException {
		assertReadable();
		Transaction tx = null;
		try {
			tx = openTransaction();
			Object obj = get(tx, key, transcoder);
			commitTransaction(tx);
			return obj;
		} catch (DatabaseException e) {
			log.error("DatabaseException inside get()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		} catch (Exception e) {
			log.error("Exception inside get()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		}
	}

	public Object get(Transaction tx, String key, Transcoder transcoder)
			throws KeyValueStoreException, IOException {
		assertReadable();
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes());
			DatabaseEntry dataEntry = new DatabaseEntry();
			OperationStatus status = db.get(tx, keyEntry, dataEntry, LOCK_MODE);
			if (OperationStatus.SUCCESS.equals(status)) {
				byte[] bytes = dataEntry.getData();
				Object obj = transcoder.decode(bytes);
				return obj;
			} else if (OperationStatus.NOTFOUND.equals(status)) {
				return null;
			} else if (OperationStatus.KEYEMPTY.equals(status)) {
				return null;
			} else {
				throw new KeyValueStoreException("Unknown status: " + status);
			}
		} catch (DatabaseException e) {
			log.error("DatabaseException inside get()", e);
			throw new KeyValueStoreException(e);
		}

	}

	public Map<String, Object> getBulk(String... keys)
			throws KeyValueStoreException, IOException {
		return getBulk(Arrays.asList(keys), defaultTranscoder);
	}

	public Map<String, Object> getBulk(List<String> keys)
			throws KeyValueStoreException, IOException {
		return getBulk(keys, defaultTranscoder);
	}

	public Map<String, Object> getBulk(List<String> keys, Transcoder transcoder)
			throws KeyValueStoreException, IOException {
		assertReadable();
		Transaction tx = null;
		try {
			tx = openTransaction();
			Map<String, Object> results = getBulk(tx, keys, transcoder);
			commitTransaction(tx);
			return results;
		} catch (DatabaseException e) {
			log.error("DatabaseException inside get()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		} catch (Exception e) {
			log.error("Exception inside get()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		}
	}

	public Map<String, Object> getBulk(Transaction tx, List<String> keys,
			Transcoder transcoder) throws KeyValueStoreException, IOException {
		assertReadable();
		try {
			Map<String, Object> results = new HashMap<String, Object>();
			for (String key : keys) {
				DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes());
				DatabaseEntry dataEntry = new DatabaseEntry();
				OperationStatus status = db.get(tx, keyEntry, dataEntry,
						LOCK_MODE);
				if (OperationStatus.SUCCESS.equals(status)) {
					byte[] bytes = dataEntry.getData();
					Object obj = transcoder.decode(bytes);
					results.put(key, obj);
				}
			}
			return results;
		} catch (DatabaseException e) {
			log.error("DatabaseException inside get()", e);
			throw new KeyValueStoreException(e);
		}
	}

	public void set(String key, Object value) throws KeyValueStoreException,
			IOException {
		set(key, value, defaultTranscoder);
	}

	public void set(String key, Object value, Transcoder transcoder)
			throws KeyValueStoreException, IOException {
		assertWriteable();
		Transaction tx = null;
		try {
			tx = openTransaction();
			set(tx, key, value, transcoder);
			commitTransaction(tx);
		} catch (DatabaseException e) {
			log.error("DatabaseException inside get()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		} catch (Exception e) {
			log.error("Exception inside get()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		}
	}

	public void set(Transaction tx, String key, Object value,
			Transcoder transcoder) throws KeyValueStoreException, IOException {
		assertWriteable();
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes());
			byte[] bytes = transcoder.encode(value);
			DatabaseEntry dataEntry = new DatabaseEntry(bytes);
			db.put(tx, keyEntry, dataEntry);
		} catch (DatabaseException e) {
			log.error("DatabaseException inside set()", e);
			throw new KeyValueStoreException(e);
		}
	}

	public void delete(String key) throws KeyValueStoreException, IOException {
		assertWriteable();
		Transaction tx = null;
		try {
			tx = openTransaction();
			delete(tx, key);
			commitTransaction(tx);
		} catch (DatabaseException e) {
			log.error("DatabaseException inside delete()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		} catch (Exception e) {
			log.error("Exception inside delete()", e);
			abortTransaction(tx);
			throw new KeyValueStoreException(e);
		}
	}

	public void delete(Transaction tx, String key)
			throws KeyValueStoreException, IOException {
		assertWriteable();
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes());
			db.delete(tx, keyEntry);
		} catch (DatabaseException e) {
			log.error("DatabaseException inside set()", e);
			throw new KeyValueStoreException(e);
		} finally {
		}
	}

	public KeyValueStoreIterator iterkeys() throws KeyValueStoreException {
		assertReadable();
		Transaction tx = null;
		try {
			tx = openTransaction();
			Cursor cursor = db.openCursor(tx, null);
			return new BDBIterator(cursor, tx, defaultTranscoder);
		} catch (DatabaseException e) {
			log.error("DatabaseException calling openCursor()", e);
			throw new KeyValueStoreException(e);
		} finally {
		}
	}

	public Transaction openTransaction() throws DatabaseException {
		Transaction tx = (transactional) ? environment.beginTransaction(null,
				null) : null;
		return tx;
	}

	public void commitTransaction(Transaction tx) throws KeyValueStoreException {
		try {
			if (tx != null)
				tx.commit();
		} catch (DatabaseException e) {
			log.error("DatabaseException calling commit()", e);
			throw new KeyValueStoreException(e);
		}
	}

	public void abortTransaction(Transaction tx) throws KeyValueStoreException {
		try {
			if (tx != null)
				tx.abort();
		} catch (DatabaseException e) {
			log.error("DatabaseException calling abort()", e);
			throw new KeyValueStoreException(e);
		}
	}

	public void sync() throws KeyValueStoreException {
		try {
			db.sync();
		} catch (DatabaseException e) {
			log.error("DatabaseException calling sync()", e);
			throw new KeyValueStoreException(e);
		}
	}

	private DatabaseType getDatabaseType(String type) {
		if (type == null)
			return DatabaseType.BTREE;
		else if (BTREE.equals(type))
			return DatabaseType.BTREE;
		else if (HASH.equals(type))
			return DatabaseType.HASH;
		else if (QUEUE.equals(type))
			return DatabaseType.QUEUE;
		else if (RECNO.equals(type))
			return DatabaseType.RECNO;
		else {
			log.error("Unknown database type " + type);
			throw new IllegalArgumentException(String.format(
					"%1$s is not in (btree, hash, queue, recno)", type));
		}
	}

	private class BDBIterator implements KeyValueStoreIterator,
			Iterator<String> {

		private Cursor cursor;

		private Transaction tx;

		private String current;

		public BDBIterator(Cursor cursor, Transaction tx, Transcoder transcoder)
				throws KeyValueStoreException {
			this.cursor = cursor;
			this.tx = tx;
		}

		public Iterator<String> iterator() {
			return this;
		}

		public void close() {
			try {
				cursor.close();
				commitTransaction(tx);
			} catch (DatabaseException e) {
				log.error("DatabaseException calling cursor.close()", e);
			} catch (KeyValueStoreException e) {
				log.error("KeyValueStoreException calling commitTransaction()",
						e);
			}
		}

		public boolean hasNext() {
			DatabaseEntry keyEntry = new DatabaseEntry();
			DatabaseEntry valueEntry = new DatabaseEntry();
			try {
				cursor.getNext(keyEntry, valueEntry, null);
				byte[] bytes = keyEntry.getData();
				current = (bytes == null) ? null : new String(bytes);
			} catch (DatabaseException e) {
				throw new RuntimeException(e);
			}
			return current != null;
		}

		public String next() {
			return current;
		}

		public void remove() {
			try {
				cursor.delete();
			} catch (DatabaseException e) {
				log.error("DatabaseException calling cursor.delete()", e);
			}
		}

	}
}
