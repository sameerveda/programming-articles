package programming.articles.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import com.carrotsearch.hppc.predicates.ShortPredicate;

import programming.articles.api.DataItemPagination;
import programming.articles.api.StateManager;
import programming.articles.model.DataItem;
import programming.articles.model.DataStatus;
import sam.collection.ArraysUtils;

public class DefaultDataItemPagination implements DataItemPagination {
	private final short[] allIds;
	private int pageSize = -1;
	private short startingId = -1;
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
	
	public void setStartingId(short startingId) {
		this.startingId = startingId;
	}
	
	@Override
	public ShortPredicate getFilter() {
		if(status == null)
			return (s -> true);
		byte expected = status.byteValue();
		return (s -> stateManager.getStatusOrdinal(s) == expected);
	}
	
	@Override
	public  void getData(BiConsumer<List<DataItem>, Exception> onResult) {
		stateManager.loadItems(data(), onResult);
	}
	
	private short[] data() {
		if((startingId < 0 && page < 0) || pageSize <= 0)
			throw new IllegalStateException(String.format("startingId(%s) < 0 && page(%s) < 0) || pageSize(%s) <= 0", startingId, page, pageSize));

		int skip = page > 0 ? skip() : 0;
		if(skip < 0)
			throw new IllegalStateException("small skip value");
		
		short[] list = new short[pageSize];
		int start = 0;
		if(startingId >= 0) {
			while(allIds[start++] != startingId) {}
		}
		if(start == allIds.length)
			throw new IllegalArgumentException("startingId not found: "+startingId);
		
		int skipped = 0;
		int size = 0;
		
		ShortPredicate filter = getFilter();
		
		while(start < allIds.length) {
			short s = allIds[start++];
			if(filter.apply(s)) {
				if(++skipped > skip) {
					list[size++] = s;
					if(size == pageSize)
						break;
				}
			}
		}
		
		return size < pageSize ? Arrays.copyOf(list, size) : list;
	}

	@Override
	public  List<DataItem> getData() throws Exception {
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
