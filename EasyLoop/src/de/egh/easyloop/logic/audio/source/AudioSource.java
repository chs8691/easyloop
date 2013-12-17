package de.egh.easyloop.logic.audio.source;

import de.egh.easyloop.logic.audio.ReadResult;

/** Controlling an input path. */
public interface AudioSource {

	/**
	 * If running, the maximum of the last buffered signal will be returned,
	 * otherwise 0.
	 */
	public short getActualMaxLevel();

	/** Returns the result of the last audio buffer read. */
	public ReadResult getReadResult();

	/**
	 * Returns TRUE, if the source is running. There are two reasons for
	 * returning FALSE: Either <code>start()</code> was not called before or
	 * audio source stopped itself.
	 */
	public boolean isOpen();

	/**
	 * Blocking reading of the next buffer. After processing this method,
	 * consumer can get the result with methods of interface
	 * <code>de.egh.easyloop.logic.ReadResult</code>.
	 */
	public void read();

	/**
	 * Try to open the audio source. You can get the result with
	 * <code>isStarted</code>.
	 */
	public void start();

	/** Closes the in channel. */
	public void stop();

}
