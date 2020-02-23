package sam.full.access.dynamodb;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.KeyPair;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;

public class DynamoConnection implements AutoCloseable {
	public final AmazonDynamoDB db;
	public final DynamoDBMapper mapper;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public DynamoConnection() {
		this(new EnvironmentVariableCredentialsProvider());
	}

	public DynamoConnection(AWSCredentialsProvider creds, Regions region) {
		this.db = AmazonDynamoDBClientBuilder.standard().withCredentials(creds).withRegion(region).build();
		this.mapper = new DynamoDBMapper(db);
	}

	public DynamoConnection(AWSCredentialsProvider creds) {
		this(creds, Regions.AP_SOUTH_1);
	}

	public PutItemResult putItem(String tableName, String keyField, AttributeValue keyValue, String valueField,
			AttributeValue valueValue) {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put(keyField, keyValue);
		item.put(valueField, valueValue);

		return this.db.putItem(tableName, item);
	}

	public <E> Map<Class<?>, List<Object>> loadBatch(Map<Class<?>, List<KeyPair>> itemsToGet) {
		Map<String, List<Object>> result = mapper.batchLoad(itemsToGet);
		if (result.isEmpty())
			return emptyMap();

		Map<Class<?>, List<Object>> map = new HashMap<>();
		itemsToGet.forEach((cls, list) -> map.put(cls, result.get(tablename(cls))));
		return map;
	}

	public UpdateItemResult update(String tableName, String keyField, AttributeValue keyValue, boolean versionUpdate,
			String valueField, AttributeValueUpdate valueValue) {
		return update(tableName, keyField, keyValue, versionUpdate,
				Collections.singletonList(map(valueField, valueValue)));
	}

	public static Map<String, AttributeValueUpdate> map(String field, AttributeValueUpdate value) {
		return singletonMap(field, value);
	}

	public UpdateItemResult update(String tableName, String keyField, AttributeValue keyValue, boolean versionUpdate,
			Iterable<Map<String, AttributeValueUpdate>> updatesMap) {
		Iterator<Map<String, AttributeValueUpdate>> updates = updatesMap.iterator();
		if (!updates.hasNext())
			throw new IllegalArgumentException("no updates specified");

		UpdateItemRequest req = new UpdateItemRequest();
		req.withTableName(tableName).withKey(singletonMap(keyField, keyValue));

		while (updates.hasNext())
			updates.next().forEach(req::addAttributeUpdatesEntry);

		if (versionUpdate) {
			req.addAttributeUpdatesEntry("version", add(value(1)));
			req.setReturnValues(ReturnValue.UPDATED_NEW);
		}
		
		return this.db.updateItem(req);
	}

	public DeleteItemResult delete(String tableName, String keyField, AttributeValue keyValue) {
		return this.db.deleteItem(tableName, singletonMap(keyField, keyValue));
	}

	public static AttributeValue value(Set<String> val) {
		return new AttributeValue().withSS(val);
	}

	public static AttributeValue value(CharSequence val) {
		return new AttributeValue().withS(val.toString());
	}

	public static AttributeValue value(int val) {
		return new AttributeValue().withN(Integer.toString(val));
	}

	public static AttributeValue value(Number val) {
		return new AttributeValue().withN(val.toString());
	}

	public static AttributeValue value(ByteBuffer buf) {
		return new AttributeValue().withB(buf);
	}

	public static AttributeValue value(AttributeValue... values) {
		return new AttributeValue().withL(values);
	}

	public static AttributeValue value(Boolean bool) {
		return new AttributeValue().withBOOL(bool);
	}

	public static AttributeValueUpdate add(AttributeValue value) {
		return new AttributeValueUpdate(value, AttributeAction.ADD);
	}

	public static AttributeValueUpdate put(AttributeValue value) {
		return new AttributeValueUpdate(value, AttributeAction.PUT);
	}

	public static AttributeValueUpdate delete(AttributeValue value) {
		return new AttributeValueUpdate(value, AttributeAction.DELETE);
	}

	public static AttributeValueUpdate delete() {
		return new AttributeValueUpdate(null, AttributeAction.DELETE);
	}
	
	public static int getInt(AttributeValue value) {
		return Integer.parseInt(value.getN());
	}
	public static short getShort(AttributeValue value) {
		return Short.parseShort(value.getN());
	}
	
	public static String optString(Map<String, AttributeValue> values, String key) {
		return optString(values.get(key), null);
	}
	
	public static String optString(AttributeValue value, String defaultValue) {
		return value == null || value.getS() == null ? defaultValue : value.getS();
	}
	
