package de.egh.easyloop.logic.audio.source;

import de.egh.easyloop.logic.audio.ReadResult;

public class ReadResultImplementation implements ReadResult {

	private short[] buffer = new short[0];

	private int size;

	@Override
	public short[] getBuffer() {
		return buffer;
	}

	@Override
	public int getBufferSizeInByte() {
		return buffer.length * 2;
	}

	@Override
	public int getSize() {
		return size;
	}

	/** Save a buffer copy */
	void setBuffer(final short[] buffer, final int size) {
		this.buffer = buffer.clone();
		this.size = size;

	}

}
