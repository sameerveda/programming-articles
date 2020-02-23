package sam.viewer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Optional;

import programming.articles.model.sql.DbManager;
import programming.articles.utils.Utils;
import sam.config.JsonConfig;
import sam.myutils.Checker;

public class DbManagerImpl extends DbManager {
	public DbManagerImpl(JsonConfig config) throws SQLException {
		Path dbpath = Optional.ofNullable(config.optString("dbpath")).filter(Checker::isNotEmptyTrimmed).map(Paths::get).orElseGet(Utils::dbpath);
		logger.debug("dbpath: {}", dbpath);
		
		init(dbpath, config.optInt("data_buffer", 5000), config.getDeep("iconStoreSize", 5000));
	}
}