	public static int optInt(AttributeValue value, int defaultValue) {
		return value == null || value.getN() == null ? defaultValue : Integer.parseInt(value.getN());
	}
	public static short optShort(AttributeValue value, short defaultValue) {
		return value == null || value.getN() == null ? defaultValue : Short.parseShort(value.getN());
	}
	
	public static ByteBuffer optB(AttributeValue value) {
		return value == null ? null : value.getB();
	}
	
	public static <E> List<E> optL(AttributeValue value, Function<AttributeValue, E> mapper) {
		List<AttributeValue> list = value == null ? null : value.getL();
		if(list == null || list.isEmpty())
			return null;
		return list.stream().map(mapper).collect(Collectors.toList());
	}

	private static String tablename(Class<?> cls) {
		return cls.getAnnotation(DynamoDBTable.class).tableName();
	}

	@SuppressWarnings({ "unchecked" })
	public <E> List<E> loadBatch(Class<E> target, List<KeyPair> query) {
		return (List<E>) mapper.batchLoad(singletonMap(target, query)).get(tablename(target));
	}

	@Override
	public void close() {
		db.shutdown();
	}
	
	public void putItem(String tableName, Map<String, AttributeValue> items) {
		this.db.putItem(tableName, items);
	}
	
	public void putItems(String tableName, List<Map<String, AttributeValue>> itemsToPut,
			Predicate<List<Map<String, List<WriteRequest>>>> retryFailed) {
		requireNonNull(itemsToPut);
		
		List<WriteRequest> items = new ArrayList<>();
		
		for (Map<String,AttributeValue> t : itemsToPut) {
			items.add(new WriteRequest(new PutRequest(t)));
			if(items.size() == 25) {
				if(!write(Collections.singletonList(singletonMap(tableName, items)), retryFailed))
					return;
				items.clear();
			}
		} 
		
		if(!items.isEmpty()) {
			write(Collections.singletonList(singletonMap(tableName, items)), retryFailed);
			items.clear();
		}
	}

	public void save(Iterable<? extends Object> itemsToPut,
			Predicate<List<Map<String, List<WriteRequest>>>> retryFailed) {
		requireNonNull(itemsToPut);

		List<Map<String, List<WriteRequest>>> list = mapper.batchSave(itemsToPut).stream()
				.map(f -> f.getUnprocessedItems())
				.filter(map -> !map.isEmpty())
				.collect(toList());
		
		write(list, retryFailed);
	}

	/**
	 * 
	 * @param list
	 * @param retryFailed
	 * @return value of continue
	 */
	private boolean write(List<Map<String, List<WriteRequest>>> list, Predicate<List<Map<String, List<WriteRequest>>>> retryFailed) {
		while (!list.isEmpty()) {
			if (!retryFailed.test(list))
				return false;

			list = list.stream()
					.map(db::batchWriteItem).map(BatchWriteItemResult::getUnprocessedItems)
					.filter(m -> !m.isEmpty())
					.collect(toList());
		}
		return true;
	}

	public List<Map<String, AttributeValue>> getBatch(String tableName, String idField, Stream<AttributeValue> ids, Collection<String> attributesToGet) {
		KeysAndAttributes attr = new KeysAndAttributes();
		if (attributesToGet != null)
			attr.withAttributesToGet(attributesToGet);
		attr.withKeys(ids.map(m -> singletonMap(idField, m)).collect(Collectors.toList()));

		BatchGetItemResult result = this.db.batchGetItem(singletonMap(tableName, attr));
		List<Map<String, AttributeValue>> sink = new ArrayList<>();
		sink.addAll(result.getResponses().getOrDefault(tableName, Collections.emptyList()));

		int retries = 10;
		Map<String, KeysAndAttributes> remaining;
		while (--retries > 0 && !(remaining = result.getUnprocessedKeys()).isEmpty()) {
			logger.debug("retry 'get' attempt {} for tablename: {}, idField: {}, remaining({}): {}", 10 - retries, tableName, idField, remaining.size(), remaining);
			result = this.db.batchGetItem(remaining);
			sink.addAll(result.getResponses().getOrDefault(tableName, Collections.emptyList()));
		}
		if(!result.getUnprocessedKeys().isEmpty())
			throw new IllegalStateException("get operation failed: "+result.getUnprocessedKeys());
		return sink;
	}
	
	public static Map<String, String> toAttributesToGet(Collection<String> list) {
		return list.stream().collect(Collectors.toMap(s -> "#".concat(s), s -> s));
	}

	public Map<String, AttributeValue> get(String tableName, String idField, AttributeValue idValue, Collection<String> attributesToGet) {
		GetItemRequest req = new GetItemRequest();
		req.withKey(singletonMap(idField, idValue))
		.withTableName(tableName);
		
		if(attributesToGet != null)
			req.withAttributesToGet(attributesToGet);

		return this.db.getItem(req).getItem();
	}

}