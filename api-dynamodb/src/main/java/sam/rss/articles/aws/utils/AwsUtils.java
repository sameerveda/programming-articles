package sam.rss.articles.aws.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;

public interface AwsUtils {
	public static AttributeValue value(Set<String> val) {
		return new AttributeValue().withSS(val);
	}

	public static AttributeValue value(CharSequence val) {
		return new AttributeValue().withS(val.toString());
	}

	public static AttributeValue value(int val) {
		return new AttributeValue().withN(Integer.toString(val));
	}

	public static AttributeValue value(Number val) {
		return new AttributeValue().withN(val.toString());
	}

	public static AttributeValue value(ByteBuffer buf) {
		return new AttributeValue().withB(buf);
	}
	
	public static AttributeValue numberSet(Number... values) {
		return new AttributeValue().withNS(Arrays.stream(values).map(Number::toString).collect(Collectors.toList()));
	}
	
	public static AttributeValue mEntry(String key, Number value) {
		return new AttributeValue().addMEntry(key, value(value));
	}
	public static AttributeValue mEntry(String key, String value) {
		return new AttributeValue().addMEntry(key, value(value));
	}

	public static AttributeValue value(Boolean bool) {
		return new AttributeValue().withBOOL(bool);
	}

	public static AttributeValueUpdate add(AttributeValue value) {
		return new AttributeValueUpdate(value, AttributeAction.ADD);
	}

	public static AttributeValueUpdate set(AttributeValue value) {
		return new AttributeValueUpdate(value, AttributeAction.PUT);
	}
	
	public static AttributeValueUpdate put(AttributeValue value) {
		return new AttributeValueUpdate(value, AttributeAction.PUT);
	}

	public static AttributeValueUpdate delete(AttributeValue value) {
		return new AttributeValueUpdate(value, AttributeAction.DELETE);
	}

	public static AttributeValueUpdate delete() {
		return new AttributeValueUpdate(null, AttributeAction.DELETE);
	}
}
