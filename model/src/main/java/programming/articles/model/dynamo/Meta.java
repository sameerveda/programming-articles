package programming.articles.model.dynamo;

import static sam.full.access.dynamodb.DynamoConnection.optInt;
import static sam.full.access.dynamodb.DynamoConnection.optString;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import sam.full.access.dynamodb.DynamoConnection;

@DynamoDBTable(tableName=Meta.TABLE_NAME)
public class Meta {
	public static final String TABLE_NAME = "programming-articles-meta";
	public static final String ID = "id";
	public static final String COMPRESSED_DATA = "compressedData";
	public static final String SPARSE_DATA = "sparseData";
	public static final String VERSION = "version";

	@DynamoDBHashKey
    public String id;
	
    public ByteBuffer compressedData;
    public List<ByteBuffer> sparseData;
    public int version;
    
    public Meta() { }
    
	public Meta(Map<String, AttributeValue> data) {
		this.id = Objects.requireNonNull(optString(data.get(ID), null));
		this.compressedData = data.get(COMPRESSED_DATA).getB();
		this.sparseData = DynamoConnection.optL(data.get(SPARSE_DATA), DynamoConnection::optB);
		this.version = optInt(data.get(VERSION), -1);
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public ByteBuffer getCompressedData() {
		return compressedData;
	}
	public void setCompressedData(ByteBuffer compressedData) {
		this.compressedData = compressedData;
	}
	public List<ByteBuffer> getSparseData() {
		return sparseData;
	}
	public void setSparseData(List<ByteBuffer> sparseData) {
		this.sparseData = sparseData;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
}
