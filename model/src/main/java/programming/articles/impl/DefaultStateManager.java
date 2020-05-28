package programming.articles.impl;

import static programming.articles.model.DataItemMeta.DYNAMO_TABLE_NAME;
import static programming.articles.model.DataItemMeta.ID;
import static programming.articles.model.DataItemMeta.STATUS;
import static programming.articles.model.DataItemMeta.VERSION;
import static programming.articles.model.DataStatus.parse;
import static programming.articles.model.DataStatus.values;
import static sam.full.access.dynamodb.DynamoConnection.delete;
import static sam.full.access.dynamodb.DynamoConnection.map;
import static sam.full.access.dynamodb.DynamoConnection.put;
import static sam.full.access.dynamodb.DynamoConnection.value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.carrotsearch.hppc.ShortByteMap;
import com.carrotsearch.hppc.ShortByteScatterMap;
import com.carrotsearch.hppc.ShortObjectMap;
import com.carrotsearch.hppc.ShortObjectScatterMap;

import programming.articles.api.StateManager;
import programming.articles.model.DataItem;
import programming.articles.model.DataStatus;
import programming.articles.model.LoadedMetas;
import programming.articles.model.Tag;
import sam.full.access.dynamodb.DynamoConnection;
import sam.myutils.Checker;
import sam.myutils.HPPCUtils;

public class DefaultStateManager implements StateManager {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<String, Tag> tagsByName;
	private final ShortObjectScatterMap<Tag> tagsById;
	private final LoadedMetas metas;
	private final AtomicInteger nextTag = new AtomicInteger();
	private final List<Tag> newTags = new ArrayList<>();
	private final ShortByteMap statuses;
	private final ShortByteMap newStatuses = new ShortByteScatterMap();

	private volatile boolean closed;
	private ExecutorService executor;
	private DynamoConnection dynamo;

	public DefaultStateManager(LoadedMetas metas) throws IOException {
		this.metas = metas;
		this.statuses = metas.idStatusMap() == null ? new ShortByteScatterMap() : metas.idStatusMap();
		this.tagsByName = new HashMap<>(this.metas.tags().size());
		this.tagsById = new ShortObjectScatterMap<>(this.metas.tags().size());
		int max[] = { 0 };
		HPPCUtils.forEach(this.metas.tags(), (n, s) -> {
			putTag(new Tag(n, s), false);
			max[0] = Math.max(max[0], n);
		});
		this.nextTag.set(max[0]);
	}

	public void setConnection(DynamoConnection dynamo) {
		this.dynamo = dynamo;
	}

	private void putTag(Tag t, boolean isNew) {
		this.tagsByName.put(t.name.toLowerCase().trim(), t);
		this.tagsById.put(t.getId(), t);
		if (isNew)
			newTags.add(t);
	}

	private void ensureNotClosed() {
		if (closed)
			throw new IllegalStateException("closed");
	}

	@Override
	public Collection<Tag> allTagsNames() {
		ensureNotClosed();
		return Collections.unmodifiableCollection(tagsByName.values());
	}

	static final DataStatus[] statusArray = values();

	@Override
	public List<DataItem> loadItems(short[] ids) {
		if(ids.length == 0)
			return Collections.emptyList();
		List<Map<String, AttributeValue>> list = dynamo.getBatch(DYNAMO_TABLE_NAME, ID, IntStream.range(0, ids.length).mapToObj(n -> value(ids[n])), DataItem.COLUMNS);
		return list.stream().map(DataItem::new).collect(Collectors.toList());
	}

	@Override
	public void loadItems(short[] ids, BiConsumer<List<DataItem>, Exception> onResult) {
		supplyAsync(() -> loadItems(ids)).handle((ret, error) -> {
			onResult.accept(ret, (Exception) error);
			return null;
		});
	}

	public Map<String, AttributeValue> getIfVersionMismatch(short id, int version) {
		QueryRequest req = new QueryRequest();
		req.withTableName(DYNAMO_TABLE_NAME).withKeyConditionExpression(ID + " = :id")
				.withFilterExpression("attribute_exists(version) AND version <> :version")
				.addExpressionAttributeValuesEntry(":id", value(id))
				.addExpressionAttributeValuesEntry(":version", value(version))
				.withProjectionExpression("tags, #status, notes, version")
				.withExpressionAttributeNames(Collections.singletonMap("#status", "session"));

		List<Map<String, AttributeValue>> list = this.dynamo.db.query(req).getItems();

		if (list.isEmpty())
			return null;

		if (list.size() > 1)
			throw new IllegalStateException(list.toString());
		System.out.println(list.get(0).keySet());
		return list.get(0);
	}

