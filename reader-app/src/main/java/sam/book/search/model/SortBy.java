package sam.book.search.model;

import static sam.book.search.model.ArticleMeta.ID;
import static sam.book.search.model.SortDir.DESC;

public enum SortBy {
	ADDED(ID, DESC), 
	TITLE(ArticleMeta.TITLE, DESC);
	
	public final String field;
	public final SortDir dir;
	
	private SortBy(String field, SortDir dir) {
		this.field = field;
		this.dir = dir;
	}
}
