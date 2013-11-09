package de.egh.easyloop.demo.livemic;

import java.text.DecimalFormat;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ToggleButton;

public class LiveMicDemoMain2 extends Activity {
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

	private class SingnalRouterTask extends AsyncTask<Integer, String, Integer> {
		private static final String TAG = "SignalRouterTask";

		@Override
		protected Integer doInBackground(final Integer... params) {

			log(TAG, "doInBackground()");
			final short[] buffer = new short[bufferSizeShort];
			final int cnt = 0;
			int bufferReadResult;

			try {
				audioRecord.startRecording();
				audioTrack.play();
				while (!isCancelled()) {

					bufferReadResult = audioRecord.read(buffer, 0,
							bufferSizeShort);
					audioTrack.write(buffer, 0, bufferReadResult);

					// log(TAG, "cnt=" + ++cnt);

				}
				log(TAG, "calling audioRecord.stop()");
				audioRecord.stop();
				audioTrack.stop();
				log(TAG, "Calling dos.close()");
				log(TAG, "finished!");

			} catch (final Exception e) {
				e.printStackTrace();
			}

			log(TAG, "Leaving doInBackground() cnt=" + cnt);

			return null;
		}

	}

	private int recordByteBufferSize;

	private AudioRecord audioRecord;
	private SingnalRouterTask recordTask;

	private final long starttime = System.currentTimeMillis();

	ToggleButton power;
	private static final String TAG = "MainActivity ";
	public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

	public static final int FREQUENCY = 44100;

	private AudioTrack audioTrack;
	private int playByteBufferSize;

	private int bufferSizeShort;
	private int bufferSizeByte;

	private void log(final String tag, final String text) {
		final DecimalFormat df = new DecimalFormat();
		Log.v(tag, df.format(System.currentTimeMillis() - starttime) + "  "
				+ text);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		power = (ToggleButton) findViewById(R.id.toggleButtonPower);

		power.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				log(TAG, "power.onClick: " + power.isChecked());
				if (power.isChecked()) {

					recordTask = new SingnalRouterTask();
					recordTask.executeOnExecutor(
							AsyncTask.THREAD_POOL_EXECUTOR, 0);

				} else {
					log(TAG, "calling recordTask.cancel()");
					recordTask.cancel(true);
					log(TAG, "recordTask.cancel() finished!");
				}
			}
		});

		try {

			recordByteBufferSize = AudioRecord.getMinBufferSize(FREQUENCY,
					CHANNEL_CONFIG, AUDIO_ENCODING);

			log(TAG, "buffer size = " + recordByteBufferSize);

			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					FREQUENCY, CHANNEL_CONFIG, AUDIO_ENCODING,
					recordByteBufferSize);

			playByteBufferSize = AudioTrack.getMinBufferSize(FREQUENCY,
					AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING);

			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, FREQUENCY,
					AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING,
					playByteBufferSize, AudioTrack.MODE_STREAM);
		} catch (final Throwable t) {
			log(TAG,
					"An error occured during recording initialization"
							+ t.getMessage());
		}
		bufferSizeByte = Math.min(playByteBufferSize, recordByteBufferSize);
		bufferSizeShort = bufferSizeByte / 2;

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
		audioTrack.release();

		super.onDestroy();
	}

}
