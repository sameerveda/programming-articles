package programming.articles.model;

import static programming.articles.model.DataItemMeta.ADDED_ON;
import static programming.articles.model.DataItemMeta.DATA_TABLE_NAME;
import static programming.articles.model.DataItemMeta.DYNAMO_TABLE_NAME;
import static programming.articles.model.DataItemMeta.FAVICON;
import static programming.articles.model.DataItemMeta.ID;
import static programming.articles.model.DataItemMeta.NOTES;
import static programming.articles.model.DataItemMeta.REDIRECT;
import static programming.articles.model.DataItemMeta.SOURCE;
import static programming.articles.model.DataItemMeta.STATUS;
import static programming.articles.model.DataItemMeta.TAGS;
import static programming.articles.model.DataItemMeta.TITLE;
import static programming.articles.model.DataItemMeta.UPDATED_ON;
import static programming.articles.model.DataItemMeta.VERSION;
import static sam.full.access.dynamodb.DynamoConnection.optString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonProperty;

import sam.sql.QueryUtils;

@DynamoDBTable(tableName = DYNAMO_TABLE_NAME)
public class DataItem {
	public static final List<String> COLUMNS = Collections.unmodifiableList(
			Arrays.asList(ID, TITLE, SOURCE, REDIRECT, TAGS, FAVICON, ADDED_ON, STATUS, NOTES, UPDATED_ON, VERSION));
	
	public static final List<String> UPDATEABLE_FIELDS = Collections.unmodifiableList(Arrays.asList(TAGS, STATUS, NOTES, UPDATED_ON));

	@JsonProperty
	@DynamoDBHashKey
	protected int id;
	@JsonProperty protected String title;
	@JsonProperty protected String source;
	@JsonProperty protected String redirect;
	@JsonProperty protected String tags;
	@JsonProperty protected String favicon;
	@JsonProperty protected String addedOn;
	@JsonProperty 
	@DynamoDBTypeConvertedEnum 
	protected DataStatus status;
	@JsonProperty protected String notes;
	@JsonProperty protected String updatedOn;
	@JsonProperty protected int version;

	public DataItem() {}
	
	public DataItem(Map<String, AttributeValue> values) {
		set(values);
	}

	protected void set(Map<String, AttributeValue> values) {
		this.id = Short.parseShort(values.get(ID).getN());
		this.title = values.get(TITLE).getS();
		this.source = values.get(SOURCE).getS();
		this.redirect = optString(values, REDIRECT);
		this.favicon = optString(values, FAVICON);
		this.addedOn = values.get(ADDED_ON).getS();
		this.updatedOn = optString(values, UPDATED_ON);
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

	public DataItem(SQLiteStatement rs, IntFunction<String> dateMap, IntFunction<String> faviconMap) throws SQLiteException {
		int n = 0;
		this.id = (short)rs.columnInt(n++); // ID
		this.title = rs.columnString(n++); // TITLE
		this.source = rs.columnString(n++); // SOURCE
		this.redirect = rs.columnString(n++); // REDIRECT
		this.tags = rs.columnString(n++); // TAGS
		this.favicon = faviconMap.apply(rs.columnInt(n++)); // FAVICON
		this.addedOn = dateMap.apply(rs.columnInt(n++)); // ADDEDON
		this.status = DataStatus.parse(rs.columnString(n++)); // STATUS
		this.notes = rs.columnString(n++); // NOTES
		this.updatedOn = dateMap.apply(rs.columnInt(n++)); // UPDATEDON
		this.version = rs.columnInt(n++); // VERSION
	}

	public DataItem(short id, String title, String source, String redirect, String tags, String favicon, String addedOn, DataStatus status, String notes, String updatedOn, int version) {
		this.id = id;
		this.title = title;
		this.source = source;
		this.redirect = redirect;
		this.tags = tags;
		this.favicon = favicon;
		this.addedOn = addedOn;
		this.status = status;
		this.notes = notes;
		this.updatedOn = updatedOn;
		this.version = version;
	}


	public int getId(){ return this.id; }
	public void setId(int id){ this.id = id; }

	public String getTitle(){ return this.title; }
	public void setTitle(String title){ this.title = title; }

	public String getSource(){ return this.source; }
	public void setSource(String source){ this.source = source; }

	public String getRedirect(){ return this.redirect; }
	public void setRedirect(String redirect){ this.redirect = redirect; }

	public String getTags(){ return this.tags; }
	public void setTags(String tags){ this.tags = tags; }

	public String getFavicon(){ return this.favicon; }
	public void setFavicon(String favicon){ this.favicon = favicon; }

	public String getAddedOn(){ return this.addedOn; }
	public void setAddedOn(String addedOn){ this.addedOn = addedOn; }

	public DataStatus getStatus(){ return this.status; }
	public void setStatus(DataStatus status){ this.status = status; }

	public String getNotes(){ return this.notes; }
	public void setNotes(String notes){ this.notes = notes; }

	public String getUpdatedOn(){ return this.updatedOn; }
	public void setUpdatedOn(String updatedOn){ this.updatedOn = updatedOn; }

	public int getVersion(){ return this.version; }
	public void setVersion(int version){ this.version = version; }


	public static final String SELECT_SQL = "SELECT " + String.join(",", COLUMNS) + " FROM " + DATA_TABLE_NAME;
	public static final String INSERT_SQL = QueryUtils.insertSQL(DATA_TABLE_NAME, COLUMNS);

	public void insert(SQLiteStatement p, ToIntFunction<String> dates, ToIntFunction<String> favicons) throws SQLiteException {
		int n = 1;
		p.bind(n++, id);
		p.bind(n++, title);
		p.bind(n++, source);
		p.bind(n++, redirect);
		p.bind(n++, tags);
		p.bind(n++, favicons.applyAsInt(favicon));
		p.bind(n++, dates.applyAsInt(addedOn));
		p.bind(n++, status == null ? null : status.toString());
		p.bind(n++, notes);
		p.bind(n++, dates.applyAsInt(updatedOn));
		p.bind(n++, version);
	}

}
