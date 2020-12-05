package sam.rss.articles.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import sam.rss.articles.model.FeedEntry;

public final class Utils {
	private Utils() {
	}

	private static WeakReference<byte[]> wbuffer = new WeakReference<>(null);
	private static WeakReference<ByteArrayOutputStream> wbos = new WeakReference<>(null);

	public static byte[] unGzip(byte[] bytes) throws IOException {
		return decompress(bytes, bos -> bos.toByteArray());
	}

	public static String unGzipString(byte[] bytes) throws IOException {
		return decompress(bytes, bos -> {
			try {
				return bos.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static <T> T decompress(final byte[] bytes, Function<ByteArrayOutputStream, T> converter)
			throws IOException {
		if (bytes == null || bytes.length == 0)
			return null;

		synchronized (FeedEntry.class) {
			try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
				byte[] buffer = wbuffer.get();
				ByteArrayOutputStream bos = wbos.get();

				if (bos == null) {
					bos = new ByteArrayOutputStream();
					wbos = new WeakReference<ByteArrayOutputStream>(bos);
				}
				if (buffer == null) {
					buffer = new byte[1024];
					wbuffer = new WeakReference<byte[]>(buffer);
				}

				bos.reset();
				int n;
				while ((n = gis.read(buffer)) > 0)
					bos.write(buffer, 0, n);

				return converter.apply(bos);
			}
		}
	}

}
