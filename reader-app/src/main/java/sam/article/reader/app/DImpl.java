package sam.article.reader.app;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

class DImpl<T> implements Delayed {
	final T value;
	final long startTime;

	public DImpl(T value, int delayInMills) {
		this.value = value;
		this.startTime = System.currentTimeMillis() + delayInMills;
	}

	@Override
	public int compareTo(Delayed o) {
		return Long.compare(this.startTime, ((DImpl<?>) o).startTime);
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(startTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}
}