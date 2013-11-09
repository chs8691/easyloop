package de.egh.easyloop.logic;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

@Deprecated
public class StreamService extends Service {
	public interface EventListener {

		public void onInLevelChanged(short level);
	}

	private class RecordAsyncTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(final String... params) {
			Log.v(TAG, "RecordAsyncTask started");

			// try {
			Log.v(TAG, "start recording");

			audioRecord.startRecording();

			while (true) {
				final int bufferReadResult = audioRecord.read(buffers, 0,
						bufferSize);
				inLevel = 0;
				for (int i = 0; i < bufferReadResult; i++) {

					// find max of act. buffer
					if (buffers[i] > inLevel) {
						inLevel = buffers[i];
					}

				}

				// Fire event
				if (eventListener != null) {
					eventListener.onInLevelChanged(inLevel);
				}
				if (isCancelled()) {
					audioRecord.stop();
					break;
				}
			}
			return "";

		}

		@Override
		protected void onCancelled() {
			Log.v(TAG, "onCancelled");
			super.onCancelled();
		}
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class ServiceBinder extends Binder {
		StreamService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return StreamService.this;
		}
	}

	private EventListener eventListener;

	private short inLevel = 0;

	private int bufferSize;
	private short[] buffers;

	private AudioRecord audioRecord;
	public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

	public static final int FREQUENCY = 44100;
	// Binder given to clients
	private final IBinder mBinder = new ServiceBinder();
	private static final String TAG = "Service";

	private RecordAsyncTask recordAsyncTask;

	private SessionService jamsession;

	public SessionService getJamsession() {
		return jamsession;
	}

	@Override
	public IBinder onBind(final Intent intent) {
		Log.v(TAG, "onBind");
		// startRecordingInAsyncTask();

		return mBinder;
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate() ");
		jamsession = new SessionService();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy()");
		if (recordAsyncTask != null) {
			recordAsyncTask.cancel(true);
		}
	}

	/**
	 * Must be defined to let service alive after unbind it from application
	 * (e.g. when configuration changes)
	 */
	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		Log.v(TAG, "onStartCommand");
		return Service.START_NOT_STICKY;
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		Log.v(TAG, "onUnbind()");

		return super.onUnbind(intent);
	}

	public void setEventListener(final EventListener eventListener) {
		this.eventListener = eventListener;
	}

	private void startRecordingInAsyncTask() {
		Log.v(TAG, "startRecordingInAsyncTask");

		if (recordAsyncTask == null) {
			try {
				Log.v(TAG, "initialize audioRecord and start AsyncTask...");

				bufferSize = AudioRecord.getMinBufferSize(FREQUENCY,
						CHANNEL_CONFIG, AUDIO_ENCODING);

				Log.v(TAG, "buffer size = " + bufferSize);

				buffers = new short[bufferSize];

				audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
						FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING, bufferSize);
				recordAsyncTask = new RecordAsyncTask();
				recordAsyncTask.execute("");
			} catch (final Throwable t) {
				Log.v(TAG, "An error occured during recording initialization",
						t);
			}
		} else {
			Log.v(TAG, "AsyncTask still running (not null) ");
		}

	}

}
