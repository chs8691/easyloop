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

						for (j = 0; dis.available() > 0 && (j < playBufferSize); j++) {
							playBuffer[j] = dis.readShort();
							if (playBuffer[j] > level) {
								level = playBuffer[j];
							}
						}
						publishProgress(playBuffer);

						audioTrack.write(playBuffer, 0, j);

						if (dis.available() <= 0) {
							end = true;
						}

					}
					dis.close();
					bis.close();
					is.close();
				}
				audioTrack.stop();

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
					bufferReadResult = audioRecord.read(recordBuffer, 0,
							recordBufferSize);

					for (int i = 0; i < bufferReadResult; i++) {
						dos.writeShort(recordBuffer[i]);
					}
					// publishProgress(recordBuffer[0]);

				}
				dos.close();
				bos.close();
				fos.close();
				audioRecord.stop();
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
			eventListener.onLevelChanged((short) 0);
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
			eventListener.onLevelChanged(max);
		}
	}

	private static final String RECORD_FILENAME = "loop1.pcm";

	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int FREQUENCY = 44100;
	private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private TapemachineAsyncTask playTask;
	private TapemachineAsyncTask recordTask;

	private static final String TAG = "Tapemachine";

	private final EventListener eventListener;
	private Status status = Status.INACTIVE;
	private Mode mode = Mode.RECORDING;

	// --- For recording ---//
	private int recordBufferSize;
	private short[] recordBuffer;
	private AudioRecord audioRecord;

	private int playBufferSize;
	private AudioTrack audioTrack;
	private short[] playBuffer;

	// For file access
	private final ContextWrapper contextWrapper;

	public Tapemachine(final EventListener eventListener,
			final ContextWrapper contextWrapper) {
		this.eventListener = eventListener;
		this.contextWrapper = contextWrapper;

		try {

			recordBufferSize = AudioRecord.getMinBufferSize(FREQUENCY,
					CHANNEL_CONFIG, AUDIO_ENCODING) //
			* 2; // for testing

			recordBuffer = new short[recordBufferSize];

			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING, recordBufferSize);

			playBufferSize = AudioTrack.getMinBufferSize(FREQUENCY,
					AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING)// ;
			* 2; // for testing

			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, FREQUENCY,
					AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING,
					playBufferSize, AudioTrack.MODE_STREAM);

			playBuffer = new short[playBufferSize];

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

	public boolean isActive() {
		return status.equals(Status.ACTIVE);

	}

	public boolean isPlaying() {
		return status.equals(Status.ACTIVE) && mode.equals(Mode.PLAYING);
	}

	public boolean isRecording() {
		return status.equals(Status.ACTIVE) && mode.equals(Mode.RECORDING);
	}

}
