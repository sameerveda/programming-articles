package programming.articles.model.dynamo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import programming.articles.model.DataStatus;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;

class DataItemTest {

	@Test
	void testConstantDataItem() throws IOException, ClassNotFoundException {
		ConstantDataItem t = new ConstantDataItem();
		set(t);
		
		String json = t.toString();
		System.out.println(json);
		ConstantDataItem t2 = new ConstantDataItem(new JSONObject(json));
		
		check(t, t2);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectWriter.write(bos, t2);
		
		byte[] bytes = bos.toByteArray();
		System.out.println(bytes.length);
		t2 = ObjectReader.read(new ByteArrayInputStream(bytes));
		
		check(t, t2);
	}
	
	private static void check(ConstantDataItem t, ConstantDataItem t2) {
		assertEquals(t.id, t2.id);
		assertEquals(t.title, t2.title);
		assertEquals(t.source, t2.source);
		assertEquals(t.redirect, t2.redirect);
		assertEquals(t.favicon, t2.favicon);
		assertEquals(t.addedOn, t2.addedOn);
	}

	@Test
	void testDataItem() throws IOException, ClassNotFoundException {
		DataItem t = new DataItem();
		set(t);
		
		t.tags = uuid();
		t.status = DataStatus.DELETED;
		t.notes = uuid();
		t.version = new Random().nextInt();
		
		String json = t.toString();
		System.out.println(json);
		DataItem t2 = new DataItem(new JSONObject(json));
		
		check0(t, t2);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectWriter.write(bos, t2);
		
		byte[] bytes = bos.toByteArray();
		System.out.println(bytes.length);
		t2 = ObjectReader.read(new ByteArrayInputStream(bytes));
		
		check0(t, t2);
	}

	private static void check0(DataItem t, DataItem t2) {
		check(t, t2);
		assertEquals(t.tags, t2.tags);
		assertEquals(t.status, t2.status);
		assertEquals(t.notes, t2.notes);
		assertEquals(t.version, t2.version);
	}

	private void set(ConstantDataItem t) {
		t.id = (short) new Random().nextInt(Short.MAX_VALUE);
		t.title = uuid();
		t.source = uuid();
		t.redirect = uuid();
		t.favicon = uuid();
		t.addedOn = uuid();
	}

	private String uuid() {
		return UUID.randomUUID().toString();
	}

}
