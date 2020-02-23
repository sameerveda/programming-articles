package sam.api;

import java.util.List;

import programming.articles.model.Tag;

public interface TagsAdder {
	List<Tag> open(List<Tag> initialTags); 
}
