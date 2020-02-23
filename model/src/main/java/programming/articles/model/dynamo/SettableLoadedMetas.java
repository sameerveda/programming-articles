package programming.articles.model.dynamo;

import static programming.articles.model.dynamo.Meta.COMPRESSED_DATA;
import static programming.articles.model.dynamo.Meta.ID;
import static programming.articles.model.dynamo.Meta.SPARSE_DATA;
import static programming.articles.model.dynamo.Meta.TABLE_NAME;
import static programming.articles.model.dynamo.Utils.buf;
import static sam.full.access.dynamodb.DynamoConnection.delete;
import static sam.full.access.dynamodb.DynamoConnection.map;
import static sam.full.access.dynamodb.DynamoConnection.put;
import static sam.full.access.dynamodb.DynamoConnection.value;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.carrotsearch.hppc.ShortByteMap;
import com.carrotsearch.hppc.ShortObjectMap;

import sam.full.access.dynamodb.DynamoConnection;

public final class SettableLoadedMetas extends LoadedMetas {
	private static final Logger logger = LoggerFactory.getLogger(SettableLoadedMetas.class);

	public static SettableLoadedMetas load(DynamoConnection connection, SettableLoadedMetas oldMetas) {
		return load(connection, VALID_KEYS, oldMetas);		
	}
	public static SettableLoadedMetas load(DynamoConnection connection, Collection<String> idsToLoad, SettableLoadedMetas oldMetas) {
		return load0(connection, idsToLoad, oldMetas, SettableLoadedMetas.class);		
	}

	private UpdateItemResult putCompressed(String key, ByteBuffer buf) {
		return update(key, "put compressed", buf, () -> connection.update(TABLE_NAME, ID, value(key), true, Arrays.asList(map(COMPRESSED_DATA, put(value(buf))), map(SPARSE_DATA, delete()))));
	}

	public void setAllIds(short[] ids) {
		if(ids == null || ids.length == 0)
			throw new IllegalArgumentException("ids cannot be empty: "+ids);
		
		ids = processIds(ids);
		putCompressed(IDS, buf(ids));
		this.ids = ids;
	}

	public static short[] processIds(short[] ids) {
		if(ids == null || ids.length < 2)
			return ids;
		
		Arrays.sort(ids);
		
		int size = 0;
		for (int i = 0; i < ids.length - 1; i++) {
			if(ids[i] != ids[i + 1])
				size++;
		}
		
		size++; // account for last elment
		
		if(size == ids.length)
			return ids;
		
		short[] newIds = new short[size];
		size = 0;
		for (int i = 0; i < ids.length - 1; i++) {
			if(ids[i] != ids[i + 1])
				newIds[size++] = ids[i];
		}
		
		newIds[size++] = ids[ids.length - 1];
		
		return Arrays.copyOf(newIds, size);
	}

	public void setStatusIds(ShortByteMap statues) {
		putCompressed(IDS_STATUS, buf(statues));
		logger.debug("set statues: {}", statues.size());
		this.idStatusMap = statues;
	}

	public void setTags(ShortObjectMap<String> tags) {
		if(tags == null || tags.isEmpty())
			throw new IllegalArgumentException("tags cannot be empty: "+tags);
		logger.debug("set tags: {}", tags.size());		
		putCompressed(TAGS, buf(tags));
		this.tags = tags;
	}
}
