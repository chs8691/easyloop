package de.egh.easyloop.logic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

@Deprecated
class LoopService extends Service {
	public interface EventListener {
		public void inPlaybackLevelChanged(short level);

		public void onInLevelChanged(short level);
	}

	private class RecordAsyncTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(final String... params) {
			Log.v(TAG, "RecordAsyncTask started");

			// try {
			Log.v(TAG, "start recording ");

			// os = new FileOutputStream(file);
			// bos = new BufferedOutputStream(os);
			// // bos = new BufferedOutputStream(pos);
			// dos = new DataOutputStream(bos);

			audioRecord.startRecording();

			while (true) {
				final int bufferReadResult = audioRecord.read(buffers, 0,
						bufferSize);
				inLevel = 0;
				for (int i = 0; i < bufferReadResult; i++) {
					// dos.writeShort(buffers[i]);

					// find max of act. buffer
					if (buffers[i] > inLevel) {
						inLevel = buffers[i];
					}

				}

				// Fire event
				if (eventListener != null) {
					eventListener.onInLevelChanged(inLevel);
				}

			}
			// Log.v(TAG, "stop recording");
			// audioRecord.stop();
			// dos.close();

			// } catch (final IOException e) {
			// Log.v(TAG, "An error occured during recording", e);
			// }
			// Log.v(TAG, "MyAsyncTask stopped");
			// return null;
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
	public class RecordBinder extends Binder {
		LoopService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return LoopService.this;
		}
	}

	private EventListener eventListener;
	private short inLevel = 0;

	private OutputStream os;

	private BufferedOutputStream bos;
	private DataOutputStream dos;
	private int bufferSize;
	private short[] buffers;
	private AudioRecord audioRecord;

	final File file = new File(Environment.getExternalStorageDirectory(),
			FILENAME);

	public boolean isRecording = false;
	private AudioTrack audioTrack;
	public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int FREQUENCY = 44100;

	private String filename;
	public final static String FILENAME = "loop.pcm";
	// Binder given to clients
	private final IBinder mBinder = new RecordBinder();

	private static final String TAG = "Service";
	private RecordAsyncTask recordAsyncTask;

	// private PipedOutputStream pos;
	// private PipedInputStream pis;

	private void initRecording() {
		// try {
		// file.createNewFile();
		// } catch (final IOException e) {
		// Log.v(TAG, "IO Exception, e");
		// }

		try {

			bufferSize = AudioRecord.getMinBufferSize(FREQUENCY,
					CHANNEL_CONFIG, AUDIO_ENCODING);

			Log.v(TAG, "buffer size = " + bufferSize);

			buffers = new short[bufferSize];

			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING, bufferSize);
			recordAsyncTask = new RecordAsyncTask();
			recordAsyncTask.execute("");
		} catch (final Throwable t) {
			Log.v(TAG, "An error occured during recording initialization", t);
		}

	}

	public boolean isPlaying() {
		return audioTrack != null;
	}

	@Override
	public IBinder onBind(final Intent intent) {
		Log.v(TAG, "onBind");
		// initRecording();

		return mBinder;
	}

	@Override
	public void onCreate() {
		filename = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/" + FILENAME;
		Log.v(TAG, "onCreate()  filename=" + filename);
		// pis = new PipedInputStream();
		// try {
		// pos = new PipedOutputStream(pis);
		// } catch (final IOException e) {
		// Log.v(TAG, "Couldn't create PipedOutputStream", e);
		// }
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		if (recordAsyncTask != null) {
			recordAsyncTask.cancel(true);
		}
		audioRecord.stop();
		stopRecording();
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		// TODO Auto-generated method stub
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

	public void startPlaying() {
		final File file = new File(Environment.getExternalStorageDirectory(),
				LoopService.FILENAME);

		// Short array to store audio track (16 bit so 2 bytes per short)
		final int audioLength = (int) (file.length() / 2);
		final short[] audio = new short[audioLength];

		try {
			final InputStream is = new FileInputStream(file);
			final BufferedInputStream bis = new BufferedInputStream(is);
			// final BufferedInputStream bis = new BufferedInputStream(pis);
			final DataInputStream dis = new DataInputStream(bis);

			int i = 0;
			while (dis.available() > 0) {
				audio[i] = dis.readShort();
				i++;
			}
			// Close input stream
			dis.close();

			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					LoopService.FREQUENCY, AudioFormat.CHANNEL_OUT_MONO,
					LoopService.AUDIO_ENCODING, audioLength,
					AudioTrack.MODE_STREAM);
			audioTrack
					.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {

						@Override
						public void onMarkerReached(final AudioTrack track) {
							// do nothing
						}

						/** Loop */
						@Override
						public void onPeriodicNotification(
								final AudioTrack track) {
							// Log.v(TAG, "onPeriodicNotification");
							audioTrack.write(audio, 0, audioLength);
						}
					});
			audioTrack.setPositionNotificationPeriod(audioLength);

			audioTrack.play();
			audioTrack.write(audio, 0, audioLength);
		} catch (final Throwable t) {
			Log.v(TAG, "An error occurred during playpack!", t);
		}
	}

	public void startRecording() {
		isRecording = true;
		// new RecordAsyncTask().execute("");
	}

	public void stopPlaying() {
		if (audioTrack != null) {
			audioTrack.pause();
			audioTrack.flush();
		}
	}

	public void stopRecording() {
		isRecording = false;
		// TODO thread reagiert leider nicht mehr auf false
	}

}
