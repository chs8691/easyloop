package de.egh.easyloop;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import de.egh.easyloop.application.Constants;
import de.egh.easyloop.application.SettingsBuffer;
import de.egh.easyloop.helper.Util;
import de.egh.easyloop.logic.SessionService;
import de.egh.easyloop.logic.SessionService.SessionEventListener;
import de.egh.easyloop.ui.SettingsActivity;
import de.egh.easyloop.ui.component.SliderSlot;
import de.egh.easyloop.ui.component.SliderSlot.EventListener;
import de.egh.easyloop.ui.components.tapebutton.TapeButtonView;
import de.egh.easyloop.ui.components.tapebutton.TapeButtonView.Status;

public class MainActivity extends Activity {

	public static class AboutFragment extends DialogFragment {
		static AboutFragment newInstance() {
			final AboutFragment f = new AboutFragment();
			return f;
		}

		@Override
		public Dialog onCreateDialog(final Bundle savedInstanceState) {
			Log.v(TAG, "onCreateDialog()");
			final Dialog aboutDialog = super.onCreateDialog(savedInstanceState);

			aboutDialog.setContentView(R.layout.about);
			aboutDialog.setTitle(getText(R.string.about_title));

			final Button button = (Button) aboutDialog
					.findViewById(R.id.about_button_close);

			button.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(final View v) {
					aboutDialog.cancel();
				}
			});

