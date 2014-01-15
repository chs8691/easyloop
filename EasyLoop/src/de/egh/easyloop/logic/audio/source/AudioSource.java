package de.egh.easyloop.logic.audio.source;

import de.egh.easyloop.logic.audio.ReadResult;

/** Controlling an input path. */
public interface AudioSource {

	interface LoopEventListener {

		/**
		 * Called every time a source has started playing
		 * 
		 * @param duration
		 *            integer with duration in ms, if playing time is
		 *            determinable, otherwise NO_DURATION
		 */
		void onNewLoopStart(int duration);
	}

	final static int NO_DURATION = -1;

	/**
	 * If running, the maximum of the last buffered signal will be returned,
	 * otherwise 0.
	 */
	short getActualMaxLevel();

	/** Returns the result of the last audio buffer read. */
	ReadResult getReadResult();

	/** Returns TRUE, if the source is available for using, otherwise FALSE. */
	boolean isAvailable();

	/**
	 * Returns TRUE, if the source is running. There are two reasons for
	 * returning FALSE: Either <code>start()</code> was not called before or
	 * audio source stopped itself.
	 */
	boolean isOpen();

	/**
	 * Blocking reading of the next buffer. After processing this method,
	 * consumer can get the result with methods of interface
	 * <code>de.egh.easyloop.logic.ReadResult</code>.
	 */
	void read();

	/** There can be set up to one listener. */
	void setLoopEventListener(LoopEventListener loopEventListener);

	/**
	 * Try to open the audio source. You can get the result with
	 * <code>isStarted</code>.
	 */
	void start();

	/** Closes the in channel. */
	void stop();

}
