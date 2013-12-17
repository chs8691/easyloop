package de.egh.easyloop.logic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.content.ContextWrapper;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

/** Has two channels: A record/play and a live signal routing */
public class Tapemachine {

	interface LevelChangeEventListener {
		/** Peak strategy fires a level change */
		public void onLevelChange(short level);

	}

	/** Tape machine's events for live channel */
	interface LiveEventListener extends LevelChangeEventListener {

		/** Fires, when live signal opened */
		public void onStart();

		/** Fires, when live signal stopped */
		public void onStop();

	}

	/** Only open input channel without recording */
	private class LiveTask extends TapemachineAsyncTask {

		@Override
		protected Result doInBackground(final String... params) {

			try {

				audioLive.startRecording();

				while (!isCancelled()) {
					audioLive
							.read(liveBufferShort, 0, liveBufferSizeInByte / 4);

					publishProgress(liveBufferShort);

				}
				audioLive.stop();
				// Last value event must be 0
				publishProgress(new short[0]);

			} catch (final Exception e) {
				e.printStackTrace();
			}

			return null;

		}

		@Override
		protected LevelChangeEventListener getLevelChangeEventListener() {
			return liveEventListener;
		}

		/** Same configuration as recording */
		@Override
		protected int getPeakHistorySize() {
			return livePeakHistorySize;
		}

		@Override
		protected float getVolume() {
			return liveVolume;
		}

	}

	enum Mode {
		PLAYING, RECORDING;

		public Mode copy() {
			if (name().equals(Mode.PLAYING)) {
				return Mode.PLAYING;
			} else {
				return Mode.RECORDING;
			}
		}
	}

	private class PlayTask extends TapemachineAsyncTask {

		@Override
		protected Result doInBackground(final String... params) {
			InputStream is;
			BufferedInputStream bis;
			DataInputStream dis;
			int j;
			short level;
			try {
				boolean end;

				audioTrack.play();

				// loop
				while (!isCancelled()) {
					is = contextWrapper.openFileInput(RECORD_FILENAME);
					// final File file = new File(
					// Environment.getExternalStorageDirectory(),
					// RECORD_FILENAME);
					// is = new FileInputStream(file);
					bis = new BufferedInputStream(is);

					dis = new DataInputStream(bis);
					end = false;
					j = 0;

					while (!isCancelled() && !end) {
						level = 0;

						for (j = 0; dis.available() > 0
								&& (j < playBufferSizeInByte / 2); j++) {
							playBufferShort[j] = dis.readShort();
							if (playBufferShort[j] > level) {
								level = playBufferShort[j];
							}
						}
						audioTrack.write(playBufferShort, 0, j);

						publishProgress(playBufferShort);

						if (dis.available() <= 0) {
							end = true;
						}

					}
					dis.close();
					bis.close();
					is.close();
				}

				// Interrupt playing immediately
				audioTrack.pause();
				// Discard buffer because we don't support a pause button
				audioTrack.flush();
				// Last value event must be 0
				publishProgress(new short[0]);

			} catch (final Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected LevelChangeEventListener getLevelChangeEventListener() {
			return tapeEventListener;
		}

		@Override
		protected int getPeakHistorySize() {
			return playPeakHistorySize;
		}

		@Override
		protected float getVolume() {
			return tapeVolume;
		}

	}

	private class RecordTask extends TapemachineAsyncTask {
		@Override
		protected Result doInBackground(final String... params) {
			BufferedOutputStream bos;
			DataOutputStream dos;
			FileOutputStream fos;
			int bufferReadResult;

			try {
				fos = contextWrapper.openFileOutput(RECORD_FILENAME,
						Context.MODE_PRIVATE);
				// final File file = new File(
				// Environment.getExternalStorageDirectory(),
				// RECORD_FILENAME);
				// fos = new FileOutputStream(file);

				bos = new BufferedOutputStream(fos);
				dos = new DataOutputStream(bos);

				audioRecord.startRecording();

				while (!isCancelled()) {
					bufferReadResult = audioRecord.read(recordBufferShort, 0,
							recordBufferSizeInByte / 4);

					for (int i = 0; i < bufferReadResult; i++) {
						dos.writeShort(recordBufferShort[i]);
					}
					publishProgress(recordBufferShort);

				}
				dos.close();
				bos.close();
				fos.close();
				audioRecord.stop();
				// Last value event must be 0
				publishProgress(new short[0]);

			} catch (final Exception e) {
				e.printStackTrace();
			}

			return null;

		}

		@Override
		protected LevelChangeEventListener getLevelChangeEventListener() {
			return tapeEventListener;
		}

		@Override
		protected int getPeakHistorySize() {
			return recordPeakHistorySize;
		}

		@Override
		protected float getVolume() {
			return tapeVolume;
		}

	}

	/** Dummy implementation */
	private class Result {

	}

	private enum Status {
		INACTIVE, ACTIVE
	}

	/** Tape machine's events for tape channel */
	interface TapeEventListener extends LevelChangeEventListener {

		/** Fires, when tape started playing */
		public void onStartPlaying();

		/** Fires, when tape started recording */
		public void onStartRecording();

		/** Fires, when tape stopped recording or playing */
		public void onStop(Mode fromMode);

	}

	/**
	 * Parameterized base class with three value String ,Short[] = actual
	 * buffer, Result
	 */
	private abstract class TapemachineAsyncTask extends
			AsyncTask<String, short[], Result> {

		// Biggest index in maxHistory
		// Ugly trap: The array size is fixed, but the buffer size will be
		// calculated at runtime
		private final short[] maxHistory = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0 };

		protected abstract LevelChangeEventListener getLevelChangeEventListener();

		/** Peak strategy needs the buffer size. */
		protected abstract int getPeakHistorySize();

		/** Value of volume for this module */
		protected abstract float getVolume();

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (getPeakHistorySize() > 20 || getPeakHistorySize() < 1)
				throw new RuntimeException("peakHistorySize is "
						+ getPeakHistorySize()
						+ " but must be between 1 and 20");

		}

