package sam.rss.articles.aws.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;

import sam.rss.articles.aws.service.AwsFeedService;
import sam.rss.articles.aws.utils.IntArrayConverter;
import sam.rss.articles.model.FeedEntryIds;

@DynamoDBTable(tableName = AwsFeedService.TABLE_NAME)
public class AwsFeedEntryIds extends FeedEntryIds {
	private static final long serialVersionUID = 1L;

	@DynamoDBHashKey
	@Override
	public int getId() {
		return super.getId();
	}

	@DynamoDBTypeConverted(converter=IntArrayConverter.class)
	@DynamoDBAttribute
	@Override
	public int[] getData() {
		return super.getData();
	}

	@DynamoDBAttribute(attributeName="max_id")
	@Override
	public int getMaxId() {
		return super.getMaxId();
	}
	
	@DynamoDBAttribute
	@Override
	public int getVersion() {
		return super.getVersion();
	}
}
