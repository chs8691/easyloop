package de.egh.easyloop.logic.audio.destination;

import de.egh.easyloop.logic.audio.ReadResult;

public interface AudioDestination {

	/**
	 * Closes audio destination. Audio destination can be reopened by calling
	 * <code>open()</code>. Nothing will be done, if audio destination is
	 * already closed.
	 */
	public void close();

	/**
	 * If running, the maximum of the last buffered signal will be returned,
	 * otherwise 0.
	 */
	public short getActualMaxLevel();

	/**
	 * Return the volume level in percentage [0 .. 100].
	 */
	public int getVolume();

	/** TRUE, if destination is muted, otherwise false. */
	public boolean isMuted();

	/** Returns TRUE, if the audio destination is usable, otherwise FALSE. */
	public boolean isOpen();

	/** Toggles output's mute. Has no impact to the max. level signal. */
	public void mute(boolean mute);

	/**
	 * Opens audio destination for receiving audio buffer data. Must be called
	 * before using <code>write()</code>. Nothing will be done, if destination
	 * is already open.
	 */
	public void open();

	/**
	 * Sets the volume of the audio destination. Volume has impact to the max.
	 * level signal, too.
	 * 
	 * @param volume
	 *            integer with percentage value [0 .. 100]
	 */
	public void setVolume(int volume);

	/**
	 * Writes the buffer to the the audio destination. This method should be
	 * called in the asynchronous task.
	 * 
	 * @throws RuntimeException
	 *             if open()==FALSE
	 */
	public void write(final ReadResult readResult);
}
