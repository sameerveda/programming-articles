package jdbm.cached;

import static jdbm.RecordManagerFactory.createRecordManager;
import static jdbm.RecordManagerOptions.CACHE_TYPE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import jdbm.RecordManager;

public class JdbmCache implements AutoCloseable {
	public final RecordManager fileDb;
	
	public JdbmCache(Path cacheDir) throws IOException {
		Properties options = new Properties();
		options.put(CACHE_TYPE, "none");

		this.fileDb = createRecordManager(cacheDir.toString().replace('\\', '/').concat("/cache"), options);
	}
	
	public void commit() throws IOException {
		fileDb.commit();
		fileDb.clearCache();
	}
	
	@Override
	public void close() throws Exception {
		commit();
		fileDb.close();
	}
}
