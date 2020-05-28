package programming.articles.model;

import static programming.articles.model.DataItemMeta.DYNAMO_TABLE_NAME;
import static sam.full.access.dynamodb.DynamoConnection.value;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.almworks.sqlite4java.SQLiteException;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import sam.full.access.dynamodb.DynamoConnection;
import sam.sql.sqlite.Sqlite4javaHelper;

class MainTest {

	@Test
	void test() throws JsonProcessingException, SQLException, SQLiteException {
		try(DynamoConnection con = new DynamoConnection(new PropertiesFileCredentialsProvider("D:\\importents_are_here\\eclipse_workplace\\Grouped-Projects\\programming-articles\\ffe87d75-3b62-4a6c-82d5-a009a718bd56"));
				Sqlite4javaHelper db = new Sqlite4javaHelper(new File("C:\\Users\\sameer\\Documents\\MEGAsync\\SimpleUtils\\sam\\programs\\rssowl\\data.db"), false);
				) {
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(con.mapper.load(DataItem.class, 7886)));
			System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(db.getFirst("SELECT * FROM Data WHERE id=7886", null, st -> new DataItem(st, n -> "DATE", n -> "FAVICON"))));
			
			ScanRequest req = new ScanRequest(DYNAMO_TABLE_NAME)
					.withProjectionExpression("id, version")
					.withFilterExpression("attribute_exists(version) AND version > :version")
					.addExpressionAttributeValuesEntry(":version", value(0));
			
			List<Map<String, AttributeValue>> list = con.collect(req);
			list.forEach(m -> System.out.println());
		}
	}

}
