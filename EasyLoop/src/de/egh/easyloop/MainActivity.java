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
import de.egh.easyloop.helper.Util;
import de.egh.easyloop.logic.AudioService;
import de.egh.easyloop.logic.SessionService;
import de.egh.easyloop.logic.SessionService.SessionEventListener;
import de.egh.easyloop.ui.component.SliderSlot;
import de.egh.easyloop.ui.component.SliderSlot.EventListener;

public class MainActivity extends Activity {

	private final static String TAG = "MainActivity";
	private AudioManager audioManager;

	AudioService audioService = new AudioService() {

		@Override
		public boolean isInMute() {
			return audioManager.isMicrophoneMute();
		}

		@Override
		public void setInMute(final boolean mute) {
			Log.v(TAG, "AudioService.setInMute=" + mute);
			audioManager.setMicrophoneMute(mute);
		}
	};
	public String filename;

	private SliderSlot inSliderSlot;

	private SliderSlot liveSliderSlot;

	private ToggleButton play;
	private ToggleButton record;
	private SessionService.SessionEventListener sessionEventListener;
	private SessionService sessionService;
	private boolean sessionServiceBound = false;

	/** Defines callbacks for service binding, passed to bindService() */

	private ServiceConnection sessionServiceConnection;
	private Button stop;
	private SliderSlot tapeSliderSlot;

	/**
	 * Don't create ServiceConnection befor UI elements have been created, for
	 * instance after a configuration changen. Because a rebind service can
	 * receive events for dispatching to ui elements.
	 */
	private void createServiceConnection() {
		sessionServiceConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(final ComponentName className,
					final IBinder service) {
				Log.v(TAG, "onServiceConnected()");
				// We've bound to LocalService, cast the IBinder and get
				// LocalService instance
				final SessionService.ServiceBinder serviceBinder = (SessionService.ServiceBinder) service;
				sessionService = serviceBinder.getService();
				sessionServiceBound = true;

				// Simplest implementation of a AudioService. Only mic support.
				sessionService.setAudioService(audioService);

				sessionEventListener = new SessionEventListener() {
					@Override
					public void onInLevelChanged(final short level) {
						inSliderSlot.setLevel(level);
					}

					@Override
					public void onLiveLevelChanged(final short level) {
						liveSliderSlot.setLevel(level);
					}

					@Override
					public void onPlay() {
						Log.v(TAG, "onStartPlaying()");

						uiElementsOnStop();

						play.setChecked(true);
						play.setEnabled(false);

					}

					@Override
					public void onRecording() {
						Log.v(TAG, "onStartrecording()");

						uiElementsOnStop();

						record.setChecked(true);
						record.setEnabled(false);

						inSliderSlot.enableSwitchButton(false);
					}

					@Override
					public void onStop() {
						Log.v(TAG, "onStop()");

						uiElementsOnStop();
					}

					@Override
					public void onTapeLevelChanged(final short level) {
						tapeSliderSlot.setLevel(level);
					}

				};

				sessionService.setSessionEventListener(sessionEventListener);

				// Initialize Buttons
				record.setChecked(sessionService.isRecording());
				record.setEnabled(sessionService.canRecord()
						&& !sessionService.isRecording());
				play.setChecked(sessionService.isPlaying());
				play.setEnabled(!sessionService.isPlaying());

				// --- Mic In Slot -------------------------//
				inSliderSlot.setSwitchButton(sessionService.isInOpen());
				inSliderSlot.enableSwitchButton(!sessionService.isRecording());
				inSliderSlot.setMaxLevel(Util.getMaxLevel());
				inSliderSlot.setEventListener(new EventListener() {

					@Override
					public void onMuteButtonToggled(final boolean set) {
						// Live signal has no mute button
					}

					@Override
					public void onSwitchButtonToggled(final boolean set) {
						sessionService.switchIn(set);
						record.setEnabled(set);
					}

					@Override
					public void onVolumeChanged(final int volume) {
						// Live signal has no volume control
					}
				});

				// --- Tape Slot -------------------------//
				tapeSliderSlot.setMute(sessionService.isTapeMuted());
				tapeSliderSlot.setVolume(sessionService.getTapeVolume());
				tapeSliderSlot.setMaxLevel(Util.getMaxLevel());
				tapeSliderSlot.setEventListener(new EventListener() {

					@Override
					public void onMuteButtonToggled(final boolean mute) {
						sessionService.setTapeMute(mute);
					}

					@Override
					public void onSwitchButtonToggled(final boolean set) {
						// Tape button has no Switch button

					}

					@Override
					public void onVolumeChanged(final int volume) {
						sessionService.setTapeVolume(volume);

					}
				});

				// --- Live Out Slot -------------------------//
				liveSliderSlot.setMute(sessionService.isLiveMuted());
				liveSliderSlot.setVolume(sessionService.getLiveVolume());
				liveSliderSlot.setMaxLevel(Util.getMaxLevel());
				liveSliderSlot.setEventListener(new EventListener() {

					@Override
					public void onMuteButtonToggled(final boolean set) {
						sessionService.setLiveMute(set);
					}

					@Override
					public void onSwitchButtonToggled(final boolean set) {
						// Live Slot has no Switch button
					}

					@Override
					public void onVolumeChanged(final int volume) {
						sessionService.setLiveVolume(volume);
					}
				});

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
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		record = (ToggleButton) findViewById(R.id.buttonRecord);
		play = (ToggleButton) findViewById(R.id.buttonPlay);
		stop = (Button) findViewById(R.id.buttonStop);

		audioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);

		record.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				Log.v(TAG, "Record:onCLick()");
				sessionService.tapeRecord();
			}
		});
		play.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				Log.v(TAG, "Play:onCLick()");
				sessionService.tapePlay();
			}
		});
		stop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				Log.v(TAG, "Stop:onCLick()");
				sessionService.tapeStop();
			}
		});

		// Up- and down buttons connect to volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		tapeSliderSlot = (SliderSlot) findViewById(R.id.tapeSliderSlot);
		tapeSliderSlot.activateMuteButton(true);
		tapeSliderSlot.activateSwitchButton(false);

		// Live signal's slot has no volume control and can only be switched
		// on/off.
		inSliderSlot = (SliderSlot) findViewById(R.id.inSliderSlot);
		inSliderSlot.activateMuteButton(false);
		inSliderSlot.activateSwitchButton(true);
		inSliderSlot.setSeekBarEnable(false);

		liveSliderSlot = (SliderSlot) findViewById(R.id.liveSliderSlot);
		liveSliderSlot.activateMuteButton(true);
		liveSliderSlot.activateSwitchButton(false);

	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		Log.v(TAG, "onStart()");

		super.onStart();

		// Views must be created at this point in time.
		createServiceConnection();

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

			// Sometimes after redeploying, sessionService is null
			if (sessionService != null)
				sessionService.removeSessionEventListener();

			unbindService(sessionServiceConnection);
		}
	}

	private void uiElementsOnStop() {
		record.setChecked(false);
		record.setEnabled(sessionService.canRecord());

		play.setChecked(false);
		play.setEnabled(sessionService.canPlay());

		inSliderSlot.enableSwitchButton(true);
	}

}
