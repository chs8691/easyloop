package de.egh.easyloop.logic.audio.destination;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import de.egh.easyloop.application.Constants;
import de.egh.easyloop.helper.Util;
import de.egh.easyloop.logic.PeakStrategy;
import de.egh.easyloop.logic.audio.ReadResult;

public class SpeakerDestination implements AudioDestination {
	private static final String TAG = "SpeakerDestination";
	private final AudioTrack audioTrack;
	private final short[] buffer;

	private int bufferSize;
	private boolean muted;
	// If muted: Play empty buffer
	private short[] mutedBuffer;
	private boolean open;
	private final PeakStrategy peakStrategy;
	private final int playBufferSizeInByte;
	private int volume;

	/**
	 * 
	 * @param context
	 *            Context with activity context
	 * @param name
	 *            String with name of this destination. Must be unique.
	 */
	public SpeakerDestination() {
		Log.v(TAG, "SpeakerDestination()");

		// playBufferSizeInByte = AudioTrack.getMinBufferSize(
		// Constants.AudioSettings.FREQUENCY,
		// AudioFormat.CHANNEL_OUT_MONO,
		// Constants.AudioSettings.AUDIO_ENCODING)// ;
		// * Constants.AudioSettings.BUFFER_SIZE_FACTOR;
		playBufferSizeInByte = Util.getBufferSizeInByte();

		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				Constants.AudioSettings.FREQUENCY,
				AudioFormat.CHANNEL_OUT_MONO,
				Constants.AudioSettings.AUDIO_ENCODING, playBufferSizeInByte,
				AudioTrack.MODE_STREAM);

		peakStrategy = new PeakStrategy(playBufferSizeInByte);

		open = false;

		// prevent null pointer exception
		buffer = Util.createBuffer();

	}

	@Override
	public void close() {
		Log.v(TAG, "close()");

		// Not running
		if (!open)
			return;

		this.open = false;

		// Interrupt playing immediately
		audioTrack.pause();
		// Discard buffer because we don't support a pause button
		audioTrack.flush();

	}

	@Override
	public short getActualMaxLevel() {
		return peakStrategy.getMax(buffer, bufferSize);
	}

	@Override
	public int getVolume() {
		return volume;
	}

	@Override
	public boolean isMuted() {
		return muted;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void mute(final boolean mute) {

		this.muted = mute;

	}

	@Override
	public void open() {
		Log.v(TAG, "open()");

		// Already running
		if (open)
			return;

		this.open = true;
		audioTrack.play();
	}

	@Override
	public void setVolume(final int volume) {
		Log.v(TAG, "setVolume() to " + volume);

		this.volume = volume;

	}

	/** Copies read result with volume controlled value. */
	private void storeReadResult(final ReadResult readResult) {

		readResult.copy(buffer);
		mutedBuffer = new short[buffer.length];

		bufferSize = readResult.getSize();

		// Implement linear volume control
		if (volume < 100 && bufferSize > 0)
			for (int i = 0; i < bufferSize; i++)
				buffer[i] = (short) (buffer[i] * volume / 100);

	}

	@Override
	public void write(final ReadResult readResult) {
		storeReadResult(readResult);

		if (muted)
			audioTrack.write(mutedBuffer, 0, bufferSize);
		else
			audioTrack.write(buffer, 0, bufferSize);
	}

}
