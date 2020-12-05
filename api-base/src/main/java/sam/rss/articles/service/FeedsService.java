package sam.rss.articles.service;


import java.util.List;

import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.model.FeedEntryIds;
import sam.rss.articles.model.FeedEntryStatuses;
import sam.rss.articles.model.Tags;

public interface FeedsService extends AutoCloseable {
	FeedEntry getEntry(int id);
	List<FeedEntry> getEntries(int[] ids);
	FeedEntryIds getIds();
	FeedEntryStatuses getStatueses();
	Tags getTags();
	int versionOf(int id);
}
