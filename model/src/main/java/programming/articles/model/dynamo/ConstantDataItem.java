package programming.articles.model.dynamo;

import static sam.full.access.dynamodb.DynamoConnection.optString;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import programming.articles.api.JsonWritable;

public class ConstantDataItem implements JsonWritable, Serializable {
	private static final long serialVersionUID = 4594811689425599481L;

	public static final String TABLE_NAME = "programming-articles-data";
	
	public static final String ID = "id";
	public static final String TITLE = "title";
	public static final String SOURCE = "source";
	public static final String REDIRECT = "redirect";
	public static final String FAVICON = "favicon";
	public static final String ADDED_ON = "addedOn";

	public static final List<String> CONSTANT_DATAITEM_FIELDS = Collections.unmodifiableList(Arrays.asList(ID, TITLE, SOURCE, REDIRECT, FAVICON, ADDED_ON));

	protected short id;
	protected String title;
	protected String source;
	protected String redirect;
	protected String favicon;
	protected String addedOn;
	
	public ConstantDataItem() { }
	
	public ConstantDataItem(Map<String, AttributeValue> values) {
		this.id = Short.parseShort(values.get(ID).getN());
		this.title = values.get(TITLE).getS();
		this.source = values.get(SOURCE).getS();
		this.redirect = optString(values, REDIRECT);
		this.favicon = optString(values, FAVICON);
		this.addedOn = values.get(ADDED_ON).getS();
	}
	
	public ConstantDataItem(JSONObject json) {
		this.id = (short) json.getInt(ID);
		this.title = json.getString(TITLE);
		this.source = json.getString(SOURCE);
		this.redirect = json.optString(REDIRECT);
		this.favicon = json.optString(FAVICON);
		this.addedOn = json.getString(ADDED_ON);
	}

	public void write(JSONWriter w) {
		w.key(ID).value(id)
		.key(TITLE).value(title)
		.key(SOURCE).value(source)
		.key(REDIRECT).value(redirect)
		.key(FAVICON).value(favicon)
		.key(ADDED_ON).value(addedOn);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		JSONWriter w = new JSONWriter(sb);
		w.object();
		write(w);
		w.endObject();
		return sb.toString();
	}

	public short getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getSource() {
		return source;
	}

	public String getRedirect() {
		return redirect;
	}

	public String getFavicon() {
		return favicon;
	}

	public String getAddedOn() {
		return addedOn;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return id == ((ConstantDataItem) obj).id;
	}
}
