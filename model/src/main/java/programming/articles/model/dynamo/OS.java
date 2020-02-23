package programming.articles.model.dynamo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class OS extends OutputStream {
	ByteBuffer buf = ByteBuffer.allocate(4 * 1024);

	@Override
	public void write(int b) throws IOException {
		ensureCap(1);
		buf.put((byte) b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ensureCap(len);
		buf.put(b, off, len);
	}

	private void ensureCap(int len) {
		if(buf.remaining() < len) {
			ByteBuffer temp = ByteBuffer.allocate(buf.capacity() + 2 * 1024);
			buf.flip();
			temp.put(buf);
			this.buf = temp;
		}
	}
}
