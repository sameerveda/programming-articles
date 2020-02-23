package programming.articles.model.dynamo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.ShortScatterSet;

import sam.collection.ArraysUtils;
import sam.myutils.LoggerUtils;

class SettableLoadedMetasTest {
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		LoggerUtils.enableSlf4jSimple(Level.DEBUG);
	}

	@Test
	void processIds() {
		assertNull(SettableLoadedMetas.processIds(null));
		short[] ids = {1};
		assertSame(ids, SettableLoadedMetas.processIds(ids));
		
		ids = new short[] {1,2,3,4,65,7,8};
		assertSame(ids, SettableLoadedMetas.processIds(ids));
		
		assertArrayEquals(new short[]{1,2,3,4,5,6}, SettableLoadedMetas.processIds(new short[]{1,2,3,4,5,5,5,6}));
		assertArrayEquals(new short[]{1,2,3,4,5}, SettableLoadedMetas.processIds(new short[]{1,2,3,4,5,5,5}));
		assertArrayEquals(new short[]{1,2,3,4,5}, SettableLoadedMetas.processIds(new short[]{1,2,3,4,4,4,4,5}));
		
		assertArrayEquals(new short[]{1,2,3,4,5,6}, SettableLoadedMetas.processIds(new short[]{1,1,1,2,3,4,5,5,5,6}));
		assertArrayEquals(new short[]{1,2,3,4,5,6}, SettableLoadedMetas.processIds(new short[]{1,2,2,2,3,4,5,5,5,6}));
		
		int[] array = new Random().ints(10000, 0, 100).toArray();
		assertEquals(array.length, 10000);
		
		ids = ArraysUtils.mapToShort(array);
		short[] converted = SettableLoadedMetas.processIds(ids);
		System.out.println("size change: "+ids.length + " -> "+converted.length);
		IntScatterSet set = new IntScatterSet();
		set.addAll(array);
		array = set.toArray();
		Arrays.sort(array);
		
		assertArrayEquals(array, ArraysUtils.mapToInt(converted));
		
		ShortScatterSet set2 = new ShortScatterSet();
		new Random().ints(5000, 0, 1000).forEach(t -> set2.add((short)t));
		
		System.out.println("unique size: "+set2.size());
		
		ids = set2.toArray();
		assertSame(SettableLoadedMetas.processIds(ids), ids);
	}

}
