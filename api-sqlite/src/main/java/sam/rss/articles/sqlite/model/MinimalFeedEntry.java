package sam.rss.articles.sqlite.model;

public class MinimalFeedEntry {
	public final int id;
	public final String title;
	
	public MinimalFeedEntry(int id, String title) {
		this.id = id;
		this.title = title;
	}
	
	@Override
	public String toString() {
		return title;
	}
}
