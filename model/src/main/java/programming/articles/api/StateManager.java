package programming.articles.api;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import programming.articles.model.DataItem;
import programming.articles.model.Tag;

public interface StateManager extends AutoCloseable {
	Collection<Tag> allTagsNames();
	Tag getTagByName(String name);
	short[] allIds();
	Tag getTagById(short tagId);
	byte getStatusOrdinal(short itemId);
	
	 void loadItems(short[] ids, BiConsumer<List<DataItem>, Exception> onResult);
	 List<DataItem> loadItems(short[] ids) throws Exception;
	
	void commit(DataItem item, BiConsumer<Boolean, Exception> onResult);
	
	/**
	 *  
	 * @param item
	 * @return returns true if update else false
	 */
	
	boolean commit(DataItem item);
	DataItem getItem(short id);
	void getItem(short id, BiConsumer<DataItem, Exception> consumer);
	void commitNewTags();
}
