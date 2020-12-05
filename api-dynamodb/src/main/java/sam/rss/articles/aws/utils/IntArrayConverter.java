package sam.rss.articles.aws.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

public class IntArrayConverter implements DynamoDBTypeConverter<Set<Integer>, int[]> {

	@Override
	public Set<Integer> convert(int[] array) {
		return array == null ? null : Arrays.stream(array).boxed().collect(Collectors.toSet());
	}

	@Override
	public int[] unconvert(Set<Integer> set) {
		return set == null ? null : set.isEmpty() ? new int[0] : set.stream().mapToInt(Integer::intValue).sorted().toArray();
	}	
}
