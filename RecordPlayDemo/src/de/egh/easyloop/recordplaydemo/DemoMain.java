package de.egh.easyloop.recordplaydemo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ToggleButton;

public class DemoMain extends Activity {
	class Clock {
		private long t1;
		private long t2;

		public void clock() {
			t2 = System.currentTimeMillis();
		}

		public long next() {
			t1 = t2;
			t2 = System.currentTimeMillis();
			return t2 - t1;
		}
	}

	private class PlayTask extends AsyncTask<Integer, String, Integer> {
		private static final String TAG = "PlayTask";

		@Override
		protected Integer doInBackground(final Integer... params) {

			log(TAG, "doInBackground()" + System.currentTimeMillis());
			try {
				boolean end = false;
				final int playBufferSize = AudioTrack
						.getMinBufferSize(FREQUENCY,
								AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING);

				final short[] playBuffer = new short[playBufferSize];

				final File file = new File(
						Environment.getExternalStorageDirectory(),
						RECORD_FILENAME);
				final InputStream is = new FileInputStream(file);
				// openFileInput(RECORD_FILENAME);

				final BufferedInputStream bis = new BufferedInputStream(is);
				AudioTrack audioTrack;
				audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						FREQUENCY, AudioFormat.CHANNEL_OUT_MONO,
						AUDIO_ENCODING, playBufferSize, AudioTrack.MODE_STREAM);
				audioTrack.play();
				final DataInputStream dis = new DataInputStream(bis);
				int peak;
				int j = 0;
				boolean first = true;

				while (!isCancelled() && !end) {

					if (first) {
						log(TAG, "First loop over readShort");
					}

					peak = 0;
					for (j = 0; dis.available() > 0 && (j < playBufferSize); j++) {
						playBuffer[j] = dis.readShort();
						peak = playBuffer[j] > peak ? playBuffer[j] : peak;
					}

					audioTrack.write(playBuffer, 0, j);

					if (dis.available() <= 0) {
						end = true;
					}
					first = false;
				}
				audioTrack.stop();
				dis.close();
				bis.close();
				is.close();

			} catch (final Exception e) {
				e.printStackTrace();
			}
			log(TAG, "Leaving doInBackground()");

			return null;
		}
	}

	private class RecordTask extends AsyncTask<Integer, String, Integer> {
		private static final String TAG = "RecordTask ";

		@Override
		protected Integer doInBackground(final Integer... params) {

			log(TAG, "doInBackground()");

			try {
				final File file = new File(
						Environment.getExternalStorageDirectory(),
						RECORD_FILENAME);
				final FileOutputStream fos = new FileOutputStream(file);

				log(TAG, Environment.getExternalStorageDirectory().toString());
				// openFileOutput(RECORD_FILENAME,
				// Context.MODE_PRIVATE);

				bos = new BufferedOutputStream(fos);
				dos = new DataOutputStream(bos);

				audioRecord.startRecording();
				int bufferReadResult;
				boolean first = true;
				while (!isCancelled()) {
					if (first) {
						log(TAG, "First calling audioRecord.read...");
					}
					bufferReadResult = audioRecord.read(recordBuffer, 0,
							recordBufferSize);
					if (first) {
						log(TAG, "Done!");
					}

					for (int i = 0; i < bufferReadResult; i++) {
						dos.writeShort(recordBuffer[i]);
					}
					first = false;
				}
				dos.close();
				bos.close();
				fos.close();
				audioRecord.stop();
			} catch (final Exception e) {
				e.printStackTrace();
			}

			// final File file = getFileStreamPath(RECORD_FILENAME);
			// final Date date = new Date(file.lastModified());
			// log(TAG, "Wrote to " + file.getAbsoluteFile() + " " +
			// file.length()
			// + " " + date.toString());

			log(TAG, "Leaving doInBackground()");

			return null;
		}

	}

	private static final String RECORD_FILENAME = "loop1.pcm";

	private BufferedOutputStream bos;
	private DataOutputStream dos;
	private int recordBufferSize;
	private short[] recordBuffer;
	private AudioRecord audioRecord;

	private RecordTask recordTask;
	private final long starttime = System.currentTimeMillis();
	private PlayTask playTask;
	ToggleButton kind;
	ToggleButton power;
	private static final String TAG = "MainActivity";
	public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int FREQUENCY = 44100;

	private void log(final String tag, final String text) {
		final DecimalFormat df = new DecimalFormat();
		Log.v(tag, df.format(System.currentTimeMillis() - starttime) + " "
				+ text);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		kind = (ToggleButton) findViewById(R.id.toggleButtonKind);
		power = (ToggleButton) findViewById(R.id.toggleButtonPower);

		power.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				log(TAG, "power.onClick: " + power.isChecked());
				if (power.isChecked()) {
					if (kind.isChecked()) {
						recordTask = new RecordTask();
						recordTask.execute(0);
					} else {
						playTask = new PlayTask();
						playTask.execute(0);
					}
				} else {
					if (recordTask != null && !recordTask.isCancelled()) {
						recordTask.cancel(true);
					}
					if (playTask != null && !playTask.isCancelled()) {
						playTask.cancel(true);
					}
				}
			}
		});

		kind.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				log(TAG, "kind.onClick: " + power.isChecked());
				if (kind.isChecked()) {
					if (power.isChecked()) {
						if (playTask != null && !playTask.isCancelled()) {
							playTask.cancel(true);
						}
						recordTask = new RecordTask();
						recordTask.execute(0);
					}

				} else {
					if (power.isChecked()) {
						if (recordTask != null && !recordTask.isCancelled()) {
							recordTask.cancel(true);
						}
						playTask = new PlayTask();
						playTask.execute(0);

					}
				}

			}
		});

		try {

			recordBufferSize = AudioRecord.getMinBufferSize(FREQUENCY,
					CHANNEL_CONFIG, AUDIO_ENCODING);

			log(TAG, "buffer size = " + recordBufferSize);

			recordBuffer = new short[recordBufferSize];

			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING, recordBufferSize);

		} catch (final Throwable t) {
			log(TAG,
					"An error occured during recording initialization"
							+ t.getMessage());
		}
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		audioRecord.release();
		super.onDestroy();
	}

}
