package programming.articles.model.sql;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Table;


@Table(name="Data")
public abstract class DataItem extends MinimalData {
	@Column(unique=true, nullable=false) 
	private String source;
	
	@Column(unique=true)
	private String redirect;
	
	@Column(name="added_on")
	private int addedOn;
	
	@Basic protected String tags;
	@Basic protected String notes;
	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getRedirect() {
		return redirect;
	}
	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}
	public int getAddedOn() {
		return addedOn;
	}
	public void setAddeOn(int added_on) {
		this.addedOn = added_on;
	}
	public String getTags() {
		return tags;
	}
	public void setTags(String tags) {
		this.tags = tags;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
}
