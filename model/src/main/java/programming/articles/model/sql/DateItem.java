package programming.articles.model.sql;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name="Dates")
public class DateItem {
	@Id
	@GeneratedValue(strategy=GenerationType.TABLE)
    private int id;
	@Column(name="_date", nullable=false, unique=true)
    private String date;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getDate() {
		return date;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
}
