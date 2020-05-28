package sam.book.search.model;

import static sam.book.search.model.ArticleMeta.ADDEDON;
import static sam.book.search.model.ArticleMeta.DATA_TABLE_NAME;
import static sam.book.search.model.ArticleMeta.FAVICON;
import static sam.book.search.model.ArticleMeta.ID;
import static sam.book.search.model.ArticleMeta.NOTES;
import static sam.book.search.model.ArticleMeta.REDIRECT;
import static sam.book.search.model.ArticleMeta.SOURCE;
import static sam.book.search.model.ArticleMeta.STATUS;
import static sam.book.search.model.ArticleMeta.TAGS;
import static sam.book.search.model.ArticleMeta.TITLE;
import static sam.book.search.model.ArticleMeta.UPDATEDON;
import static sam.book.search.model.ArticleMeta.VERSION;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import java.util.List;
import java.util.Objects;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import sam.sql.QueryUtils;

public class Article {
	public static final List<String> COLUMNS = unmodifiableList(asList(ID, TITLE, SOURCE, REDIRECT, TAGS, FAVICON, ADDEDON, STATUS, NOTES, UPDATEDON, VERSION));

	public final int id;
	public final String title;
	public final String source;
	public final String redirect;
	public final String tags;
	public final int favicon;
	public final int addedOn;
	public final String status;
	public final String notes;
	public final int updatedOn;
	public final int version;
	
	private String new_tags;
	private String new_status;
	private String new_notes;

	public Article(SQLiteStatement rs) throws SQLiteException {
		int n = 0;
		this.id = rs.columnInt(n++); // ID
		this.title = rs.columnString(n++); // TITLE
		this.source = rs.columnString(n++); // SOURCE
		this.redirect = rs.columnString(n++); // REDIRECT
		this.tags = rs.columnString(n++); // TAGS
		this.favicon = rs.columnInt(n++); // FAVICON
		this.addedOn = rs.columnInt(n++); // ADDEDON
		this.status = rs.columnString(n++); // STATUS
		this.notes = rs.columnString(n++); // NOTES
		this.updatedOn = rs.columnInt(n++); // UPDATEDON
		this.version = rs.columnInt(n++); // VERSION
		
		this.new_tags = this.tags;
		this.new_status = this.status;
		this.new_notes = this.notes;
	}

	public Article(int id, String title, String source, String redirect, String tags, int favicon, int addedOn, String status,
			String notes, int updatedOn, int version) {
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

	@Override
	public String toString() {
		return "Article [id=" + id + ", title=" + title + ", source=" + source + ", redirect=" + redirect + ", tags="
				+ tags + ", favicon=" + favicon + ", addedOn=" + addedOn + ", status=" + status + ", notes=" + notes
				+ ", updatedOn=" + updatedOn + ", version=" + version + ", new_tags=" + new_tags + ", new_status="
				+ new_status + ", new_notes=" + new_notes + "]";
	}

	public boolean isChanged() {
		return 
				!Objects.equals(tags, new_tags) || 
				!Objects.equals(status, new_status) || 
				!Objects.equals(notes, new_notes) 
				;
	}

	public void setTags(String new_tags) {
		this.new_tags = new_tags;
	}

	public void setStatus(String new_status) {
		this.new_status = new_status;
	}

	public void setNotes(String new_notes) {
		this.new_notes = new_notes;
	}

	public static final String SELECT_SQL = "SELECT " + String.join(",", COLUMNS) + " FROM " + DATA_TABLE_NAME;
	public static final String INSERT_SQL = QueryUtils.insertSQL(DATA_TABLE_NAME, COLUMNS);

	void insert(SQLiteStatement p) throws SQLiteException {
		int n = 1;
		p.bind(n++, id);
		p.bind(n++, title);
		p.bind(n++, source);
		p.bind(n++, redirect);
		p.bind(n++, tags);
		p.bind(n++, favicon);
		p.bind(n++, addedOn);
		p.bind(n++, status);
		p.bind(n++, notes);
		p.bind(n++, updatedOn);
		p.bind(n++, version);
	}

	public String getStatus() {
		return new_status;
	}
	public String getTags() {
		return new_tags;
	}
}
