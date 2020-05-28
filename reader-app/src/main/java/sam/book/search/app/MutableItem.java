package sam.book.search.app;

import java.util.function.BiPredicate;

import javafx.beans.property.SimpleObjectProperty;

public class MutableItem<T> extends SimpleObjectProperty<T> {
	private T old;
	private final BiPredicate<T, T> predicate;
	
	public MutableItem(BiPredicate<T, T> predicate) {
		this.predicate = predicate;
	}
	
	public boolean isChanged() {
		return this.get() != null && !predicate.test(old, this.get());
	}

	public T getOld() {
		return old;
	}

	public void setOld(T t) {
		this.old = t;
	} 
}
