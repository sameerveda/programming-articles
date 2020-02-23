package programming.articles.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import programming.articles.model.DataStatus;
import programming.articles.model.dynamo.ConstantDataItem;
import programming.articles.model.dynamo.DataItem;

class Item extends DataItem implements Serializable {
	private static final long serialVersionUID = 8980322081100311801L;

	transient Map<String, String> updates;
	
	public Item(Map<String, AttributeValue> values) {
		super(values);
	}

	public Item(Map<String, AttributeValue> values, ConstantDataItem item) {
		this.id = item.getId();
		this.title = item.getTitle();
		this.source = item.getSource();
		this.redirect = item.getRedirect();
		this.favicon = item.getFavicon();
		this.addedOn = item.getAddedOn();

		set(values);
	}

	@Override
	public DataStatus getStatus() {
		return status == null ? DataStatus.UNREAD : status;
	}
	
	@Override
	public void set(Map<String, AttributeValue> values) {
		super.set(values);
	}

	@Override
	public void setStatus(DataStatus status) {
		if (status == null)
			status = DataStatus.UNREAD;
		if (this.getStatus() == status)
			return;

		this.status = status;
		update(STATUS, status == null ? null : status.toString());
	}

	@Override
	public void setTags(String tags) {
		if (!Objects.equals(this.tags, tags)) {
			this.update(TAGS, tags);
			this.tags = tags;
		}
	}

	private void update(String field, String value) {
		if (this.updates == null)
			this.updates = new HashMap<>();
		updates.put(field, value);
	}

	@Override
	public void setNotes(String notes) {
		if (!Objects.equals(this.notes, notes)) {
			update(NOTES, notes);
			this.notes = notes;
		}
	}
}