	@Override
	public DataItem getItem(short id) {
		logger.debug("loading full data item for: " + id);
		Map<String, AttributeValue> map = this.dynamo.db.getItem(DYNAMO_TABLE_NAME, Collections.singletonMap(ID, value(id)))
				.getItem();
		if (Checker.isEmpty(map))
			return null;
		return new DataItem(map);
	}

	@Override
	public void getItem(short id, BiConsumer<DataItem, Exception> consumer) {
		runAsync(() -> {
			try {
				getItem(id);
			} catch (Exception e) {
				consumer.accept(null, e);
			}
		});
	}

	@Override
	public Tag getTagByName(String name) {
		ensureNotClosed();
		Objects.requireNonNull(name);
		if (Checker.isEmptyTrimmed(name))
			throw new IllegalArgumentException("tagname cannot be empty");

		Tag t = tagsByName.get(name.trim().toLowerCase());
		if (t == null) {
			int n = this.nextTag.incrementAndGet();
			if (n > Short.MAX_VALUE)
				throw new IllegalStateException("id exceeds short limit: " + n);
			t = new Tag((short) n, name);
			putTag(t, true);
		}
		return t;
	}

	@Override
	public Tag getTagById(short tagId) {
		ensureNotClosed();
		return this.tagsById.get(tagId);
	}

	@Override
	public short[] allIds() {
		ensureNotClosed();
		return metas.ids();
	}

	private <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
		ensureNotClosed();
		if (executor == null)
			executor = Executors.newSingleThreadExecutor();
		return CompletableFuture.supplyAsync(task, executor);
	}

	private void runAsync(Runnable task) {
		ensureNotClosed();
		if (executor == null)
			executor = Executors.newSingleThreadExecutor();
		executor.execute(task);
	}

	@Override
	public void close() throws Exception {
		ensureNotClosed();
		closed = true;

		if (executor != null) {
			executor.shutdown();
			executor.awaitTermination(2, TimeUnit.DAYS);
		}

		commitNewTags();

		if (!newStatuses.isEmpty()) {
			metas.addStatusIds(newStatuses);
			statuses.putAll(newStatuses);
			logger.debug("status change: {}", newStatuses);
			newStatuses.clear();
		}

	}

	@Override
	public void commitNewTags() {
		if (!newTags.isEmpty()) {
			ShortObjectMap<String> map = new ShortObjectScatterMap<>();
			newTags.forEach(t -> map.put(t.getId(), t.getName()));
			this.metas.addTags(map);
			logger.debug("new tags: {}", newTags);
			newTags.clear();
		}
	}

	@Override
	public void commit(DataItem dataItem, BiConsumer<Boolean, Exception> onResult) {
		Item item = (Item) dataItem;
		if (Checker.isEmpty(item.updates))
			return;

		runAsync(() -> {
			try {
				onResult.accept(commit(dataItem), null);
			} catch (Exception e) {
				onResult.accept(null, e);
			}
		});
	}

	public int commit0(final short id, final Map<String, String> updateMap, int oldVersion) {
		if (Checker.isEmpty(updateMap))
			return -1;

		updateMap.forEach((s, t) -> {
			if (!DataItem.UPDATEABLE_FIELDS.contains(s))
				throw new IllegalArgumentException("field: " + s + ", is no updateable");
		});

		List<Map<String, AttributeValueUpdate>> updates = new ArrayList<>();
		updateMap.forEach(
				(field, value) -> updates.add(map(field, Checker.isEmpty(value) ? delete() : put(value(value)))));
		UpdateItemResult result = dynamo.update(DYNAMO_TABLE_NAME, ID, value(id), true, updates);

		if (updateMap.containsKey(STATUS)) {
			statuses.put(id, parse(updateMap.get(STATUS)).byteValue());
			newStatuses.put(id, parse(updateMap.get(STATUS)).byteValue());
		}

		int version = Integer.parseInt(result.getAttributes().get(VERSION).getN());
		logger.debug("update id: {}, updates: {}, version: {} -> {}", id, updateMap, oldVersion, version);
		return version;
	}

	@Override
	public boolean commit(DataItem dataItem) {
		Objects.requireNonNull(dataItem);
		Map<String, String> updateMap = ((Item) dataItem).updates;
		int newVersion = commit0((short)dataItem.getId(), updateMap, dataItem.getVersion());
		if (newVersion < 0)
			return false;

		dataItem.setVersion(newVersion);
		updateMap.clear();
		return true;
	}

	@Override
	public byte getStatusOrdinal(short itemId) {
		return statuses.get(itemId);
	}
}
