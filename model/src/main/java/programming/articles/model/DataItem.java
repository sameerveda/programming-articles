package programming.articles.model;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.json.JSONObject;
import org.json.JSONWriter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonProperty;

@Table(name = ConstantDataItem.SQL_TABLE_NAME)
@DynamoDBTable(tableName = ConstantDataItem.TABLE_NAME)
public class DataItem extends ConstantDataItem implements Serializable {
	private static final long serialVersionUID = -6012413636015358469L;

	public static final String TAGS = "tags";
	public static final String STATUS = "status";
	public static final String NOTES = "notes";
	public static final String VERSION = "version";

	public static final List<String> DATAITEM_FIELDS = unmodifiableList(asList(TAGS, STATUS, NOTES, VERSION));
	public static final Set<String> UPDATEABLE_FIELDS = unmodifiableSet(new HashSet<>(asList(TAGS, STATUS, NOTES)));

	@Basic
	@JsonProperty
	protected String tags;
	@Column(length = 10)
	@Enumerated(EnumType.STRING)
	@DynamoDBTypeConvertedEnum
	@JsonProperty
	protected DataStatus status;
	@Column(length = 500)
	@JsonProperty
	protected String notes;
	@Basic
	@JsonProperty
	protected int version = 0;

	public DataItem() {
	}

	public DataItem(Map<String, AttributeValue> values) {
		super(values);
		set(values);
	}

	protected void set(Map<String, AttributeValue> values) {
		this.tags = opt(this.tags, values, TAGS);
		this.status = DataStatus.parse(opt(this.getStatus() == null ? null : this.getStatus().toString(), values, STATUS));
		this.notes = opt(this.notes, values, NOTES);
		AttributeValue version = values.get(VERSION);
		this.version = version == null || version.getN() == null ? -1 : Integer.parseInt(version.getN());
	}

	private static String opt(String defaultValue, Map<String, AttributeValue> values, String key) {
		AttributeValue v = values.get(key);
		return v == null ? defaultValue : v.getS();
	}

	public DataItem(JSONObject json) {
		super(json);
		this.tags = json.optString(TAGS);
		this.status = json.optEnum(DataStatus.class, STATUS);
		this.notes = json.optString(NOTES);
		this.version = json.optInt(VERSION);
	}

	public void write(JSONWriter w) {
		super.write(w);
		w.key(TAGS).value(tags).key(STATUS).value(status).key(NOTES).value(notes).key(VERSION).value(version);
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public DataStatus getStatus() {
		return status;
	}

	public void setStatus(DataStatus status) {
		this.status = status;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
