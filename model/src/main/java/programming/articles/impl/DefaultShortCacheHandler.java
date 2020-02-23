package programming.articles.impl;

import java.lang.ref.WeakReference;

import com.carrotsearch.hppc.ShortObjectMap;
import com.carrotsearch.hppc.ShortObjectScatterMap;

import programming.articles.api.CacheHandler;
import programming.articles.api.ShortCacheHandler;
import sam.reference.ReferenceUtils;

public class DefaultShortCacheHandler<E> implements ShortCacheHandler<E> {
	private final ShortObjectMap<WeakReference<E>> map = new ShortObjectScatterMap<>();
	private CacheHandler<Short, E> cache;
	
	public DefaultShortCacheHandler(CacheHandler<Short, E> cache) {
		this.cache = cache;
	}
	
	@Override
	public E get(Short key) {
		return get((short)key);
	}

	@Override
	public void put(Short key, E value) {
		put((short)key, value);
	}

	@Override
	public E get(short key) {
		E e = ReferenceUtils.get(map.get(key));
		if(e != null)
			return e;
		e = cache.get(key);
		if(e != null)
			map.put(key, new WeakReference<>(e));
		return e;
	}

	@Override
	public void put(short key, E value) {
		if(value == null) {
			remove(key);
		} else {
			cache.put(key, value);
			map.put(key, new WeakReference<>(value));
		}
	}

	@Override
	public void remove(Short key) {
		map.remove(key);
		cache.remove(key);
	}
	
}
