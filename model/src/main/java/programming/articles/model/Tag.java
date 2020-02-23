package programming.articles.model;

import java.util.stream.IntStream;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.carrotsearch.hppc.procedures.ShortProcedure;


@Table(name="Tags")
public class Tag {
	@Id private short id;
	@Column(nullable=false, unique=true) 
	private String name;
	
	@Transient
	private transient String lowercased;
    
	public Tag(short id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public Tag() { }
	
	public short getId() {
		return id;
	}
	public void setId(short id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getLowercased() {
		if(lowercased == null)
			lowercased = name.toLowerCase();
		return lowercased;
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
		for (int i = 0; i < len; i++) {
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
		tags.forEach(n -> sink.append('.').append(n).append('.'));
	}

	@Override
	public String toString() {
		return "Tag(" + id + ": " + name + ")";
	}
}
