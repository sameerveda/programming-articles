package sam.rss.articles.aws.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;

import sam.rss.articles.aws.service.AwsFeedService;
import sam.rss.articles.aws.utils.IntArrayConverter;
import sam.rss.articles.model.FeedEntryStatuses;

@DynamoDBTable(tableName = AwsFeedService.TABLE_NAME)
public class AwsFeedEntryStatuses extends FeedEntryStatuses {
	private static final long serialVersionUID = 1L;
	
	@DynamoDBHashKey
	@Override
	public int getId() {
		return super.getId();
	}

	@DynamoDBTypeConverted(converter=IntArrayConverter.class)
	@DynamoDBAttribute(attributeName="READ")
	@Override
	public int[] getRead() {
		return super.getRead();
	}
	
	@DynamoDBTypeConverted(converter=IntArrayConverter.class)
	@DynamoDBAttribute(attributeName="UNREAD")
	@Override
	public int[] getUnread() {
		return super.getUnread();
	}

	@DynamoDBTypeConverted(converter=IntArrayConverter.class)
	@DynamoDBAttribute(attributeName="LATER")
	@Override
	public int[] getLater() {
		return super.getLater();
	}

	@DynamoDBTypeConverted(converter=IntArrayConverter.class)
	@DynamoDBAttribute(attributeName="DELETED")
	@Override
	public int[] getDeleted() {
		return super.getDeleted();
	}

	@DynamoDBTypeConverted(converter=IntArrayConverter.class)
	@DynamoDBAttribute(attributeName="FAVORITE")
	@Override
	public int[] getFavorite() {
		return super.getFavorite();
	}
	
	@DynamoDBAttribute
	@Override
	public int getVersion() {
		return super.getVersion();
	}
}
