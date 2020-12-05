
/*
 */
import java.io.File;
import java.util.Objects;

import org.junit.Test;

import com.almworks.sqlite4java.SQLiteConnection;

import sam.rss.articles.aws.service.SqliteFeedService;
import sam.rss.articles.utils.Ids;

public class MainTest {
	@Test
	public void mainTest() throws Exception {
		try(SqliteFeedService s = new SqliteFeedService(new SQLiteConnection(new File(Objects.requireNonNull(System.getProperty("SQL_DB_PATH")))), true)) {
			System.out.println(s.versionOf(Ids.ID_DB_META));
			System.out.println(s.versionOf(Ids.ID_IDS));
			System.out.println(s.versionOf(Ids.ID_STATUS));
			System.out.println(s.versionOf(Ids.ID_TAGS));
			System.out.println(s.versionOf(6124));
			System.out.println(s.versionOf(2525));	
		}
		
	}
}
