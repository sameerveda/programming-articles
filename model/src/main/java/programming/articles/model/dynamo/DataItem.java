package programming.articles.model.dynamo;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONWriter;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import programming.articles.model.DataStatus;

public class DataItem extends ConstantDataItem implements Serializable {
	private static final long serialVersionUID = -6012413636015358469L;
	
	public static final String TAGS = "tags";
	public static final String STATUS = "status";
	public static final String NOTES = "notes";
	public static final String VERSION = "version";

	public static final List<String> DATAITEM_FIELDS = unmodifiableList(asList(TAGS, STATUS, NOTES, VERSION));
	public static final Set<String> UPDATEABLE_FIELDS = unmodifiableSet(new HashSet<>(asList(TAGS, STATUS, NOTES)));

	protected  String tags;
	protected  DataStatus status;
	protected  String notes;
	protected  int version = -1;
	
	public DataItem() { }
	
	public DataItem(Map<String, AttributeValue> values) {
		super(values);
		set(values);
	}
	
	protected void set(Map<String, AttributeValue> values) {
		this.tags = opt(this.tags, values, TAGS);
		this.status = DataStatus.parse(opt(this.getStatus().toString(), values, STATUS));
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
		w
		.key(TAGS).value(tags)
		.key(STATUS).value(status)
		.key(NOTES).value(notes)
		.key(VERSION).value(version);
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
