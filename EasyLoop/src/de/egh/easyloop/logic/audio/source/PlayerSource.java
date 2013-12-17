package de.egh.easyloop.logic.audio.source;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContextWrapper;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;
import de.egh.easyloop.helper.Constants;
import de.egh.easyloop.logic.PeakStrategy;
import de.egh.easyloop.logic.audio.ReadResult;

/** Plays an audio file. */
public class PlayerSource implements AudioSource {

	private static final int PLAY_BUFFER_FACTOR = 1;
	private static final String TAG = "PlayerSource";

	private BufferedInputStream bis = null;
	private short[] bufferShort;
	private final int bufferSizeInByte;

	private final ContextWrapper contextWrapper;
	private DataInputStream dis = null;
	private InputStream is = null;

	private boolean open;
	private final PeakStrategy peakStrategy;
	private final int playBufferSizeInShort;
	private final ReadResultImplementation readResult;
	private int readSize;

	/** Can be called in the UI thread */
	public PlayerSource(final ContextWrapper contextWrapper) {
		Log.v(TAG, "PlayerSource()");

		this.contextWrapper = contextWrapper;

		bufferSizeInByte = AudioTrack.getMinBufferSize(
				Constants.AudioSettings.FREQUENCY,
				AudioFormat.CHANNEL_OUT_MONO,
				Constants.AudioSettings.AUDIO_ENCODING)// ;
				* PLAY_BUFFER_FACTOR;
		Log.v(TAG, "Set playBufferSizeInBye=" + bufferSizeInByte);

		// A short has 2 bytes
		playBufferSizeInShort = bufferSizeInByte / 2;

		peakStrategy = new PeakStrategy(bufferSizeInByte);

		readResult = new ReadResultImplementation();

		open = true;

	}

	@Override
	public short getActualMaxLevel() {
		return peakStrategy.getMax(bufferShort);
	}

	@Override
	public ReadResult getReadResult() {
		readResult.setBuffer(bufferShort, readSize);
		return readResult;
	}

	@Override
	public boolean isOpen() {
		Log.v(TAG, "isOpen()=" + open);

		return open;
	}

	@Override
	public void read() {
		try {
			for (readSize = 0; dis.available() > 0
					&& (readSize < playBufferSizeInShort); readSize++) {
				bufferShort[readSize] = dis.readShort();
			}

			if (dis.available() == 0)
				reset();

		} catch (final IOException e) {
			open = false;
			e.printStackTrace();
		}

	}

	private void reset() {
		try {
			is = contextWrapper
					.openFileInput(Constants.AudioSettings.RECORD_FILENAME);
			bis = new BufferedInputStream(is);
			dis = new DataInputStream(bis);

		} catch (final FileNotFoundException e) {
			open = false;
			e.printStackTrace();
		}
	}

	@Override
	public void start() {
		Log.v(TAG, "start()");

		readSize = 0;
		bufferShort = new short[playBufferSizeInShort];
		open = true;

		reset();

		final File file = contextWrapper
				.getFileStreamPath(Constants.AudioSettings.RECORD_FILENAME);

		Log.v(TAG, "start(): Open file " + file.getPath() + " " + file.length());
	}

	@Override
	public void stop() {
		Log.v(TAG, "stop()");
		try {
			dis.close();
			bis.close();
			is.close();
		} catch (final IOException e) {
			open = false;
			e.printStackTrace();
		}

		open = true;
		readSize = 0;
		bufferShort = new short[playBufferSizeInShort];

	}

}
