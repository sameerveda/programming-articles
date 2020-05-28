package sam.book.search.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import sam.book.search.model.ArticleStatus;
import sam.book.search.model.SortBy;

public enum Choices {
    TYPE(App.SHOW_ALL, new String[]{App.SHOW_ALL, App.BOOKMARK}),
    STATUS(ArticleStatus.UNREAD, ArticleStatus.values()),
    SORT_BY(SortBy.ADDED, SortBy.values());
    
    final Object defaultValue;
    final List<Object> allValues;
    
    private Choices(Object defaultValue, Object[] allValues) {
        this.defaultValue = defaultValue;
        List<Object> list = Arrays.asList(allValues);
        if(!list.contains(defaultValue)) {
            List<Object> temp = new ArrayList<>();
            temp.add(defaultValue);
            temp.addAll(list);
            list = temp;
        }
        this.allValues = Collections.unmodifiableList(list);
    }
    
}
