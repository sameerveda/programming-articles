package programming.articles.api;

public interface ShortCacheHandler<V> extends CacheHandler<Short, V> {
	@SuppressWarnings("rawtypes")
	static ShortCacheHandler EMPTY = new ShortCacheHandler() {
		@Override public Object get(Object key) { return null; }
		@Override public void put(Object key, Object value) { }
		@Override public void remove(Object key) { }
	};
	
	@SuppressWarnings("unchecked")
	public static <T> ShortCacheHandler<T> empty() {
		return EMPTY;
	} 

	default V get(short key) {
		return this.get(Short.valueOf(key));
	}
	default void put(short key, V value) {
		this.put(Short.valueOf(key), value);
	}
}
