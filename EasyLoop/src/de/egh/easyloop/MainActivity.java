package de.egh.easyloop;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;
import de.egh.easyloop.logic.SessionService;
import de.egh.easyloop.logic.SessionService.SessionEventListener;
import de.egh.easyloop.ui.SliderSlot;

public class MainActivity extends Activity {

	private SessionService.SessionEventListener sessionEventListener;
	private final static String TAG = "MainActivity";

	private boolean sessionServiceBound = false;
	private SessionService sessionService;

	/** Defines callbacks for service binding, passed to bindService() */
	private final ServiceConnection sessionServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(final ComponentName className,
				final IBinder service) {
			Log.v(TAG, "onServiceConnected()");
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			final SessionService.ServiceBinder serviceBinder = (SessionService.ServiceBinder) service;
			sessionService = serviceBinder.getService();
			sessionServiceBound = true;

			sessionEventListener = new SessionEventListener() {
				@Override
				public void onPlay() {
					Log.v(TAG, "onStartPlaying()");
					record.setChecked(false);
					play.setChecked(true);
				}

				@Override
				public void onRecording() {
					Log.v(TAG, "onStartrecording()");
					record.setChecked(true);
					play.setChecked(false);
				}

				@Override
				public void onStop() {
					Log.v(TAG, "onStopped()");
					record.setChecked(false);
					play.setChecked(false);
				}

				@Override
				public void onTapeLevelChanged(final short level) {
					tapeSliderSlot.setLevel(level);
				}

			};

			sessionService.setSessionEventListener(sessionEventListener);

			// Initialize Buttons
			record.setChecked(sessionService.isRecording());
			play.setChecked(sessionService.isPlaying());

			// We want service to alive after unbind, if tapemachine active
			startService(new Intent(MainActivity.this, SessionService.class));
		}

		@Override
		public void onServiceDisconnected(final ComponentName arg0) {
			Log.v(TAG, "onServiceDisconnected");
			sessionService.removeSessionEventListener();
			sessionServiceBound = false;
		}
	};

	private ToggleButton record;
	private ToggleButton play;
	private Button stop;
	public String filename;
	private AudioManager audioManager;
	private SliderSlot inSliderSlot;
	private SliderSlot tapeSliderSlot;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		record = (ToggleButton) findViewById(R.id.buttonRecord);
		play = (ToggleButton) findViewById(R.id.buttonPlay);
		stop = (Button) findViewById(R.id.buttonStop);
		// inSliderSlot = (SliderSlot) findViewById(R.id.inSliderSlot);
		// tapeSliderSlot = (SliderSlot) findViewById(R.id.playbackSliderSlot);

		audioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);

		record.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				Log.v(TAG, "Record:onCLick()");
				sessionService.record();
			}
		});
		play.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				Log.v(TAG, "Play:onCLick()");
				sessionService.play();
			}
		});
		stop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				Log.v(TAG, "Stop:onCLick()");
				sessionService.stop();
			}
		});

		// Up- and down buttons connect to volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		Log.v(TAG, "onStart");

		super.onStart();
		Log.v(TAG,
				"sessionServiceConnection="
						+ sessionServiceConnection.toString());

		sessionServiceBound = bindService(
				new Intent(this, SessionService.class),
				sessionServiceConnection, Context.BIND_AUTO_CREATE);

		Log.v(TAG, "bindService returned " + sessionServiceBound);
	}

	@Override
	protected void onStop() {
		Log.v(TAG, "onStop");
		super.onStop();
		if (sessionServiceBound) {
			Log.v(TAG, "calling unbindService");
			sessionService.removeSessionEventListener();
			unbindService(sessionServiceConnection);
		}
	}

}
