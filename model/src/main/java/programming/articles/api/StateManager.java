package programming.articles.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import programming.articles.model.Tag;
import programming.articles.model.dynamo.ConstantDataItem;
import programming.articles.model.dynamo.DataItem;

public interface StateManager extends AutoCloseable {
	Collection<Tag> allTagsNames();
	Tag getTagByName(String name);
	short[] allIds();
	Tag getTagById(short tagId);
	byte getStatusOrdinal(short itemId);
	
	void loadItems(short[] ids, BiConsumer<List<ConstantDataItem>, Exception> onResult);
	List<ConstantDataItem> loadItems(short[] ids) throws Exception;
	
	void commit(DataItem item, BiConsumer<Boolean, Exception> onResult);
	
	/**
	 *  
	 * @param item
	 * @return returns true if update else false
	 */
	
	boolean commit(DataItem item);
	/**
	 *  
	 * @param item
	 * @return returns version after update, or -1 if no update performed
	 */
	int commit(short id, Map<String, String> updates) ;
	
	DataItem getItem(short id);
	void getItem(short id, BiConsumer<DataItem, Exception> consumer);
	
	DataItem getItem(ConstantDataItem item);
	void getItem(ConstantDataItem item, BiConsumer<DataItem, Exception> consumer);
	void commitNewTags();
}
