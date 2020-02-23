package programming.articles.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import com.carrotsearch.hppc.predicates.ShortPredicate;

import programming.articles.api.ConstantDataItemPagination;
import programming.articles.api.StateManager;
import programming.articles.model.DataStatus;
import programming.articles.model.dynamo.ConstantDataItem;
import sam.collection.ArraysUtils;

public class DefaultDataItemPagination implements ConstantDataItemPagination {
	private final short[] allIds;
	private int pageSize = -1;
	private int page = 0;
	private final StateManager stateManager;
	private DataStatus status;
	
	public DefaultDataItemPagination(StateManager stateManager) {
		this.allIds = stateManager.allIds();
		this.stateManager = stateManager;
		
		if(allIds.length > 2 && allIds[0] < allIds[1]) 
			ArraysUtils.reverse(allIds);
	}
	
	@Override
	public void setPageSize(int pageSize) {
		if(pageSize < 1)
			throw new IllegalArgumentException("invalid pageSize: "+pageSize);
		this.pageSize = pageSize;
	}
	
	@Override
	public int getPage() {
		return page;
	}
	
	@Override
	public int getPageSize() {
		return pageSize;
	}
	
	@Override
	public void setPage(int page) {
		if(pageSize < 0)
			throw new IllegalArgumentException("invalid page: "+page);
		
		this.page = page;
	}
	
	@Override
	public ShortPredicate getFilter() {
		if(status == null)
			return (s -> true);
		byte expected = status.byteValue();
		return (s -> stateManager.getStatusOrdinal(s) == expected);
	}
	
	@Override
	public void getData(BiConsumer<List<ConstantDataItem>, Exception> onResult) {
		stateManager.loadItems(data(), onResult);
	}
	
	private short[] data() {
		int skip = skip();
		if(skip < 0)
			throw new IllegalStateException("small skip value");
		
		short[] list = new short[pageSize];
		int skipped = 0;
		int index = 0;
		
		ShortPredicate filter = getFilter();
		
		for (short s : allIds) {
			if(filter.apply(s)) {
				if(++skipped > skip) {
					list[index++] = s;
					if(index == pageSize)
						break;
				}
			}
		}
		return index < pageSize ? Arrays.copyOf(list, index) : list;
	}

	@Override
	public List<ConstantDataItem> getData() throws Exception {
		return stateManager.loadItems(data());
	}

	@Override
	public int skip() {
		if(page < 0 || pageSize < 1)
			throw new IllegalStateException(String.format("page < %s || pageSize < %s", page, pageSize));
		return page * pageSize;
	}

	@Override
	public int size() {
		return allIds.length;
	}

	@Override
	public void setStatus(DataStatus status) {
		this.status = status;
	}

	@Override
	public DataStatus getStatus() {
		return status;
	}
}
