package sam.rssowl;

import static programming.articles.model.DataItemMeta.DATA_TABLE_NAME;
import static programming.articles.model.DataItemMeta.DYNAMO_TABLE_NAME;

import java.util.Map;
import java.util.function.IntFunction;

import javax.persistence.Table;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.carrotsearch.hppc.IntObjectScatterMap;

import programming.articles.model.DataItem;
import programming.articles.model.DataStatus;

@Table(name = DATA_TABLE_NAME)
@DynamoDBTable(tableName = DYNAMO_TABLE_NAME)
public class Mutableitem extends DataItem {
	public Mutableitem() {
	}

	public Mutableitem(Map<String, AttributeValue> values) {
		super(values);
	}

	public Mutableitem(short id, String title, String source, String redirect, String tags, String favicon,
			String addedOn, DataStatus status, String notes, String updatedOn, int version) {
		super(id, title, source, redirect, tags, favicon, addedOn, status, notes, updatedOn, version);
	}

	public Mutableitem(SQLiteStatement rs, IntFunction<String> dateMap, IntFunction<String> faviconMap)
			throws SQLiteException {
		super(rs, dateMap, faviconMap);
	}

	public Mutableitem(short id) {
		this.id = id;
	}

	public Mutableitem(SQLiteStatement rs, IntObjectScatterMap<String> dates, IntObjectScatterMap<String> favicons) throws SQLiteException {
		super(rs, dates::get, favicons::get);
	}

	public void setId(short id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}

	public void setFavicon(String favicon) {
		this.favicon = favicon;
	}

	public void setAddedOn(String addedOn) {
		this.addedOn = addedOn;
	}
}
