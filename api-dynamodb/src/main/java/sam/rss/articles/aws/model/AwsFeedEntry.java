package sam.rss.articles.aws.model;

import static sam.rss.articles.aws.utils.AwsUtils.add;
import static sam.rss.articles.aws.utils.AwsUtils.delete;
import static sam.rss.articles.aws.utils.AwsUtils.numberSet;
import static sam.rss.articles.aws.utils.AwsUtils.set;
import static sam.rss.articles.aws.utils.AwsUtils.value;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

import sam.rss.articles.aws.service.AwsFeedService;
import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.utils.FeedEntryStatus;
import sam.rss.articles.utils.Ids;

@DynamoDBTable(tableName = AwsFeedService.TABLE_NAME)
public class AwsFeedEntry extends FeedEntry {
	private static final long serialVersionUID = 1L;

	private AwsFeedService service;
	
	public AwsFeedEntry() {}
	public AwsFeedEntry(int id) {
		this.setId(id);
	}

	private void notAllowed() {
		if (service != null)
			throw new IllegalAccessError();
	}
	
	private void updateValue(String field, AttributeValue value) {
		if (service == null) return;
		
		HashMap<String, AttributeValueUpdate> update = new HashMap<String, AttributeValueUpdate>();
		update.put(field, set(value));
		update.put("updated_on", set(value(System.currentTimeMillis()/1000)));
		
		UpdateItemResult result = service.executeUpdate(this.getId(), update, ReturnValue.UPDATED_NEW, null);
		System.out.println("UPDATE" + field + ", " + result);
		super.setUpdatedOn(Long.parseLong(result.getAttributes().get("updated_on").getN()));
	}

	private void updateValue(String field, String value) {
		updateValue(field, value(value));
	}

	private void updateValue(String field, Number value) {
		updateValue(field, value(value));
	}

	public void setService(AwsFeedService service) {
		this.service = service;
	}

	@DynamoDBHashKey
	@Override
	public int getId() {
		return super.getId();
	}

	@Override
	public void setId(int id) {
		notAllowed();
		super.setId(id);
	}

	@Override
	public void setTitle(String title) {
		notAllowed();
		super.setTitle(title);
	}

	@Override
	public void setLink(String link) {
		notAllowed();
		super.setLink(link);
	}

	@Override
	public void setRedirect(String redirect) {
		updateValue("redirect", redirect);
		super.setRedirect(redirect);
	}

	@Override
	public void setTags(String tags) {
		updateValue("tags", tags);
		super.setTags(tags);
	}

	@DynamoDBTypeConvertedEnum
	@Override
	public FeedEntryStatus getStatus() {
		return super.getStatus();
	}

	@Override
	public void setStatus(FeedEntryStatus status) {
		if(service != null) {
			Objects.requireNonNull(status);
			Map<String, AttributeValueUpdate> updates = new HashMap<>();
			updates.put("version", add(value(1)));
			
			AttributeValueUpdate delete = delete(numberSet(this.getId()));
			for (FeedEntryStatus s : FeedEntryStatus.values()) {
				if(s != status)
					updates.put(s.toString(), delete);
			}
			
			updates.put(status.toString(), add(numberSet(this.getId())));
			service.executeUpdate(Ids.ID_STATUS, updates);
			updateValue("status", status.toString());
		}
		super.setStatus(status);
	}

	@Override
	public void setSummary(byte[] summary) {
		notAllowed();
		super.setSummary(summary);
	}

	@Override
	public void setNotes(String notes) {
		updateValue("notes", notes);
		super.setNotes(notes);
	}

	@DynamoDBAttribute(attributeName = "updated_on")
	@Override
	public long getUpdatedOn() {
		return super.getUpdatedOn();
	}

	@Override
	public void setUpdatedOn(long updatedOn) {
		notAllowed();
		super.setUpdatedOn(updatedOn);
	}

	@DynamoDBAttribute(attributeName = "published_on")
	@Override
	public long getPublishedOn() {
		return super.getPublishedOn();
	}

	@Override
	public void setPublishedOn(long publishedOn) {
		updateValue("published_on", publishedOn);
		super.setPublishedOn(publishedOn);
	}

	@Override
	public void setVersion(int version) {
		notAllowed();
		super.setVersion(version);
	}

	@DynamoDBAttribute
	@Override
	public String getTitle() {
		return super.getTitle();
	}

	@DynamoDBAttribute
	@Override
	public String getLink() {
		return super.getLink();
	}

	@DynamoDBAttribute
	@Override
	public String getRedirect() {
		return super.getRedirect();
	}

	@DynamoDBAttribute
	@Override
	public String getTags() {
		return super.getTags();
	}

	@DynamoDBAttribute
	@Override
	public byte[] getSummary() {
		return super.getSummary();
	}

	@DynamoDBAttribute
	@Override
	public String getNotes() {
		return super.getNotes();
	}

	@DynamoDBAttribute
	@Override
	public int getVersion() {
		return super.getVersion();
	}

}
