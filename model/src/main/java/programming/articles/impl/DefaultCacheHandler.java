package programming.articles.impl;

import static java.util.Collections.EMPTY_MAP;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import programming.articles.api.CacheHandler;
import sam.reference.ReferenceUtils;

public class DefaultCacheHandler<K, V> implements CacheHandler<K, V> {
	protected Map<K, WeakReference<V>> map = new HashMap<>();
	protected CacheHandler<K, V> store = CacheHandler.empty();


	public void setMap(Map<K, WeakReference<V>> map) {
		this.map = Objects.requireNonNull(map);
	}

	public void setStore(CacheHandler<K, V> store) {
		this.store = Objects.requireNonNull(store);
	}

	public Map<K, WeakReference<V>> getMap() {
		return map;
	}

	@Override
	public V get(K key) {
		if(key == null)
			return null;

		ensureState();

		V v = ReferenceUtils.get(map.get(key));
		if(v != null)
			return v;
		return store.get(key);
	}

	private void ensureState() {
		if(map == null && store == EMPTY_MAP)
			throw new IllegalStateException();
	}

	@Override
	public void put(K key, V value) {
		Objects.requireNonNull(key);
		ensureState();

		if(value == null) {
			remove(key);
		} else {
			if(map != EMPTY_MAP)
				map.put(key, new WeakReference<>(value));
			store.put(key, value);
		}
	}

	@Override
	public void remove(K key) {
		map.remove(key);
		store.remove(key);
	}
}
