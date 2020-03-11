package sam.rssowl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import javax.persistence.Table;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import programming.articles.model.ConstantDataItem;
import programming.articles.model.DataItem;
import sam.sql.JDBCHelper;

@Table(name = ConstantDataItem.SQL_TABLE_NAME)
@DynamoDBTable(tableName = ConstantDataItem.TABLE_NAME)
public class Mutableitem extends DataItem {
	private static final long serialVersionUID = 1L;
	
	public Mutableitem() { }

	public Mutableitem(Map<String, AttributeValue> values) {
		super(values);
	}

	public Mutableitem(short id) {
		this.id = id; 
	}
	public void setId(short id) {
			this.id = id;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}
	public void setFavicon(String favicon) {
		this.favicon = favicon;
	}
	public void setAddedOn(String addedOn) {
		this.addedOn = addedOn;
	}
	
	public static final String INSERT_SQL = JDBCHelper.insertSQL(SQL_TABLE_NAME, ID,TITLE,SOURCE,REDIRECT,TAGS,FAVICON,ADDED_ON,STATUS,NOTES,VERSION);
	
    public void insert(PreparedStatement p) throws SQLException  {
        int n = 1;
        p.setInt(n++,id);
        setString(p,n++,title);
        setString(p,n++,source);
        setString(p,n++,redirect);
        setString(p,n++,tags);
        setString(p,n++,favicon);
        setString(p,n++,addedOn);
        setString(p,n++,status == null ? null : status.toString());
        setString(p,n++,notes);
        p.setInt(n++,version);
        p.addBatch();
    }
	private void setString(PreparedStatement p, int index, String val) throws SQLException {
		if(val == null)
			p.setNull(index, Types.VARCHAR);
		else 
			p.setString(index, val);
	}
}
