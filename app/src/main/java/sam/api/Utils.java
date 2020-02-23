package sam.api;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Utils {
	static <E, F> List<F> minimumCopyList(List<E> list, Function<E, F> mapper) {
		if(list.isEmpty())
			return Collections.emptyList();
		if(list.size() == 1)
			return Collections.singletonList(mapper.apply(list.get(0)));
		
		return list.stream().map(mapper).collect(Collectors.toList());
	}

	static <E> List<E> minimumCopyList(List<E> list) {
		return minimumCopyList(list, Function.identity());
	}

}
