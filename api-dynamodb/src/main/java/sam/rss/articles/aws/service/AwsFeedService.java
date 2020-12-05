package sam.rss.articles.aws.service;

import static java.util.Collections.singletonMap;
import static sam.rss.articles.aws.utils.AwsUtils.add;
import static sam.rss.articles.aws.utils.AwsUtils.value;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

import sam.rss.articles.aws.model.AwsFeedEntry;
import sam.rss.articles.aws.model.AwsFeedEntryIds;
import sam.rss.articles.aws.model.AwsFeedEntryStatuses;
import sam.rss.articles.aws.model.AwsTags;
import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.model.FeedEntryIds;
import sam.rss.articles.model.FeedEntryStatuses;
import sam.rss.articles.model.Tags;
import sam.rss.articles.service.FeedsService;

@Singleton
public class AwsFeedService implements FeedsService {
	public static final String TABLE_NAME = "programming-articles";

	private final AmazonDynamoDB client;
	private final DynamoDBMapper mapper;
	private final Table table;

	@Inject
	public AwsFeedService(AmazonDynamoDB client) {
		this.client = Objects.requireNonNull(client);
		this.table = new DynamoDB(Objects.requireNonNull(client)).getTable(TABLE_NAME);
		this.mapper = new DynamoDBMapper(client);
	}

	@Override
	public FeedEntry getEntry(int id) {
		if (id < 100)
			throw new IllegalArgumentException("bad id, id should be > 100, but was: " + id);

		AwsFeedEntry f = mapper.load(AwsFeedEntry.class, id);
		if (f != null)
			f.setService(this);
		return f;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<FeedEntry> getEntries(int[] ids) {
		if (ids.length == 0)
			return Collections.emptyList();
		for (int id : ids) {
			if (id < 100)
				throw new IllegalArgumentException("bad id, id should be > 100, but was: " + id + "in: " + ids);
		}

		List list = this.mapper.batchLoad(() -> Arrays.stream(ids).mapToObj(AwsFeedEntry::new).map(Object.class::cast).iterator())
				.get(TABLE_NAME);
		list.forEach(e -> {
			if (e != null)
				((AwsFeedEntry) e).setService(this);
		});
		return list;
	}

	@Override
	public FeedEntryIds getIds() {
		return mapper.load(new AwsFeedEntryIds());
	}

	@Override
	public FeedEntryStatuses getStatueses() {
		return mapper.load(new AwsFeedEntryStatuses());
	}

	@Override
	public Tags getTags() {
		AwsTags t = mapper.load(new AwsTags());
		t.setService(this);
		return t;
	}

	public UpdateItemResult executeUpdate(int id, Map<String, AttributeValueUpdate> updates) {
		return executeUpdate(id, updates, ReturnValue.NONE, null);
	}

	public UpdateItemResult executeUpdate(int id, Map<String, AttributeValueUpdate> updates, int expectedVersion) {
		return executeUpdate(id, updates, ReturnValue.UPDATED_NEW,
				Collections.singletonMap("version", new ExpectedAttributeValue(value(expectedVersion))));
	}

	public UpdateItemResult executeUpdate(int id, Map<String, AttributeValueUpdate> updates, ReturnValue returnValue,
			Map<String, ExpectedAttributeValue> expected) {
		if (!updates.containsKey("version")) {
			updates = updates instanceof HashMap ? updates : new HashMap<>(updates);
			updates.put("version", add(value(1)));
		}

		return this.client
				.updateItem(new UpdateItemRequest().withTableName(TABLE_NAME).withKey(singletonMap("id", value(id)))
						.withAttributeUpdates(updates).withReturnValues(returnValue).withExpected(expected));
	}

	public Table getTable() {
		return table;
	}

	public AmazonDynamoDB getClient() {
		return client;
	}

	@Override
	public int versionOf(int id) {
		AttributeValue version = this.client.getItem(new GetItemRequest().withTableName(TABLE_NAME)
				.withAttributesToGet("version").withKey(Collections.singletonMap("id", value(id)))).getItem()
				.get("version");
		return version == null ? -1 : Integer.parseInt(version.getN());
	}

	@Override
	public void close() throws Exception {
	}
}