		@Override
		protected void onProgressUpdate(final short[]... level) {

			// remove oldest max value
			// buffer size * history size is constant
			for (int i = getPeakHistorySize() - 1; i >= 1; i--) {
				maxHistory[i] = maxHistory[i - 1];
			}

			// This is our PeakStrategy:
			// Max of the last 10 buffers
			short actMax = 0;
			for (int i = 0; i < level[0].length; i++)
				if (level[0][i] > actMax)
					actMax = level[0][i];

			// Store max of actual buffer in our max history
			maxHistory[0] = actMax;

			// find max in history
			short newMax = 0;
			for (int i = 0; i < getPeakHistorySize(); i++)
				if (maxHistory[i] > newMax)
					newMax = maxHistory[i];

			// Level depends on
			getLevelChangeEventListener().onLevelChange(
					(short) (newMax * getVolume()));
		}

	}

	// Buffer must be bigger than minimum buffer size
	private static final int RECORD_BUFFER_FACTOR = 10;
	private static final int PLAY_BUFFER_FACTOR = 10;

	private static final String RECORD_FILENAME = "loop1.pcm";

	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int FREQUENCY = 44100;
	private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private TapemachineAsyncTask playTask;
	private TapemachineAsyncTask recordTask;
	private TapemachineAsyncTask liveTask;

	private static final String TAG = "Tapemachine";

	/** mute play signal */
	private boolean tapeMute;

	private final TapeEventListener tapeEventListener;
	private final LiveEventListener liveEventListener;
	private Status tapeStatus = Status.INACTIVE;
	private Status liveStatus = Status.INACTIVE;
	private Mode tapeMode = Mode.RECORDING;

	// --- For recording ---//
	private int recordBufferSizeInByte;
	private final int recordPeakHistorySize;
	private short[] recordBufferShort;
	private AudioRecord audioRecord;

	// --- For live routing ---- //
	private int liveBufferSizeInByte;
	private final int livePeakHistorySize;
	private short[] liveBufferShort;
	private AudioRecord audioLive;

	private int playBufferSizeInByte;
	private final int playPeakHistorySize;
	private AudioTrack audioTrack;
	private short[] playBufferShort;

	// For file access
	private final ContextWrapper contextWrapper;
	private float tapeVolume;
	private float liveVolume;
	private AudioService audioService;

	public Tapemachine(final TapeEventListener tapeEventListener,
			final LiveEventListener liveEventListener,
			final ContextWrapper contextWrapper) {
		this.tapeEventListener = tapeEventListener;
		this.liveEventListener = liveEventListener;
		this.contextWrapper = contextWrapper;

		try {

			recordBufferSizeInByte = AudioRecord.getMinBufferSize(FREQUENCY,
					CHANNEL_CONFIG, AUDIO_ENCODING) //
					* RECORD_BUFFER_FACTOR;

			Log.v(TAG, "Set recordBufferSizeInBye=" + recordBufferSizeInByte);

			// A short has 2 bytes
			recordBufferShort = new short[recordBufferSizeInByte / 2];

			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING,
					recordBufferSizeInByte);

			// Live is configured like record
			liveBufferSizeInByte = recordBufferSizeInByte;
			liveBufferShort = new short[liveBufferSizeInByte / 2];
			audioLive = new AudioRecord(MediaRecorder.AudioSource.MIC,
					FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING,
					liveBufferSizeInByte);

			// Live has no volume control
			liveVolume = 1.0f;

			playBufferSizeInByte = AudioTrack.getMinBufferSize(FREQUENCY,
					AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING)// ;
					* PLAY_BUFFER_FACTOR;
			Log.v(TAG, "Set playBufferSizeInBye=" + playBufferSizeInByte);

			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, FREQUENCY,
					AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING,
					playBufferSizeInByte, AudioTrack.MODE_STREAM);

