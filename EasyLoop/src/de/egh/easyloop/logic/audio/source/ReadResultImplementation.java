package de.egh.easyloop.logic.audio.source;

import de.egh.easyloop.helper.Util;
import de.egh.easyloop.logic.audio.ReadResult;

public class ReadResultImplementation implements ReadResult {

	private final short[] buffer = Util.createBuffer();

	private int size;

	@Override
	public short[] copy(final short[] buffer) {
		System.arraycopy(this.buffer, 0, buffer, 0, this.buffer.length);

		return buffer;
	}

	@Override
	public short[] getBuffer() {
		return buffer.clone();
	}

	// @Override
	// public int getBufferSizeInByte() {
	// return buffer.length * 2;
	// }

	@Override
	public int getSize() {
		return size;
	}

	/** Save a buffer copy */
	void setBuffer(final short[] buffer, final int size) {
		// this.buffer = buffer.clone();
		System.arraycopy(buffer, 0, this.buffer, 0, this.buffer.length);

		this.size = size;

	}

}
