package sam.rss.articles.aws.model;

import java.util.Collections;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;

import sam.rss.articles.aws.service.AwsFeedService;
import sam.rss.articles.model.Tags;

@DynamoDBTable(tableName = AwsFeedService.TABLE_NAME)
public class AwsTags extends Tags {
	private static final long serialVersionUID = 1L;
	
	private AwsFeedService service;
	
	public void setService(AwsFeedService service) {
		this.service = service;
	}
	
	private void notAllowed() {
		if (service != null)
			throw new IllegalAccessError();
	}
	

	@DynamoDBHashKey
	@Override
	public int getId() {
		return super.getId();
	}

	@DynamoDBAttribute
	@Override
	public Map<String, String> getData() {
		return Collections.unmodifiableMap(super.getData());
	}

	@Override
	public int putNewTag(String value) {
		try {
			return putNewTag0(value);
		} catch (ConditionalCheckFailedException e) {
			super.reset(this.service.getTags());
			System.out.println("WARN: failed to put tag: "+ value + ", retry after update");
			return putNewTag0(value);
		}
	}

	private int putNewTag0(String value) {
		UpdateItemOutcome res = this.service.getTable()
				.updateItem(
						new UpdateItemSpec()
						.withPrimaryKey("id", this.getId())
						.withUpdateExpression("SET #data.#key=:value, max_id=:max_id ADD #version :increament")
						.withConditionExpression("#version=:version AND attribute_not_exists(#data.#key)")
						.withNameMap(new NameMap().with("#data", "data").with("#version", "version").with("#key", Integer.toString(getMaxId() + 1)))
						.withValueMap(new ValueMap().with(":version", this.getVersion()).with(":value", value).with(":increament", 1).with(":max_id", getMaxId() + 1))
						.withReturnValues(ReturnValue.UPDATED_NEW)
						);
		
		Item item = res.getItem();
		int maxId = item.getInt("max_id");
		super.setMaxId(maxId);
		super.putTag(maxId, value);
		super.setVersion(item.getInt("version"));
		return maxId;
	}
	
	@Override
	protected void putTag(int id, String value) {
		throw new IllegalAccessError();
	}

	@DynamoDBAttribute
	@Override
	public int getVersion() {
		return super.getVersion();
	}

	@DynamoDBAttribute(attributeName="max_id")
	@Override
	public int getMaxId() {
		return super.getMaxId();
	}

	@Override
	public void setData(Map<String, String> data) {
		notAllowed();
		super.setData(data);
	}

	@Override
	public void setType(String type) {
		notAllowed();
		super.setType(type);
	}

	@Override
	public void setVersion(int version) {
		notAllowed();
		super.setVersion(version);
	}

	@Override
	public void setMaxId(int maxId) {
		notAllowed();
		super.setMaxId(maxId);
	}
}
