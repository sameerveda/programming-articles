package programming.articles.app.providers;

import static java.nio.file.Files.exists;
import static programming.articles.api.CacheHandler.fromMap;
import static sam.io.serilizers.ObjectReader.read;
import static sam.myutils.System2.lookupInt;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codejargon.feather.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import programming.articles.api.CacheHandler;
import programming.articles.api.DataItemPagination;
import programming.articles.api.StateManager;
import programming.articles.impl.DefaultDataItemPagination;
import programming.articles.impl.DefaultStateManager;
import programming.articles.model.LoadedMetas;
import sam.full.access.dynamodb.DynamoConnection;
import sam.io.serilizers.ObjectWriter;
import sam.myutils.MyUtilsException;

public abstract class DefaultProviders {
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final DefaultStateManager stateManager;
	protected final DefaultDataItemPagination dataItemPagination;
	protected final DynamoConnection connection;
	protected LoadedMetas loadedMetas;
	protected final List<Closeable> closeables = new ArrayList<>();

	public DefaultProviders() throws Exception {
		Path p = this.loadedMetasPath();
		LoadedMetas m = p != null && exists(p) ? read(p) : null; // open connection if cache is read successfully 
		this.connection = openConnection();
		this.loadedMetas = m;

		reset();
		cacheInit();

		this.stateManager = new DefaultStateManager(this.loadedMetas);
		this.stateManager.setConnection(connection);
		this.loadedMetas.setConnection(connection);

		this.dataItemPagination = new DefaultDataItemPagination(stateManager);
		dataItemPagination.setPageSize(lookupInt("dataitem.page.size", 50));
	}

	protected abstract DynamoConnection openConnection() throws Exception;
	protected abstract void cacheInit() throws IOException;
	protected abstract <E extends Comparable<?>, F> Map<E, F> cacheFor(String name);
	protected abstract Path loadedMetasPath();

	protected <E, F> CacheHandler<E, F> wrap(Map<E, F> map, Class<?> cls) {
		if (map == null || map == Collections.EMPTY_MAP)
			return CacheHandler.empty();

		CacheHandler<E, F> c = fromMap(map);
		if (map.getClass().getCanonicalName().startsWith("java.util."))
			return c;

		if (logger.isDebugEnabled()) {
			c = CacheHandler.logged(c, cls);
			closeables.add(c);
		}
		return c;
	}

	public LoadedMetas getLoadedMetas() {
		return loadedMetas;
	}

	public void saveLoadedMetas(Path p) throws IOException {
		if (p == null)
			throw new IllegalStateException("cacheDir not specified");

		if (loadedMetas.isUpdated()) {
			ObjectWriter.write(p, loadedMetas);
			loadedMetas.setUpdated(false);
			logger.debug("saved: {}, at: {}", loadedMetas.getClass(), p);
		}
	}

	public void close() throws Exception {
		stateManager.close();
		closeables.forEach(t -> MyUtilsException.toUnchecked(() -> {
			t.close();
			return null;
		}));

		if (loadedMetasPath() != null)
			saveLoadedMetas(loadedMetasPath());

		connection.close();
		this.stateManager.setConnection(null);
		this.loadedMetas.setConnection(null);
	}

	public void reset() {
		this.loadedMetas = LoadedMetas.load(connection, this.loadedMetas);
	}

	@Provides
	public DataItemPagination dataItemPagination() {
		return dataItemPagination;
	}

	@Provides
	public StateManager stateManager() {
		return stateManager;
	}
}
