package programming.articles.api;

import java.util.List;
import java.util.function.BiConsumer;

import com.carrotsearch.hppc.predicates.ShortPredicate;

import programming.articles.model.DataStatus;
import programming.articles.model.dynamo.ConstantDataItem;

public interface ConstantDataItemPagination {
	int getPage();
	int getPageSize();
	void setPage(int page);
	void setStatus(DataStatus status);
	DataStatus getStatus();
	void getData(BiConsumer<List<ConstantDataItem>, Exception> onResult);
	List<ConstantDataItem> getData() throws Exception;
	int skip();
	int size();
	void setPageSize(int pageSize);
	ShortPredicate getFilter();
}