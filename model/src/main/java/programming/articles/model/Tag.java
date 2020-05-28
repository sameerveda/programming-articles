package programming.articles.model;

import static programming.articles.model.TagMeta.ID;
import static programming.articles.model.TagMeta.NAME;
import static programming.articles.model.TagMeta.TAG_TABLE_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.carrotsearch.hppc.procedures.ShortProcedure;

import sam.sql.QueryUtils;

public class Tag {
	public static final List<String> COLUMNS = Collections.unmodifiableList(Arrays.asList(ID, NAME));

	public final short id;
	public final String name;

	public Tag(SQLiteStatement rs) throws SQLiteException {
		short n = 0;
		this.id = (short)rs.columnInt(n++); // ID
		this.name = rs.columnString(n++); // NAME
	}

	public Tag(short id, String name) {
		this.id = id;
		this.name = name;
	}

	public short getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	static final String SELECT_SQL = "SELECT " + String.join(",", COLUMNS) + " FROM " + TAG_TABLE_NAME;
	static final String INSERT_SQL = QueryUtils.insertSQL(TAG_TABLE_NAME, COLUMNS);

	void insert(SQLiteStatement p) throws SQLiteException {
		short n = 1;
		p.bind(n++, id);
		p.bind(n++, name);
	}
	
	public static String serialize(IntStream tags) {
		StringBuilder sb = new StringBuilder();
		appendTo(tags, sb);
		return sb.length() == 0 ? null : sb.toString(); 
	}
	
	public static void parse(CharSequence tags, ShortProcedure action) {
		int len;
		if(tags == null || (len = tags.length()) == 0)
			return;
		
		StringBuilder sb = new StringBuilder();
		for (short i = 0; i < len; i++) {
			char c = tags.charAt(i);
			if(c == '.') {
				if(sb.length() != 0) {
					action.apply(Short.parseShort(sb.toString()));
					sb.setLength(0);
				}
			} else {
				sb.append(c);
			}
		}
	}

	public static void appendTo(IntStream tags, StringBuilder sink) {
		tags.sorted().distinct().forEach(n -> sink.append('.').append(n).append('.'));
	}

	@Override
	public String toString() {
		return "Tag(" + id + ": " + name + ")";
	}

}
