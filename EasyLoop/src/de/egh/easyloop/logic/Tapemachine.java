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

public class Tapemachine {

	/** Tape machine's events */
	interface EventListener {

		public void onLevelChanged(short level);

		public void onStartPlaying();

		public void onStartRecording();

		public void onStopped(Mode fromMode);

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

	}

	/** Dummy implementation */
	private class Result {

	}

	private enum Status {
		INACTIVE, ACTIVE
	}

	/**
	 * Parameterized base class with three value String ,Short[] = actual
	 * buffer, Result
	 */
	private abstract class TapemachineAsyncTask extends
			AsyncTask<String, short[], Result> {

		@Override
		protected void onCancelled() {
			Log.v(TAG, "onCancelled()");
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(final Result result) {
			Log.v(TAG, "onPostExecute() " + mode.toString());
		}

		@Override
		protected void onProgressUpdate(final short[]... level) {

			// This is our PeakStrategy:
			// Max of buffer
			short max = 0;
			for (int i = 0; i < level[0].length; i++)
				if (level[0][i] > max)
					max = level[0][i];

			// Level depends on
			eventListener.onLevelChanged((short) (max * volume));
		}
	}

	// Buffer must be bigger than minimum buffer size
	private static final int RECORD_BUFFER_FACTOR = 10;
	private static final int PLAY_BUFFER_FACTOR = 2;

	private static final String RECORD_FILENAME = "loop1.pcm";

	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int FREQUENCY = 44100;
	private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private TapemachineAsyncTask playTask;
	private TapemachineAsyncTask recordTask;

	private static final String TAG = "Tapemachine";

	/** mute play signal */
	private boolean mute;

	private final EventListener eventListener;
	private Status status = Status.INACTIVE;
	private Mode mode = Mode.RECORDING;

	// --- For recording ---//
	private int recordBufferSizeInByte;
	private short[] recordBufferShort;
	private AudioRecord audioRecord;

	private int playBufferSizeInByte;
	private AudioTrack audioTrack;
	private short[] playBufferShort;

	// For file access
	private final ContextWrapper contextWrapper;
	private float volume;

	public Tapemachine(final EventListener eventListener,
			final ContextWrapper contextWrapper) {
		this.eventListener = eventListener;
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

	}

	public void becomePlaying() {
		Log.v(TAG, "becomePlaying()");

		if (isActive()) {
			if (isPlaying()) {
				Log.v(TAG, "Already playing! Nothing to do.");
				return;
			}
			if (isRecording()) {
				Log.v(TAG, "first stop recording");
				recordTask.cancel(false);
			}
		}
		playTask = new PlayTask();
		playTask.execute();
		mode = Mode.PLAYING;
		status = Status.ACTIVE;
		eventListener.onStartPlaying();
	}

	public void becomeRecording() {
		Log.v(TAG, "becomeRecording()");
		if (isActive()) {
			if (isRecording()) {
				Log.v(TAG, "Already recoding! Nothing to do.");
				return;
			}
			if (isPlaying()) {
				Log.v(TAG, "first stop playing");
				playTask.cancel(true);
			}
		}
		recordTask = new RecordTask();
		recordTask.execute();
		status = Status.ACTIVE;
		mode = Mode.RECORDING;
		eventListener.onStartRecording();
	}

	public void becomeStop() {
		Log.v(TAG, "becomeStop()");
		// Hard stop both
		if (recordTask != null && !recordTask.isCancelled()) {
			recordTask.cancel(false);
		}
		if (playTask != null && !playTask.isCancelled()) {
			playTask.cancel(true);
		}

		status = Status.INACTIVE;
		eventListener.onStopped(mode.copy());
	}

	/** Returns the possible maximum level as absolute value. */
	public short getMaxValue() {

		return Short.MAX_VALUE;
	}

	public boolean isActive() {
		return status.equals(Status.ACTIVE);

	}

	public boolean isPlaying() {
		return status.equals(Status.ACTIVE) && mode.equals(Mode.PLAYING);
	}

	public boolean isRecording() {
		return status.equals(Status.ACTIVE) && mode.equals(Mode.RECORDING);
	}

	public void setMute(final boolean mute) {
		Log.v(TAG, "set mute=" + mute);
		this.mute = mute;
		if (mute)
			audioTrack.setStereoVolume(0f, 0f);
		else
			audioTrack.setStereoVolume(volume, volume);
	}

	public void setVolume(final float volume) {
		Log.v(TAG, "set volume=" + volume);
		this.volume = volume;
		if (!mute)
			audioTrack.setStereoVolume(volume, volume);

	}

}
