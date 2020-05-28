package programming.articles.api;

import java.util.List;
import java.util.function.BiConsumer;

import com.carrotsearch.hppc.predicates.ShortPredicate;

import programming.articles.model.DataItem;
import programming.articles.model.DataStatus;

public interface DataItemPagination {
	int getPage();
	int getPageSize();
	void setPage(int page);
	void setStatus(DataStatus status);
	DataStatus getStatus();
	 void getData(BiConsumer<List<DataItem>, Exception> onResult);
	 List<DataItem> getData() throws Exception;
	int size();
	void setPageSize(int pageSize);
	ShortPredicate getFilter();
	int skip();
}