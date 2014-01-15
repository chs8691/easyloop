package de.egh.easyloop.logic.audio;

/** Provides result of a audio buffer read. */
public interface ReadResult {

	/** Returns the actual audio buffer copy */
	public short[] getBuffer();

	/**
	 * Returns the static array size of the buffer (in contrast to
	 * <code>getSize()</code>). This value is useful for a dependent
	 * AudioDestination.
	 */
	public int getBufferSizeInByte();

	/**
	 * Returns the size of the last reading in shorts if last reading was
	 * successful. Otherwise a non positive integer will be returned.
	 */
	public int getSize();

}
