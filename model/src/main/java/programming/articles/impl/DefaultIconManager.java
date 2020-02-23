package programming.articles.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.image.Image;
import programming.articles.api.CacheHandler;
import programming.articles.api.IconManager;
import sam.internetutils.InternetUtils;
import sam.io.IOUtils;
import sam.myutils.Checker;
import sam.reference.ReferenceUtils;

public class DefaultIconManager implements IconManager {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Set<String> notFound = Collections.synchronizedSet(new HashSet<>());
	

	private byte[] buffer;
	private ByteArrayOutputStream download_sink;
	private ExecutorService iconThread;
	
	private final Map<String, WeakReference<Image>> ramCache = new ConcurrentHashMap<>();
	private final CacheHandler<String, byte[]> fileCache;
	
	public DefaultIconManager(CacheHandler<String, byte[]> fileCache) {
		this.fileCache = fileCache;
	}
	
	private static final Object NOT_FOUND = new Object();
	
	@Override
	public Image getIcon(String url) {
		Object o = fromCache(url);
		return o == null || o == NOT_FOUND ? null : (Image)o;
	}
	
	private Object fromCache(String url) {
		if (url == null)
			return null;

		if(notFound.contains(url))
			return NOT_FOUND;
		
		Image img = ReferenceUtils.get(ramCache.get(url));
		if(img != null)
			return img;

		return putRAMCache(url, fileCache.get(url));
	}

	private Image putRAMCache(String url, byte[] icon) {
		if(icon != null) {
			Image img = new Image(new ByteArrayInputStream(icon));
			ramCache.put(url, new WeakReference<>(img));
			return img;
		} else {
			return null;
		}
	}

	@Override
	public void loadIcon(String url, BiConsumer<Image, Throwable> onResult) {
		if (Checker.isEmptyTrimmed(url)) {
			onResult.accept(null, null);
			return;
		}

		Object m = fromCache(url);
		if(m != null) {
			onResult.accept(m == NOT_FOUND ? null : (Image)m, null);
			return;
		}

		if (iconThread == null) {
			iconThread = Executors.newSingleThreadExecutor();
			download_sink = new ByteArrayOutputStream();
			buffer = new byte[4 * 1024];
		}

		iconThread.execute(() -> {
			synchronized (download_sink) {
				Image img = ReferenceUtils.get(ramCache.get(url));
				if(img != null) {
					onResult.accept(img, null);
					return;
				}
						
				try {
					download_sink.reset();
					IOUtils.pipe(InternetUtils.connection(url).getInputStream(), download_sink, buffer);

					if (download_sink.size() == 0)
						throw new IllegalStateException("empty icon");

					byte[] bytes = download_sink.toByteArray();
					logger.debug("downloaded icon: ({}) bytes from: {}", bytes.length, url);
					fileCache.put(url, bytes);
					onResult.accept(putRAMCache(url, bytes), null);
				} catch (Throwable e) {
					if(e instanceof InterruptedException)
						return;

					logger.debug("failed load icon: {}: {} ", url, e.toString());
					notFound.add(url);
					onResult.accept(null, e);
				}
			}
		});
	}

	@Override
	public void close() throws InterruptedException {
		if(iconThread != null) {
			iconThread.shutdownNow();
			iconThread.awaitTermination(2, TimeUnit.HOURS);
		}
	}
}
