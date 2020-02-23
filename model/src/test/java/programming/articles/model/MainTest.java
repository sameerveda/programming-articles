package programming.articles.model;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import programming.articles.model.dynamo.LoadedMetas;
import programming.articles.model.dynamo.SettableLoadedMetas;
import sam.full.access.dynamodb.DynamoConnection;
import sam.io.serilizers.ObjectReader;
import sam.myutils.LoggerUtils;

class MainTest {
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		LoggerUtils.enableSlf4jSimple(Level.DEBUG);
	}

	@Test
	void test() throws IOException, ClassNotFoundException {
	}

}