			// A short has 2 bytes
			playBufferShort = new short[playBufferSizeInByte / 2];

		} catch (final Throwable t) {
			Log.v(TAG,
					"An error occured during recording initialization"
							+ t.getMessage());
		}

		// For peak strategy we want to know the size of max value history
		recordPeakHistorySize = calculatePeakHistorySize(recordBufferSizeInByte);
		livePeakHistorySize = calculatePeakHistorySize(liveBufferSizeInByte);
		playPeakHistorySize = calculatePeakHistorySize(playBufferSizeInByte);
		Log.v(TAG, "Set recordPeakHistorySizes=" + recordPeakHistorySize);
		Log.v(TAG, "Set livePeakHistorySizes=" + livePeakHistorySize);
		Log.v(TAG, "Set playPeakHistorySizes=" + playPeakHistorySize);

	}

	/** Opens live channel */
	public void becomeLiveOn() {
		Log.v(TAG, "becomeLiveOn()");
		if (isLiveOn()) {
			Log.v(TAG, "Already on! Nothing to do.");
			return;
		}
		liveTask = new LiveTask();
		liveTask.execute();
		liveStatus = Status.ACTIVE;
		liveEventListener.onStart();

		audioService.setInMute(false);
	}

	/** Stop live channel */
	public void becomeLiveStop() {
		Log.v(TAG, "becomeLiveStop()");
		// Hard stop both
		if (liveTask != null && !liveTask.isCancelled()) {
			liveTask.cancel(false);
		}

		liveStatus = Status.INACTIVE;
		liveEventListener.onStop();

		// TODO Move this to a encapsulating service
		audioService.setInMute(true);

	}

	/** Start tape channel to play */
	public void becomePlaying() {
		Log.v(TAG, "becomePlaying()");

		if (isTapeActive()) {
			if (isTapePlaying()) {
				Log.v(TAG, "Already playing! Nothing to do.");
				return;
			}
			if (isTapeRecording()) {
				Log.v(TAG, "first stop recording");
				recordTask.cancel(false);
			}
		}
		playTask = new PlayTask();
		playTask.execute();
		tapeMode = Mode.PLAYING;
		tapeStatus = Status.ACTIVE;
		tapeEventListener.onStartPlaying();
	}

	/** Switch tape channel to recording mode */
	public void becomeTapeRecording() {
		Log.v(TAG, "becomeRecording()");
		if (isTapeActive()) {
			if (isTapeRecording()) {
				Log.v(TAG, "Already recoding! Nothing to do.");
				return;
			}
			if (isTapePlaying()) {
				Log.v(TAG, "first stop playing");
				playTask.cancel(true);
			}
		}
		recordTask = new RecordTask();
		recordTask.execute();

		tapeStatus = Status.ACTIVE;
		tapeMode = Mode.RECORDING;
		tapeEventListener.onStartRecording();
	}

	/** Stop tape channel */
	public void becomeTapeStop() {
		Log.v(TAG, "becomeTapeStop()");
		// Hard stop both
		if (recordTask != null && !recordTask.isCancelled()) {
			recordTask.cancel(false);
		}
		if (playTask != null && !playTask.isCancelled()) {
			playTask.cancel(true);
		}

		tapeStatus = Status.INACTIVE;
		tapeEventListener.onStop(tapeMode.copy());
	}

	private int calculatePeakHistorySize(final int bufferSizeInByte) {
		if (bufferSizeInByte > 100000)
			return 1;
		else
			return ((100000 - bufferSizeInByte) / 20000) + 1;

	}

	/** Returns the possible maximum level as absolute value. */
	public short getMaxValue() {

		return (short) (Short.MAX_VALUE * AudioTrack.getMaxVolume());
	}

	public boolean isLiveOn() {
		return liveStatus.equals(Status.ACTIVE);
	}

	/** TRUE, if tape channel is not stopped, otherwise FALSE. */
	public boolean isTapeActive() {
		return tapeStatus.equals(Status.ACTIVE);

	}

	/** TRUE, if tape channel is playing, otherwise FALSE. */
	public boolean isTapePlaying() {
		return tapeStatus.equals(Status.ACTIVE)
				&& tapeMode.equals(Mode.PLAYING);
	}

	public boolean isTapeRecording() {
		return tapeStatus.equals(Status.ACTIVE)
				&& tapeMode.equals(Mode.RECORDING);
	}

	public void setAudioService(final AudioService audioService) {
		this.audioService = audioService;
	}

	/** Mute tape channel */
	public void setTapeMute(final boolean mute) {
		Log.v(TAG, "set tapeMute=" + mute);
		this.tapeMute = mute;
		if (mute)
			audioTrack.setStereoVolume(0f, 0f);
		else
			audioTrack.setStereoVolume(tapeVolume, tapeVolume);
	}

	public void setTapeVolume(final float volume) {
		Log.v(TAG, "set tapeVolume=" + volume);
		this.tapeVolume = volume;

		if (!tapeMute)
			audioTrack.setStereoVolume(volume, volume);

	}

}
