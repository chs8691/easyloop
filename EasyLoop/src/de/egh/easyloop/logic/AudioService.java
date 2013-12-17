package de.egh.easyloop.logic;

/** Access to the incoming audio source */
public interface AudioService {

	/** TRUE, if actual audio source is in mute, otherwise FALSE. */
	public boolean isInMute();

	/** Mute the actual audio in source, for instance microphone */
	public void setInMute(boolean mute);

}
