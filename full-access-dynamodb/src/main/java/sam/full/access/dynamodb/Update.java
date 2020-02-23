package sam.full.access.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;

public class Update {
	public final String field;
	public AttributeValueUpdate value;
	
	public Update(String field, AttributeValueUpdate value) {
		this.field = field;
		this.value = value;
	}
}
