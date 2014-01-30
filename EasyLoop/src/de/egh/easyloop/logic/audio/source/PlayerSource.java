package de.egh.easyloop.logic.audio.source;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContextWrapper;
import android.media.AudioFormat;
import android.util.Log;
import de.egh.easyloop.application.Constants;
import de.egh.easyloop.application.SettingsBuffer;
import de.egh.easyloop.helper.Util;
import de.egh.easyloop.logic.PeakStrategy;
import de.egh.easyloop.logic.audio.ReadResult;

/** Plays an audio file. */
public class PlayerSource implements AudioSource {

	/**
	 * Manage all fading stuff. For faster access, there are no setter and
	 * getter.
	 */
	private static class FadeController {
		/** Switch for fade in and fade out . */
		private enum Phase {
			IN, NO, OUT
		}

		private int counter;
		float factor;
		int fadeInSteps;
		private final int fadeInStepSize;

		long fadeOutPositionShorts;
		int fadeOutSteps;
		private final int fadeOutStepSize;
		private Phase phase;
		private long totalReadSize;

		FadeController() {
			fadeInSteps = SettingsBuffer.getInstance().getFadeIn();
			fadeInStepSize = fadeInSteps / 100;
			fadeOutSteps = SettingsBuffer.getInstance().getFadeOut();
			fadeOutStepSize = fadeOutSteps / 100;

			Log.v(TAG, "Fade in Steps=" + fadeInSteps + " --> sinze="
					+ fadeInStepSize);
			Log.v(TAG, "Fade out Steps=" + fadeOutSteps + " --> sinze="
					+ fadeOutStepSize);

		}

		/** Returns TRUE, if Phase equals 'Fade in', otherwise FALSE. */
		boolean isPhaseFadeIn() {
			return phase.equals(Phase.IN);
		}

		/** Returns TRUE, if Phase equals 'Fade out', otherwise FALSE. */
		boolean isPhaseFadeOut() {
			return phase.equals(Phase.OUT);
		}

		/**
		 * Increase factor until end maximum is reached (1.0f). Call this only
		 * in Phase 'Fade in'.
		 */
		float nextInFactor() {
			if (++counter >= fadeInStepSize) {
				factor += 0.01f;
				counter = 0;
			}

			return factor;
		}

		/**
		 * Decrease factor until end maximum is reached (1.0f). Call this onyl
		 * in phase 'Fade out'
		 */
		float nextOutFactor() {
			if (++counter >= fadeOutStepSize) {
				factor -= 0.01f;
				counter = 0;
			}

			return factor;
		}

		/** Must be called for every sequence */
		void reset(final long fileLengthShort) {
			Log.v(TAG, "reset()");

			// If fade out is set, then calculate entry position for fading out
			if (fadeOutSteps > 0) {
				if (fileLengthShort > fadeOutSteps)
					fadeOutPositionShorts = (fileLengthShort - fadeOutSteps);
				else
					// Special case: very, very small file: Should never happens
					fadeOutPositionShorts = Long.MAX_VALUE;
			}
			// otherwise never fade out
			else
				fadeOutPositionShorts = Long.MAX_VALUE;

			// Sequence starts with fade in, if this was set in the preferences
			if (fadeInSteps > 0)
				phase = Phase.IN;
			else
				phase = Phase.NO;

			Log.v(TAG, "file lenght and fadeOutPosition:" + fileLengthShort
					+ " " + fadeOutPositionShorts);

			// Set start factor for fading in
			factor = 0;

			// Actual number of shorts, that have been read.
			totalReadSize = 0;

			counter = 0;
		}

		/**
		 * Change the phase, when end of previous phase is reached: Fade in -->
		 * No fading --> Fade out. Call this for example after every buffer
		 * read.
		 */
		void triggerPhase(final long readSize) {
			totalReadSize += readSize;

			// Fade in ends, when factor has been increased to 1
			if (phase.equals(Phase.IN) && factor >= 1) {
				phase = Phase.NO;
				Log.v(TAG, "Leaving phase in at " + totalReadSize);
			}

			// Fade out starts, when fade out position has been reached (and if
			// fade must must be set in the preferences, of course.)
			if (totalReadSize >= fadeOutPositionShorts && fadeOutSteps > 0
					&& !phase.equals(Phase.OUT)) {
				phase = Phase.OUT;
				factor = 1;
				Log.v(TAG, "Entering phase out at " + totalReadSize);

			}
		}
	}

	private static final int PLAY_BUFFER_FACTOR = 2;

	private static final String TAG = "PlayerSource";
	private BufferedInputStream bis = null;
	private short[] bufferShort;

	private final int bufferSizeInByte;
	private final ContextWrapper contextWrapper;
	private DataInputStream dis = null;

	private int duration;
	private final FadeController fadeController;

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

		// bufferSizeInByte = AudioTrack.getMinBufferSize(
		// Constants.AudioSettings.FREQUENCY,
		// Constants.AudioSettings.CHANNEL_OUT_CONFIG,
		// Constants.AudioSettings.AUDIO_ENCODING)// ;
		// * PLAY_BUFFER_FACTOR;
		bufferSizeInByte = Util.getBufferSizeInByte();
		Log.v(TAG, "Set playBufferSizeInBye=" + bufferSizeInByte);

		// A short has 2 bytes
		playBufferSizeInShort = bufferSizeInByte / 2;

		peakStrategy = new PeakStrategy(bufferSizeInByte);

		readResult = new ReadResultImplementation();

		open = true;

		startTime = 0;

		duration = 0;

		fadeController = new FadeController();

		setLoopEventListener(null);

	}

	/**
	 * Returns duration of the sequence.
	 * 
	 * @param bytes
	 *            length of the file in bytes
	 * @return integer with duration in milliseconds
	 */
	private int calculateDuration(final long bytes) {
		Log.v(TAG, "calculateDuration()");

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
		return peakStrategy.getMax(bufferShort, readSize);
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

			// First
			if (fadeController.isPhaseFadeIn()) {
				// Log.v(TAG, "FADE IN");
				for (readSize = 0; dis.available() > 0
						&& (readSize < playBufferSizeInShort); readSize++) {
					bufferShort[readSize] = (short) (dis.readShort() * fadeController
							.nextInFactor());
				}
			} else if (fadeController.isPhaseFadeOut()) {
				// Log.v(TAG, "FADE OUT");
				for (readSize = 0; dis.available() > 0
						&& (readSize < playBufferSizeInShort); readSize++) {
					bufferShort[readSize] = (short) (dis.readShort() * fadeController
							.nextOutFactor());
				}

			}
			// Normal
			else {
				// Log.v(TAG, "NORMAL");
				for (readSize = 0; dis.available() > 0
						&& (readSize < playBufferSizeInShort); readSize++) {
					bufferShort[readSize] = dis.readShort();
				}
			}
			fadeController.triggerPhase(readSize);

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
		final File file = contextWrapper
				.getFileStreamPath(Constants.AudioSettings.RECORD_FILENAME);

		final long fileLengthBytes = file.length();

		fadeController.reset(fileLengthBytes / 2);
		duration = calculateDuration(fileLengthBytes);
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
		bufferShort = Util.createBuffer();
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
		bufferShort = Util.createBuffer();

	}

}
