package programming.articles.model.dynamo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import sam.full.access.dynamodb.DynamoConnection;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.myutils.LoggerUtils;

class LoadedMetasTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		LoggerUtils.enableSlf4jSimple(Level.DEBUG);
	}

	@Test
	void test() throws IOException, Exception {
		try(DynamoConnection con = new DynamoConnection()) {
			test0(con, LoadedMetas.class);
			test0(con, SettableLoadedMetas.class);
		}
	}

	private void test0(DynamoConnection con, Class<? extends LoadedMetas> cls) throws IOException, Exception {
		LoadedMetas m = measure(() -> LoadedMetas.load0(con, LoadedMetas.VALID_KEYS, null, cls));
		assertSame(m.getClass(), cls);
		System.err.println("------------------");
		LoadedMetas m2 = measure(() -> LoadedMetas.load0(con, LoadedMetas.VALID_KEYS, m, (Class<LoadedMetas>)cls));
		System.err.println("------------------");
		
		assertSame(m, m2);

		assertNotNull(m.ids);
		assertNotNull(m.tags);
		assertNotNull(m.idStatusMap);
		assertNotNull(m._version);

		assertFalse(m.ids.length == 0);
		assertFalse(m.tags.isEmpty());
		assertFalse(m.idStatusMap.isEmpty());
		assertFalse(m._version.isEmpty());

		Path p1 = Files.createTempFile(null, null);
		ObjectWriter.write(p1, m);

		LoadedMetas m3 = measure(() -> LoadedMetas.load0(con, LoadedMetas.VALID_KEYS, ObjectReader.read(p1), (Class<LoadedMetas>)cls));
		assertSame(m3.getClass(), cls);

		assertNotSame(m.ids, m3.ids);
		assertNotSame(m.tags, m3.tags);
		assertNotSame(m.idStatusMap, m3.idStatusMap);

		assertArrayEquals(m.ids, m3.ids);
		assertEquals(m.tags, m3.tags);
		assertEquals(m.idStatusMap, m3.idStatusMap);
		assertEquals(m._version, m3._version);
	}

	private <E> E measure(Callable<E> c) throws Exception {
		long t = System.currentTimeMillis();
		E e = c.call();
		System.err.println(System.currentTimeMillis() - t);
		return e;
	}
}
