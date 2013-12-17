package de.egh.easyloop.logic.audio.destination;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import de.egh.easyloop.helper.Constants;
import de.egh.easyloop.logic.PeakStrategy;
import de.egh.easyloop.logic.audio.ReadResult;

public class SpeakerDestination implements AudioDestination {
	private static final int PLAY_BUFFER_FACTOR = 2;
	private static final String TAG = "SpeakerDestination";
	private final AudioTrack audioTrack;
	private short[] buffer;
	private int bufferSize;
	private boolean mute;
	private boolean open;
	private final PeakStrategy peakStrategy;
	private final int playBufferSizeInByte;
	private int volume;

	public SpeakerDestination() {
		Log.v(TAG, "SpeakerDestination()");

		playBufferSizeInByte = AudioTrack.getMinBufferSize(
				Constants.AudioSettings.FREQUENCY,
				AudioFormat.CHANNEL_OUT_MONO,
				Constants.AudioSettings.AUDIO_ENCODING)// ;
				* PLAY_BUFFER_FACTOR;

		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				Constants.AudioSettings.FREQUENCY,
				AudioFormat.CHANNEL_OUT_MONO,
				Constants.AudioSettings.AUDIO_ENCODING, playBufferSizeInByte,
				AudioTrack.MODE_STREAM);

		peakStrategy = new PeakStrategy(playBufferSizeInByte);

		open = false;
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
		return peakStrategy.getMax(buffer);
	}

	@Override
	public int getVolume() {
		return volume;
	}

	@Override
	public boolean isMuted() {
		return mute;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void mute(final boolean mute) {
		this.mute = mute;
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
		this.volume = volume;
	}

	/** Copies read result and updates volume value */
	private void storeReadResult(final ReadResult readResult) {
		buffer = readResult.getBuffer();
		bufferSize = readResult.getSize();

		// Implement linear volume control
		if (volume < 100 && bufferSize > 0)
			for (int i = 0; i < bufferSize; i++)
				buffer[i] = (short) (buffer[i] * volume / 100);

	}

	@Override
	public void write(final ReadResult readResult) {
		storeReadResult(readResult);

		if (!mute)
			audioTrack.write(buffer, 0, bufferSize);
	}

}
