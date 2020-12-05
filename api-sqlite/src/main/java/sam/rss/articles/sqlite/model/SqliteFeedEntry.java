package sam.rss.articles.sqlite.model;

import sam.rss.articles.aws.service.SqliteFeedService;
import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.utils.FeedEntryStatus;

public class SqliteFeedEntry extends FeedEntry {
	private static final long serialVersionUID = 1L;

	private SqliteFeedService service;
	

	public SqliteFeedEntry() {
		super();
	}

	public SqliteFeedEntry(int id, String title, String link, String redirect, String tags, FeedEntryStatus status,
			byte[] summary, String notes, int updatedOn, int publishedOn, int version) {
		super(id, title, link, redirect, tags, status, summary, notes, updatedOn, publishedOn, version);
	}

	private void notAllowed() {
		if (service != null)
			throw new IllegalAccessError();
	}

	private void updateValue(String field, String value) {
		if (service != null)
			service.executeUpdate(this.getId(), field, value);
	}

	private void updateValue(String field, long value) {
		if (service != null)
			service.executeUpdate(this.getId(), field, value);
	}

	public void setService(SqliteFeedService service) {
		this.service = service;
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
	
	@Override
	public void setStatus(FeedEntryStatus status) {
		updateValue("status", status.toString());
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
	
	@Override
	public void setUpdatedOn(long updatedOn) {
		updateValue("updated_on", updatedOn);
		super.setUpdatedOn(updatedOn);
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
}