			return aboutDialog;
		}

	}

	private final static String TAG = "MainActivity";

	private AudioManager audioManager;

	private ImageButton countInButton;

	public String filename;

	private SliderSlot inSliderSlot;
	private SliderSlot liveSliderSlot;

	private TapeButtonView play1Button;
	private SharedPreferences prefs;
	private TapeButtonView record1Button;
	private SessionService.SessionEventListener sessionEventListener;
	private SessionService sessionService;
	private boolean sessionServiceBound = false;
	/** Defines callbacks for service binding, passed to bindService() */

	private ServiceConnection sessionServiceConnection;

	private ImageButton stop;

	private ImageButton tapeCountInToggle;

	private SliderSlot tapeSliderSlot;

	private ImageButton volumeDown;

	private ImageButton volumeUp;

	private TextView volumeValue;

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

				// We want service to alive after unbind, if tapemachine active
				startService(new Intent(MainActivity.this, SessionService.class));

				// // Simplest implementation of a AudioService. Only mic
				// support.
				// sessionService.setAudioService(audioService);

				sessionService.initialize(MainActivity.this);

				sessionEventListener = new SessionEventListener() {

					@Override
					public void onCountInStart(final int countInTime) {
						Log.v(TAG, "onCountInstart()");

						record1Button.setEnabled(true);
						record1Button.setStatus(Status.COUNT_IN);
						record1Button.startCounterCountIn(countInTime, 0);

						inSliderSlot.enableSwitchButton(false);

					}

					@Override
					public void onInLevelChanged(final short level) {
						inSliderSlot.setLevel(level);
					}

					@Override
					public void onInSwitched(final boolean on) {
						record1Button.setEnabled(sessionService.canRecord());
					}

					@Override
					public void onLiveLevelChanged(final short level) {
						liveSliderSlot.setLevel(level);
					}

					@Override
					public void onLoopStart(final int duration) {
						play1Button.startCounterPlay(duration, 0);
					}

					@Override
					public void onPlayStart() {
						Log.v(TAG, "onStartPlaying()");

						play1Button.setStatus(Status.RUNNING);
						play1Button.setEnabled(sessionService.canPlay());

					}

					@Override
					public void onPlayStop() {
						Log.v(TAG, "onPlayStop()");

						play1Button.setStatus(Status.STOPPED);
						play1Button.setEnabled(sessionService.canPlay());

					}

					@Override
					public void onRecordStart() {
						Log.v(TAG, "onStartrecording()");

						record1Button.setEnabled(true);
						record1Button.setStatus(Status.RUNNING);
						record1Button.startCounterRecord(0);

						inSliderSlot.enableSwitchButton(false);

						// Not nice but simple: We assume that the recording
						// create a file that we can play.
						play1Button.setEnabled(true);
					}

					@Override
					public void onRecordStop() {
						Log.v(TAG, "onRecordStop()");

						record1Button.setStatus(Status.STOPPED);
						record1Button.setEnabled(sessionService.canRecord());

						// Created file can now be play backed
						play1Button.setEnabled(sessionService.canPlay());

						inSliderSlot.enableSwitchButton(true);

					}

					@Override
					public void onStreamVolumeChanged(final int volume) {
						volumeValue.setText("" + sessionService.getVolume());
					}

					@Override
					public void onTapeLevelChanged(final short level) {
						// if (level > 100)
						// Log.v(TAG, "Level=" + level);
						tapeSliderSlot.setLevel(level);
					}

				};

				sessionService.setSessionEventListener(sessionEventListener);

				// --- Record Button -----------------------//
				record1Button.setEnabled(sessionService.canRecord());

				if (sessionService.isCountingIn()) {
					record1Button.setStatus(TapeButtonView.Status.COUNT_IN);
					record1Button.startCounterCountIn(
							sessionService.getCountInDuration(),
							sessionService.getCountInActualTime());

				} else if (sessionService.isRecording()) {
					record1Button.setStatus(TapeButtonView.Status.RUNNING);
					record1Button.startCounterRecord(sessionService
							.getRecorderActualTime());

				} else {
					record1Button.setStatus(TapeButtonView.Status.STOPPED);
				}

				record1Button
						.setStatus(sessionService.isRecording() ? TapeButtonView.Status.RUNNING
								: TapeButtonView.Status.STOPPED);
				if (sessionService.isRecording())
					record1Button.startCounterRecord(sessionService
							.getRecorderActualTime());

				// --- Play Button -------------------------//
				play1Button.setEnabled(sessionService.canPlay());
				play1Button
						.setStatus(sessionService.isPlaying() ? TapeButtonView.Status.RUNNING
								: TapeButtonView.Status.STOPPED);
				if (sessionService.isPlaying())
					play1Button.startCounterPlay(
							sessionService.getPlayerDuration(),
							sessionService.getPlayerActualTime());

				// As long as there is no nice status behaviour for the image
				// toggel we have to set the image direct
				if (sessionService.isCountInEnabled())
					countInButton.setImageResource(R.drawable.count_in);
				else
					countInButton.setImageResource(R.drawable.direct_start);

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

						// TODO Switching record ability should be part of the
						// SessionService
						record1Button.setEnabled(set);
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

				tapeCountInToggle
						.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(final View v) {
								if (sessionService.isCountInEnabled()) {
									countInButton
											.setImageResource(R.drawable.direct_start);
									sessionService.setCountIn(false);

								} else {
									countInButton
											.setImageResource(R.drawable.count_in);
									sessionService.setCountIn(true);

								}
							}
						});

				// Volume buttons for stream volume
				volumeValue.setText("" + sessionService.getVolume());

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

		// Set all default values once for this application
		// This must be done in the 'Main' first activity
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// Initialize SettingsBuffer
		SettingsBuffer.getInstance().refreshSharedPreferences(this);

		audioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);

		tapeCountInToggle = (ImageButton) findViewById(R.id.tapeCountInToggle);

		stop = (ImageButton) findViewById(R.id.buttonStop);
		stop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				Log.v(TAG, "Stop:onCLick()");
				play1Button.stop();
				record1Button.stop();
			}
		});

		countInButton = (ImageButton) findViewById(R.id.tapeCountInToggle);
		// --- Tape buttons ---//
		play1Button = (TapeButtonView) findViewById(R.id.play1Button);

		play1Button.setEventListener(new TapeButtonView.EventListener() {

			@Override
			public void onRun() {
				Log.v(TAG, "play1Button:onRun()");
				sessionService.tapePlay();
			}

			@Override
			public void onStop() {
				Log.v(TAG, "play1Button:onStop()");
				sessionService.tapeStop();
			}
		});

		record1Button = (TapeButtonView) findViewById(R.id.record1Button);
		record1Button.setEventListener(new TapeButtonView.EventListener() {

			@Override
			public void onRun() {
				Log.v(TAG, "record1Button:onRun()");
				sessionService.tapeRecord();
			}

			@Override
			public void onStop() {
				Log.v(TAG, "record1Button:onStop()");
				sessionService.tapeStop();
			}

		});

		// Up- and down buttons connect to volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		tapeSliderSlot = (SliderSlot) findViewById(R.id.tapeSliderSlot);
		tapeSliderSlot.activateMuteButton(true);
		tapeSliderSlot.activateSwitchButton(false);

		inSliderSlot = (SliderSlot) findViewById(R.id.inSliderSlot);
		inSliderSlot.activateMuteButton(false);
		inSliderSlot.activateSwitchButton(true);
		// We can't change the input level
		inSliderSlot.setSeekBarEnable(false);

		liveSliderSlot = (SliderSlot) findViewById(R.id.liveSliderSlot);
		liveSliderSlot.activateMuteButton(true);
		liveSliderSlot.activateSwitchButton(false);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// --- Volume Control ----------------------------------//
		volumeValue = (TextView) findViewById(R.id.volumeValue);
		volumeUp = (ImageButton) findViewById(R.id.buttonUp);
		volumeUp.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				sessionService.volumeUp();
			}
		});
		volumeDown = (ImageButton) findViewById(R.id.buttonDown);
		volumeDown.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				sessionService.volumeDown();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {

		if (item.getItemId() == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;

		} else if (item.getItemId() == R.id.action_about) {
			showAboutDialog();
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume()");

		super.onResume();

		final String savedOrientation = prefs.getString(
				Constants.SharedPreferences.Orientation.KEY,
				Constants.SharedPreferences.Orientation.SENSOR);
		if (savedOrientation
				.equals(Constants.SharedPreferences.Orientation.LANDSCAPE)) {
			if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (savedOrientation
				.equals(Constants.SharedPreferences.Orientation.PORTRAIT)) {
			if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

		// Views must be created at this point in time.
		createServiceConnection();

		Log.v(TAG,
				"sessionServiceConnection="
						+ sessionServiceConnection.toString());

		sessionServiceBound = bindService(
				new Intent(this, SessionService.class),
				sessionServiceConnection, Context.BIND_AUTO_CREATE);

		Log.v(TAG, "bindService returned " + sessionServiceBound);

		// Screen always on
		if (prefs.getBoolean(Constants.SharedPreferences.KEEP_SCREEN_ON, false) == true) {
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

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

		// I'm not shure if we really need this
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	}

	/** For Logging from ActivityInfo. */
	private String orientationToString(final int orientation) {
		switch (orientation) {
		case ActivityInfo.SCREEN_ORIENTATION_BEHIND:
			return "SCREEN_ORIENTATION_BEHIND";
		case ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR:
			return "SCREEN_ORIENTATION_FULL_SENSOR";
		case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
			return "SCREEN_ORIENTATION_LANDSCAPE";
		case ActivityInfo.SCREEN_ORIENTATION_NOSENSOR:
			return "SCREEN_ORIENTATION_NOSENSOR";
		case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
			return "SCREEN_ORIENTATION_PORTRAIT";
		case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
			return "SCREEN_ORIENTATION_REVERSE_LANDSCAPE";
		case ActivityInfo.SCREEN_ORIENTATION_SENSOR:
			return "SCREEN_ORIENTATION_SENSOR";
		case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
			return "SCREEN_ORIENTATION_SENSOR_LANDSCAPE";
		case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:
			return "SCREEN_ORIENTATION_SENSOR_PORTRAIT";
		case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:
			return "SCREEN_ORIENTATION_UNSPECIFIED";
		case ActivityInfo.SCREEN_ORIENTATION_USER:
			return "SCREEN_ORIENTATION_USER";
		default:
			return "Unknown value=" + orientation;
		}
	}

	private int rotationToOrientation(final int rotation) {

		final Configuration config = getResources().getConfiguration();

		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Log.v(TAG, "Normal configuation is landscape");
			if (rotation == Surface.ROTATION_90)
				return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			else if (rotation == Surface.ROTATION_180)
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
			else if (rotation == Surface.ROTATION_270)
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
			else
				return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		}
		// Portrait (or non-landscape)
		else {
			Log.v(TAG, "Normal configuation is non-landscape");
			if (rotation == Surface.ROTATION_90)
				return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			else if (rotation == Surface.ROTATION_180)
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
			else if (rotation == Surface.ROTATION_270)
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
			else
				return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		}

	}

	/** For Logging from WindowsManager. */
	private String rotationToString(final int displayRotation) {
		switch (displayRotation) {
		case Surface.ROTATION_0:
			return "ROTATION_0";
		case Surface.ROTATION_180:
			return "ROTATION_180";
		case Surface.ROTATION_270:
			return "ROTATION_270";
		case Surface.ROTATION_90:
			return "ROTATION_90";
		default:
			return "Unknown value=" + displayRotation;
		}
	}

	private void showAboutDialog() {
		final FragmentManager fm = getFragmentManager();
		final DialogFragment aboutFragment = AboutFragment.newInstance();
		Log.v(TAG, "showAboutDialog()");
		aboutFragment.show(fm, "aboutDialog");
	}

	/** Update the UI components after a service's change. */
	private void uiElementsOnStop() {

		record1Button.setStatus(Status.STOPPED);
		record1Button.setEnabled(false);

		play1Button.setStatus(Status.STOPPED);
		play1Button.setEnabled(sessionService.canPlay());

		inSliderSlot.enableSwitchButton(true);
	}

}
