package programming.articles.api;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface CacheHandler<K, V> extends Closeable {
	@SuppressWarnings("rawtypes")
	public static CacheHandler EMPTY =  new CacheHandler() {
		@Override public Object get(Object key) { return null; }
		@Override public void put(Object key, Object value) { }
		@Override public void remove(Object key) { }
	};
	
	@SuppressWarnings("unchecked")
	public static <S, T> CacheHandler<S, T> empty() {
		return EMPTY;
	}
	
	public static <S, T> CacheHandler<S, T> logged(CacheHandler<S, T> src, Class<?> cls) {
		return new CacheHandler<S, T>() {
			AtomicInteger get = new AtomicInteger(), put = new AtomicInteger(), remove = new AtomicInteger();
			@Override
			public T get(S key) {
				get.incrementAndGet();
				return src.get(key);
			}

			@Override
			public void put(S key, T value) {
				put.incrementAndGet();
				src.put(key, value);
			}

			@Override
			public void remove(S key) {
				remove.incrementAndGet();
				src.remove(key);
			}
			
			@Override
			public void close() {
				LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).debug("opertions get: {}, put: {}, remove: {}, for: {}", get.get(), put.get(), remove.get(), cls);
			}
			
		};
	}
	
	public static <S, T> CacheHandler<S, T> fromMap(Map<S, T> map) {
		return new CacheHandler<S, T>() {
			@Override
			public T get(S key) {
				return map.get(key);
			}
			@Override
			public void put(S key, T value) {
				map.put(key, value);
			}
			@Override
			public void remove(S key) {
				map.remove(key);
			}
		};
	} 
	
	V get(K key);
	void put(K key, V value);
	void remove(K key);
	@Override
	default void close() { }
}
