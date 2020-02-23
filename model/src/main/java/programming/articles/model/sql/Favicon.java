package programming.articles.model.sql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Table(name="Favicons")
public class Favicon {
	@Id
    private int id;
	
	@Column(unique=true, nullable=false)
    private String url;
	
    @Lob
    private byte[] data;
    
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
}
