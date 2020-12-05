
/*
 */
import org.junit.Test;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import sam.rss.articles.aws.service.AwsFeedService;
import sam.rss.articles.utils.Ids;

public class MainTest {
	@Test
	public void mainTest() {
		AwsFeedService s = new AwsFeedService(AmazonDynamoDBClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build());
		System.out.println(s.versionOf(Ids.ID_DB_META));
		System.out.println(s.versionOf(Ids.ID_IDS));
		System.out.println(s.versionOf(Ids.ID_STATUS));
		System.out.println(s.versionOf(Ids.ID_TAGS));
		System.out.println(s.versionOf(6124));
		System.out.println(s.versionOf(2525));
	}
}
