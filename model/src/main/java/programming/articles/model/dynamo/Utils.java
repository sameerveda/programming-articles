package programming.articles.model.dynamo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.amazonaws.services.dynamodbv2.datamodeling.KeyPair;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.ShortArrayList;
import com.carrotsearch.hppc.ShortByteMap;
import com.carrotsearch.hppc.ShortByteScatterMap;
import com.carrotsearch.hppc.ShortObjectMap;
import com.carrotsearch.hppc.ShortObjectScatterMap;
import com.carrotsearch.hppc.cursors.ShortObjectCursor;
import com.carrotsearch.hppc.procedures.ShortByteProcedure;
import com.carrotsearch.hppc.procedures.ShortProcedure;

import sam.myutils.Checker;
import sam.myutils.HPPCUtils;

interface Utils {

	public static ByteBuffer buf(int size, Consumer<ByteBuffer> filler) {
		ByteBuffer buf = ByteBuffer.allocate(size);
		filler.accept(buf);
		if (buf.hasRemaining())
			throw new IllegalStateException();
		buf.flip();
		return buf;
	}

	public static ByteBuffer buf(ShortByteMap map) {
		return buf(map.size() * (Short.BYTES + Byte.BYTES),
				buf -> map.forEach((ShortByteProcedure) ((s, t) -> buf.putShort(s).put(t))));
	}

	public static ByteBuffer buf(IntIntMap map) {
		return buf(map.size() * 2 * Integer.BYTES, buf -> HPPCUtils.forEach(map, (s, t) -> buf.putInt(s).putInt(t)));
	}

	public static ByteBuffer buf(short[] array) {
		return buf(array.length * Short.BYTES, buf -> {
			for (short n : array)
				buf.putShort(n);
		});
	}

	public static ByteBuffer buf(int[] array) {
		return buf(array.length * Short.BYTES, buf -> {
			for (int n : array)
				buf.putInt(n);
		});
	}
	
	public static ByteBuffer buf(ShortArrayList list) {
		return buf(list.size() * Short.BYTES, buf -> list.forEach((ShortProcedure)buf::putShort));
	}

	public static ByteBuffer buf(IntArrayList list) {
		return buf(list.size() * Integer.BYTES, buf -> HPPCUtils.forEach(list, buf::putInt));
	}

	public static KeyPair key(String key) {
		return new KeyPair().withHashKey(key);
	}

	public static ByteBuffer buf(ShortObjectMap<String> tags) {
		OS buf = new OS();
		try (OutputStreamWriter osw = new OutputStreamWriter(new GZIPOutputStream(buf))) {
			for (ShortObjectCursor<String> n : tags)
				osw.append(Short.toString(n.key)).append('\t').append(Objects.requireNonNull(n.value)).append('\n');
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		buf.buf.flip();
		return buf.buf;
	}

	public static ShortObjectMap<String> parseTags(Meta meta) {
		List<ByteBuffer> list = join(meta);
		int remaining = remaining(list);

		if (remaining == 0)
			return null;

		ShortObjectMap<String> map = new ShortObjectScatterMap<String>();

		for (ByteBuffer buf : list) {
			try (GZIPInputStream gis = new GZIPInputStream(
					new ByteArrayInputStream(buf.array(), buf.position(), buf.remaining()));
					BufferedReader reader = new BufferedReader(new InputStreamReader(gis))) {
				reader.lines().forEach(s -> {
					int n = s.indexOf('\t');
					if (n < 0) {
						if (!s.isEmpty())
							throw new IllegalArgumentException("bad tag line: " + s);
					} else {
						map.put(Short.parseShort(s.substring(0, n)), s.substring(n + 1));
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return map;
	}

	public static List<ByteBuffer> join(Meta meta) {
		if (meta == null)
			return Collections.emptyList();
		if (meta.compressedData == null)
			return meta.sparseData;
		if (meta.sparseData == null)
			return Collections.singletonList(meta.compressedData);

		meta.sparseData.add(0, meta.compressedData);
		return meta.sparseData;
	}

	public static interface Reader {
		void init(int byteCount);
		void next(ByteBuffer buf);
		Object result();
	}

	@SuppressWarnings("unchecked")
	public static <E> E read(Meta meta, Reader reader) {
		List<ByteBuffer> list = join(meta);
		int remaining = remaining(list);
		if(remaining == 0)
			return null;

		reader.init(remaining);
		for (ByteBuffer buf : list) {
			while(buf.hasRemaining())
				reader.next(buf);
		}

		return (E) reader.result();
	}

	public static int remaining(List<ByteBuffer> list) {
		return Checker.isEmpty(list) ? 0 : list.stream().filter(Objects::nonNull).mapToInt(Buffer::remaining).sum();
	}

	public static short[] readShorts(Meta meta) {
		return read(meta, new Reader() {
			short[] sink;
			int n = 0;

			@Override
			public void init(int byteCount) {
				sink = new short[byteCount/Short.BYTES];
			}

			@Override
			public void next(ByteBuffer buf) {
				sink[n++] = buf.getShort();
			}

			@Override
			public Object result() {
				if(sink.length != n)
					throw new IllegalStateException();
				return sink;
			}
		});
	}

	public static ShortByteScatterMap readStatus(Meta meta) {
		return read(meta, new Reader() {
			ShortByteScatterMap map;
			int size;

			@Override
			public Object result() {
				if(map.size() > size)
					throw new IllegalStateException(String.format("map.size(%s) > size(%s)", map.size(), size));
				return map;
			}

			@Override
			public void next(ByteBuffer buf) {
				map.put(buf.getShort(), buf.get());
			}

			@Override
			public void init(int byteCount) {
				this.size = byteCount / (Short.BYTES + Byte.BYTES);
				map = new ShortByteScatterMap(size);
			}
		});
	}
}
