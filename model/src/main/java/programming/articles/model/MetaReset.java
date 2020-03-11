package programming.articles.model;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static programming.articles.model.LoadedMetas.IDS;
import static programming.articles.model.LoadedMetas.IDS_STATUS;
import static programming.articles.model.LoadedMetas.TAGS;
import static programming.articles.model.Utils.parseTags;
import static programming.articles.model.Utils.readShorts;
import static programming.articles.model.Utils.readStatus;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.carrotsearch.hppc.ShortByteScatterMap;
import com.carrotsearch.hppc.ShortObjectMap;

import sam.full.access.dynamodb.DynamoConnection;
import sam.myutils.Checker;

public class MetaReset implements Runnable {
	private final DynamoConnection connection;

	public MetaReset(DynamoConnection connection) {
		this.connection = connection;
	}

	@Override
	public void run() {
		List<Meta> scan =  connection.mapper.scan(Meta.class, new DynamoDBScanExpression().withFilterExpression("attribute_exists("+Meta.SPARSE_DATA+")"));
		Map<String, Meta> metas = scan.isEmpty() ? emptyMap() : scan.stream().collect(toMap(Meta::getId, Function.identity()));
		
		if(metas.isEmpty()) {
			System.out.println("NOTHING TO UPDATE");
			return;
		}
		
		SettableLoadedMetas utils = new SettableLoadedMetas();
		utils.setConnection(connection);

		if(metas.containsKey(IDS))
			utils.setAllIds(readShorts(metas.get(IDS)));
		
		if(metas.containsKey(IDS_STATUS)) {
			ShortByteScatterMap newStatus = readStatus(metas.get(IDS_STATUS));
			System.out.println("statues: "+newStatus);
			utils.setStatusIds(newStatus);
		}
		
		ShortObjectMap<String> tags = parseTags(metas.get(TAGS));
		if(Checker.isNotEmpty(tags)) {
			System.err.println("tags: "+tags.size());
			utils.setTags(tags);			
		}
		
		System.out.println("DONE");
	}
}
