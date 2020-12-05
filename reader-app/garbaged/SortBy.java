package sam.article.reader.model;

import static sam.article.reader.model.ArticleMeta.ID;
import static sam.article.reader.model.SortDir.ASC;
import static sam.article.reader.model.SortDir.DESC;

public enum SortBy {
	ADDED(ID, DESC), 
	TITLE(ArticleMeta.TITLE, ASC);
	
	public final String field;
	public final SortDir dir;
	
	private SortBy(String field, SortDir dir) {
		this.field = field;
		this.dir = dir;
	}
}
