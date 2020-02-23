package programming.articles.model.sql;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import programming.articles.model.DataStatus;

@Table(name="Data")
public class MinimalData {
	@Id 
	private int id;
	
	@Column(unique=true, nullable=false)
	private String title;
	
	@Basic private int favicon;
	
	@Enumerated(EnumType.STRING)
	protected DataStatus status;

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof MinimalData && this.id == ((MinimalData)obj).id; 
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getFavicon() {
		return favicon;
	}
	public void setFavicon(int favicon) {
		this.favicon = favicon;
	}
	public DataStatus getStatus() {
		return status;
	}
	public void setStatus(DataStatus status) {
		this.status = status;
	}
}
