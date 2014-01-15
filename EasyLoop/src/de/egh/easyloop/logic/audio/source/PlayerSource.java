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
	private int duration;

	private InputStream is = null;
	private LoopEventListener loopEventListener;
	private boolean open;
	private final PeakStrategy peakStrategy;
	private final int playBufferSizeInShort;
	private final ReadResultImplementation readResult;

	private int readSize;
	/** For calculation actual position. SystemTime in ms or 0, if not running */
	private long startTime;

	/** Can be called in the UI thread */
	public PlayerSource(final ContextWrapper contextWrapper) {
		Log.v(TAG, "PlayerSource()");

		this.contextWrapper = contextWrapper;

		bufferSizeInByte = AudioTrack.getMinBufferSize(
				Constants.AudioSettings.FREQUENCY,
				Constants.AudioSettings.CHANNEL_OUT_CONFIG,
				Constants.AudioSettings.AUDIO_ENCODING)// ;
				* PLAY_BUFFER_FACTOR;
		Log.v(TAG, "Set playBufferSizeInBye=" + bufferSizeInByte);

		// A short has 2 bytes
		playBufferSizeInShort = bufferSizeInByte / 2;

		peakStrategy = new PeakStrategy(bufferSizeInByte);

		readResult = new ReadResultImplementation();

		open = true;

		startTime = 0;

		duration = 0;

		setLoopEventListener(null);

	}

	@SuppressWarnings("unused")
	private int calculateDuration() {
		Log.v(TAG, "calculateDuration()");

		final File file = contextWrapper
				.getFileStreamPath(Constants.AudioSettings.RECORD_FILENAME);

		final long bytes = file.length();

		final int bitsPerSample = 16;
		if (Constants.AudioSettings.AUDIO_ENCODING != AudioFormat.ENCODING_PCM_16BIT)
			throw new RuntimeException(
					"Unsupported audio format, can't calculate playing duration.");

		final int channels = 1;
		if (Constants.AudioSettings.CHANNEL_OUT_CONFIG != AudioFormat.CHANNEL_OUT_MONO)
			throw new RuntimeException(
					"Unsupported audio channel format, can't calculate playing duration.");

		final int duration = (int) ((bytes * Constants.BITS_PER_BYTE * Constants.MS_PER_SECOND) / (Constants.AudioSettings.FREQUENCY
				* channels * bitsPerSample));
		Log.v(TAG, "duration=" + duration);

		return duration;
	}

	@Override
	public short getActualMaxLevel() {
		return peakStrategy.getMax(bufferShort);
	}

	/** If running, returns running time of the loop, otherwise 0. */
	public int getActualTime() {
		if (isOpen())
			return (int) (System.currentTimeMillis() - startTime);
		else
			return 0;

	}

	public int getDuration() {
		return duration;
	}

	@Override
	public ReadResult getReadResult() {
		readResult.setBuffer(bufferShort, readSize);
		return readResult;
	}

	@Override
	public boolean isAvailable() {
		return contextWrapper.getFileStreamPath(
				Constants.AudioSettings.RECORD_FILENAME).length() > 0;
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
				// Loop
				reset();

		} catch (final IOException e) {
			open = false;
			e.printStackTrace();
		}

	}

	/**
	 * Will be called with every new loop, so it's a good place here to fire the
	 * LoopEventListener
	 */
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

		duration = calculateDuration();
		startTime = System.currentTimeMillis();
		loopEventListener.onNewLoopStart(duration);
	}

	@Override
	public void setLoopEventListener(final LoopEventListener loopEventListener) {
		if (loopEventListener != null)
			this.loopEventListener = loopEventListener;
		else
			// Null-Object
			this.loopEventListener = new LoopEventListener() {

				@Override
				public void onNewLoopStart(final int duration) {
				}
			};
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
		startTime = 0;
		bufferShort = new short[playBufferSizeInShort];

	}

}
