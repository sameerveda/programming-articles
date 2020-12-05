package sam.rss.articles.model;

import java.io.IOException;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import sam.rss.articles.utils.FeedEntryStatus;
import sam.rss.articles.utils.Utils;

public class FeedEntry implements Serializable, JSONString {
	private static final long serialVersionUID = 1L;

	private int id;
	private String title;
	private String link;
	private String redirect;
	private String tags;
	private FeedEntryStatus status;
	private byte[] summary;
	private String notes;
	private long updatedOn;
	private long publishedOn;
	private int version;
	
	public FeedEntry() {}

	public FeedEntry(int id, String title, String link, String redirect, String tags, FeedEntryStatus status,
			byte[] summary, String notes, long updatedOn, long publishedOn, int version) {
		super();
		this.id = id;
		this.title = title;
		this.link = link;
		this.redirect = redirect;
		this.tags = tags;
		this.status = status;
		this.summary = summary;
		this.notes = notes;
		this.updatedOn = updatedOn;
		this.publishedOn = publishedOn;
		this.version = version;
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getRedirect() {
		return redirect;
	}

	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public FeedEntryStatus getStatus() {
		return status;
	}
	
	/**
	 * @return status == null ? FeedEntryStatus.UNREAD : status
	 */
	public FeedEntryStatus getStatusNonNull() {
		return status == null ? FeedEntryStatus.UNREAD : status;
	}

	public void setStatus(FeedEntryStatus status) {
		this.status = status;
	}

	public byte[] getSummary() {
		return summary;
	}

	public String getSummaryAsString() {
		try {
			return Utils.unGzipString(summary);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setSummary(byte[] summary) {
		this.summary = summary;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public long getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(long updatedOn) {
		this.updatedOn = updatedOn;
	}

	public long getPublishedOn() {
		return publishedOn;
	}

	public void setPublishedOn(long publishedOn) {
		this.publishedOn = publishedOn;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
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
		return obj instanceof FeedEntry && ((FeedEntry) obj).id == this.id;
	}

	@Override
	public String toString() {
		return toJson().toString(2);
	}
	
	public JSONObject toJson() {
		JSONObject o = new JSONObject();
		o.put("id", id);
		o.put("title", title);
		o.put("link", link);
		o.put("redirect", redirect);
		o.put("tags", tags);
		o.put("status", status);
		o.put("summary", getSummaryAsString());
		o.put("notes", notes);
		o.put("updatedOn", updatedOn);
		o.put("publishedOn", publishedOn);
		o.put("version", version);
		return o;
	}

	@Override
	public String toJSONString() {
		return toJson().toString();
	}
}
