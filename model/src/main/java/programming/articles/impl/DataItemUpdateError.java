package programming.articles.impl;

import programming.articles.model.dynamo.DataItem;

public class DataItemUpdateError {
	public final String field;
	public final Object value;
	public final DataItem dataItem;

	public DataItemUpdateError(DataItem dataItem, String field, Object value) {
		this.dataItem = dataItem;
		this.field = field;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "DataItemUpdateError [field=" + field + ", value=" + value + ", dataItem=" + dataItem + "]";
	}
}
