package programming.articles.model;

import static programming.articles.model.Meta.ID;
import static programming.articles.model.Meta.SPARSE_DATA;
import static programming.articles.model.Meta.TABLE_NAME;
import static programming.articles.model.Utils.buf;
import static programming.articles.model.Utils.key;
import static programming.articles.model.Utils.readShorts;
import static programming.articles.model.Utils.readStatus;
import static sam.full.access.dynamodb.DynamoConnection.add;
import static sam.full.access.dynamodb.DynamoConnection.value;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.carrotsearch.hppc.ShortByteMap;
import com.carrotsearch.hppc.ShortObjectMap;
import com.carrotsearch.hppc.ShortObjectScatterMap;
import com.carrotsearch.hppc.cursors.ShortObjectCursor;

import sam.full.access.dynamodb.DynamoConnection;
import sam.functions.CallableWithIOException;
import sam.myutils.LoggerUtils;
import sam.myutils.MyUtilsException;

public class LoadedMetas implements Externalizable {
	private static final Logger logger = LoggerFactory.getLogger(LoadedMetas.class);

	private static final long serialVersionUID = -5364680198477460884L;

	public static final String IDS = "ids";
	public static final String IDS_STATUS = "ids-status";
	public static final String TAGS = "tags";
	public static final Set<String> VALID_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(IDS, IDS_STATUS, TAGS)));
	
	public static LoadedMetas load(DynamoConnection connection, LoadedMetas oldMetas) {
		return load(connection, VALID_KEYS, oldMetas);		
	}
	public static LoadedMetas load(DynamoConnection connection, Collection<String> idsToLoad, LoadedMetas oldMetas) {
		return load0(connection, idsToLoad, oldMetas, LoadedMetas.class);		
	}
	
	@SuppressWarnings("unchecked")
	public static <E extends LoadedMetas> E load0(DynamoConnection connection, Collection<String> idsToLoad, E oldMetas, Class<E> instanceClass) {
		if(idsToLoad.isEmpty())
			throw new IllegalArgumentException("no ids specified");

		if(idsToLoad != VALID_KEYS) {
			idsToLoad.forEach(s -> {
				if(!VALID_KEYS.contains(s))
					throw new IllegalArgumentException("bad meta key: "+s);
			});
		}

		List<Meta> loaded;
		LoadedMetas m1 = oldMetas;

		if(oldMetas == null) {
			logger.debug("batch get: {}", idsToLoad);
			loaded = connection.loadBatch(Meta.class, idsToLoad.stream().map(s -> key(s)).collect(Collectors.toList()));
		} else {
			StringBuilder sb = new StringBuilder();
			Formatter fm = new Formatter(sb);
			fm.format("(id = :id%s AND version <> :version%s)", 0, 0);
			for (int i = 1; i < idsToLoad.size(); i++) {
				fm.format(" OR (id = :id%s AND version <> :version%s)", i, i);
			}

			fm.close();
			String filter = sb.toString();

			int n = 0;
			Map<String, AttributeValue> val = new HashMap<>();
			for (String key : idsToLoad) {
				int k = n++;
				val.put(":id"+k, new AttributeValue(key));
				val.put(":version"+k, new AttributeValue().withN(m1.getVersion(key).toString()));
			}

			logger.debug("scan query: {}, with: {}", filter, val);

			loaded = connection.mapper.scan(Meta.class, new DynamoDBScanExpression()
					.withFilterExpression(filter)
					.withExpressionAttributeValues(val));
			
			if(loaded.isEmpty()) {
				logger.debug("oldMetas IS UP TO DATE");
				return oldMetas;
			}
		}

		Map<String, Meta> data = loaded
				.stream()
				.collect(Collectors.toMap(Meta::getId, Function.identity()));
		
		logger.debug("loaded keys: "+data.keySet());
		
		try {
			LoadedMetas result = instanceClass.newInstance();
			result.ids = result.read(data, oldMetas, IDS, Utils::readShorts, t -> t.ids);
			result.idStatusMap = result.read(data, oldMetas, IDS_STATUS, Utils::readStatus, t -> t.idStatusMap);
			result.tags = result.read(data, oldMetas, TAGS, Utils::parseTags, t -> t.tags);
			logger.debug("version change: {} -> {}", oldMetas == null ? "{}" : oldMetas._version, result._version);
			
			return (E)result;
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected ShortByteMap idStatusMap;
	protected short[] ids;
	protected ShortObjectMap<String> tags;

	protected DynamoConnection connection;

	protected Map<String, Integer> _version = new HashMap<>();
	private boolean updated;

	public LoadedMetas() {
	}
	public boolean isUpdated() {
		return updated;
	}

	private Integer getVersion(String key) {
		Integer n = this._version.get(key);
		return n == null ? -1 : n;
	}

	protected void putVersion(String key, Integer version) {
		if(Objects.equals(this._version.get(key), version))
			return;
		this.updated = true;
		this._version.put(key, version);
	}

	public void setConnection(DynamoConnection connection) {
		this.connection = connection;
	}

	public ShortByteMap idStatusMap() {return idStatusMap;};
	public short[] ids() {return ids;};
	public ShortObjectMap<String> tags() {return tags;};
	public Map<String, Integer> version() {return Collections.unmodifiableMap(_version);};

	private <E> E read(Map<String, Meta> data, LoadedMetas oldMetas, String key, Function<Meta, E> mapper, Function<LoadedMetas, E> getter) {
		Meta meta = data.get(key);
		if(meta != null) {
			putVersion(key, meta.version);
			E e = mapper.apply(meta);
			logger.debug("pull {}, {}", key, LoggerUtils.lazyMessage(() -> MyUtilsException.toUnchecked(() -> {
				if(e.getClass().isArray())
					return Array.getLength(e);
				else 
					return e.getClass().getMethod("size").invoke(e);
			})));
			return e;
		} else if(oldMetas == null) {
			putVersion(key, 0);
			return null;
		} else {
			putVersion(key, oldMetas.getVersion(key));
			return (E) getter.apply(oldMetas);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		write(idStatusMap, () -> buf(idStatusMap), out);
		write(ids, () -> buf(ids), out);
		out.writeInt(tags.size());
		for (ShortObjectCursor<String> s : tags) {
			out.writeShort(s.key);
			out.writeUTF(s.value);
		}
		out.writeObject(_version);
	}

	private void write(Object item, CallableWithIOException<ByteBuffer> getter, ObjectOutput out) throws IOException {
		ByteBuffer buf = item == null ? null : getter.call();
		if(item == null) {
			out.writeInt(0);
		} else {
			out.writeInt(buf.remaining());
			out.write(buf.array(), 0, buf.remaining());	
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.idStatusMap = readStatus(readBuf(in));
		this.ids = readShorts(readBuf(in));
		int size = in.readInt();
		this.tags = new ShortObjectScatterMap<>(size);
		for (int i = 0; i < size; i++) 
			tags.put(in.readShort(), in.readUTF());
		this._version = (Map<String, Integer>) in.readObject();
	}

	private Meta readBuf(ObjectInput in) throws IOException {
		int size = in.readInt();
		if(size == 0)
			return null;

		byte[] buf = new byte[size];
		in.readFully(buf);
		Meta m = new Meta();
		m.setCompressedData(ByteBuffer.wrap(buf));
		return m;
	}

	public void addStatusIds(ShortByteMap statues) {
		if(statues == null || statues.isEmpty())
			return;

		addSparsed(IDS_STATUS, buf(statues));
		this.idStatusMap.putAll(statues);
	}

	public void addTags(ShortObjectMap<String> tags) {
		if(tags == null || tags.isEmpty())
			return;

		addSparsed(TAGS, buf(tags));
		this.tags.putAll(tags);
	}

	protected UpdateItemResult update(String key, String msg, ByteBuffer buf, Supplier<UpdateItemResult> mapper) {
		int remaining = buf.remaining();
		int oldVersion = getVersion(key);
		UpdateItemResult u = mapper.get();
		putVersion(key, new Integer(u.getAttributes().get(Meta.VERSION).getN()));
		logger.debug("{}: for: {}, size: {}, version: {} -> {}", msg, key, remaining, oldVersion, this._version.get(key));
		return u;
	}

	private UpdateItemResult addSparsed(String key, ByteBuffer buf) {
		return update(key, "add sparsed", buf, () -> connection.update(TABLE_NAME, ID, value(key), true, SPARSE_DATA, add(value(value(buf)))));
	}
	public void setUpdated(boolean b) {
		this.updated = b;
	}

}
