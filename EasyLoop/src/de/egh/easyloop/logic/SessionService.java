package de.egh.easyloop.logic;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import de.egh.easyloop.MainActivity;
import de.egh.easyloop.R;
import de.egh.easyloop.application.Constants;
import de.egh.easyloop.application.SettingsBuffer;
import de.egh.easyloop.logic.audio.destination.AudioDestination;
import de.egh.easyloop.logic.audio.destination.RecorderDestination;
import de.egh.easyloop.logic.audio.destination.SpeakerDestination;
import de.egh.easyloop.logic.audio.source.AudioSource;
import de.egh.easyloop.logic.audio.source.AudioSource.LoopEventListener;
import de.egh.easyloop.logic.audio.source.MicSource;
import de.egh.easyloop.logic.audio.source.PlayerSource;

public class SessionService extends Service {

	/**
	 * Progress type for the AsyncTask. Value holder for the lVU meter on the UI
	 */
	private class MicProgress {
		/** level to show for the VU meter of the live signal output */
		public short liveLevel = 0;
		/** level to show for the VU meter of the mic signal input */
		public short micLevel = 0;

		/** Resets all level values. Enabled for method chaining. */
		public MicProgress clearLevels() {
			liveLevel = 0;
			micLevel = 0;
			return this;
		}
	}

	/** Task for reading and dispatching the input signal. */
	private class MicTask extends AsyncTask<Null, MicProgress, Null> {
		private static final String TAG = "MicTask";
		private final Null aNull = new Null();
		private AudioSource micSource;
		private MicProgress progress;

		@Override
		protected Null doInBackground(final Null... params) {
			Log.v(TAG, "doInBackgroundTask()");

			try {

				micSource.start();

				while (!isCancelled()) {

					micSource.read();

					// Route to running recorder
					if (recorder.isOpen())
						recorder.write(micSource.getReadResult());

					if (liveDestination.isOpen())
						liveDestination.write(micSource.getReadResult());

					progress.micLevel = micSource.getActualMaxLevel();
					progress.liveLevel = liveDestination.getActualMaxLevel();
					publishProgress(progress);
				}

				Log.v(TAG, "In cancelled. Stop mic now...");
				micSource.stop();

				// Last value event must be 0
				publishProgress(progress.clearLevels());

			} catch (final Exception e) {
				e.printStackTrace();
			}

			Log.v(TAG, "doInBackgroundTask() leave.");
			return aNull;
		}

		@Override
		protected void onCancelled(final Null result) {
			Log.v(TAG, "onCancelled()");
			if (restartMic) {
				restartMic = false;
				Log.v(TAG, "restarting mic");
				micTask = new MicTask();
				micTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
						new Null());
				// Always open speaker output for this channel
				liveDestination.open();
				sessionEventListener.onInSwitched(false);

			}

		}

		@Override
		protected void onPreExecute() {
			Log.v(TAG, "onPreExecute()");

			micSource = new MicSource();
			progress = new MicProgress();
		}

		@Override
		protected void onProgressUpdate(final MicProgress... progresses) {
			if (liveDestination.isOpen())
				sessionEventListener
						.onLiveLevelChanged(progresses[0].liveLevel);

			sessionEventListener.onInLevelChanged(progresses[0].micLevel);
			// if (recorder.isOpen())
			// sessionEventListener.onTapeLevelChanged(recorder
			// .getActualMaxLevel());
			// if (liveDestination.isOpen())
			// sessionEventListener.onLiveLevelChanged(liveDestination
			// .getActualMaxLevel());
			//
			// sessionEventListener
			// .onInLevelChanged(micSource.getActualMaxLevel());
		}

	}

	/** Dummy class */
	private class Null {
		/** Actual level to propagate to the UI */
		public short level;
	}

	/**
	 * Progress type for PlayerTask to handle the loops. With every new loop of
	 * the played audio source, the SessionService consumer will be informed
	 * about this.
	 */
	private class PlayerProgress {

		/** Loop duration. Will be set in the AsyncTask by the PlayerSource. */
		public int asyncTaskDuration = AudioSource.NO_DURATION;

		/** Loop counter. Will be set in the AsyncTask by the PlayerSource. */
		public int asyncTaskloopCount = 0;

		/** level to show for the VU meter of the tape signal output */
		public short tapeLevel = 0;

		/** Propagated loop counter. Will be set in the UI task. */
		public int uiTaskProgressCount = 0;

		/** Resets all level values. Enabled for method chaining. */
		public PlayerProgress clearLevels() {
			tapeLevel = 0;
			return this;
		}
	}

	/**
	 * Task for playing the recording. The second type is the play duration as
	 * Integer. It will be set on start and with ever new loop. Otherwise it is
	 * null.
	 * 
	 */
	private class PlayerTask extends AsyncTask<Null, PlayerProgress, Null> {
		private static final String TAG = "PlayerTask";
		private final Null aNull = new Null();

		private final AudioSource player;

		/**
		 * Will be set, if a new loop is started and will be nulled, after
		 * firing this event.
		 */
		private final PlayerProgress playerProgress = new PlayerProgress();

		public PlayerTask(final AudioSource player) {
			this.player = player;
		}

		@Override
		protected Null doInBackground(final Null... params) {
			Log.v(TAG, "doInBackground()");

			try {

				player.start();

				// publishProgress(playerProgress);

				while (!isCancelled()) {

					player.read();

					if (tapeDestination.isOpen())
						tapeDestination.write(player.getReadResult());

					playerProgress.tapeLevel = tapeDestination
							.getActualMaxLevel();

					publishProgress(playerProgress);
				}

				tapeDestination.close();

				player.stop();

				Log.v(TAG, "doInBackgroundTask() leave.");

				publishProgress(playerProgress.clearLevels());

			} catch (final Exception e) {
				e.printStackTrace();
			}

			return aNull;
		}

		@Override
		protected void onPreExecute() {
			Log.v(TAG, "onPreExecute()");
			player.setLoopEventListener(new LoopEventListener() {

				@Override
				public void onNewLoopStart(final int duration) {
					playerProgress.asyncTaskDuration = duration;
					playerProgress.asyncTaskloopCount++;
				}
			});

		}

		@Override
		protected void onProgressUpdate(final PlayerProgress... progresses) {

			// Update level view with every new buffer content
			if (tapeDestination.isOpen())
				sessionEventListener
						.onTapeLevelChanged(progresses[0].tapeLevel);

			// Start counter with every new loop
			// If new loop fires its duration
			if (progresses[0].asyncTaskDuration != AudioSource.NO_DURATION
					&& progresses[0].asyncTaskloopCount > progresses[0].uiTaskProgressCount) {
				// Mark playerProgress as processed
				progresses[0].uiTaskProgressCount = progresses[0].asyncTaskloopCount;
				sessionEventListener
						.onLoopStart(progresses[0].asyncTaskDuration);
			}

		}
	}

	/**
	 * Manager for starting the recorder direct and count in. tapeRecord() may
	 * only be called from this class! Out of scope is to stop or to initialize
	 * the recorder.
	 */
	private class RecordStarter {

		private boolean counting;

		/** TRUE, if we have to count in when recording will be started */
		private boolean enabled;

		private long millisUntilFinished;

		private long time;

		/** May not be null, to avoid a NullPointerException */
		private CountDownTimer timer;

		public RecordStarter() {
			enabled = prefs.getBoolean(
					Constants.PersistantValues.TapeChannel.COUNT_IN_ENABLED,
					false);

			reset();

		}

		public int getActualTime() {
			return (int) (time - millisUntilFinished);
		}

		/** How long count in is set to */
		public int getDuration() {
			return (int) time;
		}

		/** Returns TRUE, if actual counting. */
		public boolean isCounting() {
			return counting;
		}

		/** Returns TRUE, if count in is enabled */
		public boolean isEnabled() {
			return enabled;
		}

		public void reset() {

			Log.v(TAG, "createTimer() with " + time + " ms");

			if (timer != null)
				timer.cancel();

			counting = false;
			millisUntilFinished = 0;

			time = SettingsBuffer.getInstance().getCountInTime();

			timer = new CountDownTimer(time, 100) {

				@Override
				public void onFinish() {

					counting = false;
					RecordStarter.this.millisUntilFinished = 0;
					// Do the work
					startRecording();
				}

				@Override
				public void onTick(final long millisUntilFinished) {
					RecordStarter.this.millisUntilFinished = millisUntilFinished;
				}
			};
		}

		/** Set to TRUE for switching on the count in. */
		public void setEnable(final boolean on) {

			enabled = on;

			prefs.edit()
					.putBoolean(
							Constants.PersistantValues.TapeChannel.COUNT_IN_ENABLED,
							enabled).commit();
			Log.v(TAG, "setCountIn() to " + enabled);
		}

		/**
		 * Starts recording directly or after the count in. Before calling this
		 * method, the setting for the recorder must be done.
		 */
		public void start() {

			if (enabled) {
				reset();
				counting = true;
				timer.start();
				sessionEventListener.onCountInStart((int) time);

			} else
				startRecording();
		}

		private void startRecording() {
			Log.v(TAG, "startRecording()");

			recorder.open();
			sessionEventListener.onRecordStart();

		}
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class ServiceBinder extends Binder {
		public SessionService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return SessionService.this;
		}

	}

	/** Events, which occurred in SessionService */
	public interface SessionEventListener {

		/**
		 * Count in started
		 * 
		 * @param countInTime
		 *            integer with time in milliseconds
		 */
		public void onCountInStart(int countInTime);

		/**
		 * In channel signal has been changed. Use this for showing the level.
		 */
		public void onInLevelChanged(short level);

		/** Fires with every switch of the in channel. */
		public void onInSwitched(boolean on);

		/**
		 * live output signal has been changed. Use this for showing the level.
		 */
		public void onLiveLevelChanged(short level);

		/**
		 * Event started new looping
		 * 
		 * @param duration
		 *            Integer with duration of this loop in milliseconds
		 */
		public void onLoopStart(int duration);

		/**
		 * Event started playing
		 * 
		 * @param asyncTaskDuration
		 *            Integer with duration in milliseconds
		 */
		public void onPlayStart();

		/** Playing stopped */
		public void onPlayStop();

		/** Will be fired after count in has finished. */
		public void onRecordStart();

		/** Recording stopped */
		public void onRecordStop();

		/**
		 * tape output signal has been changed. Use this for showing the level.
		 */
		public void onTapeLevelChanged(short Level);
	}

	private static final int NOTIFICATION_ID = 1;

	private static final int ONGOING_PLAY_NOTIFICATION = 1;

	private static final int ONGOING_RECORD_NOTIFICATION = 2;

	private static final String TAG = "SessionService";

	/**
	 * An new session must be initialized once with initialize(), but a rebinded
	 * serivce may not be initialized.
	 */
	private boolean initialize;

	private AudioDestination liveDestination;

	private final IBinder mBinder = new ServiceBinder();
	private MicTask micTask;

	private PlayerSource player;

	private PlayerTask playerTask;

	private SharedPreferences prefs;

	private RecorderDestination recorder;

	private RecordStarter recordStarter;

	/**
	 * Only for this case: With a configuration change, the mic will be stopped
	 * and after connecting to the server, it has to be started again. But we
	 * must take care, to start the mic AFTER it has canceled.
	 */
	private boolean restartMic;

	private SessionEventListener sessionEventListener;

	private AudioDestination tapeDestination;

	/** Call initialize() after constructing. */
	public SessionService() {

		Log.v(TAG, "SessionService()");

		initialize = false;

		sessionEventListener = createSessionEventListenerDummy();

	}

	public boolean canPlay() {
		if (player == null)
			return new PlayerSource(this).isAvailable();
		else
			return player.isAvailable();

	}

	/** Returns TRUE, if recording is possible. Otherwise FALSE. */
	public boolean canRecord() {
		return micTask != null && !micTask.isCancelled();
	}

	private SessionEventListener createSessionEventListenerDummy() {
		return new SessionEventListener() {

			@Override
			public void onCountInStart(final int countInTime) {
			}

			@Override
			public void onInLevelChanged(final short level) {
			}

			@Override
			public void onInSwitched(final boolean on) {
			}

			@Override
			public void onLiveLevelChanged(final short Level) {
			}

			@Override
			public void onLoopStart(final int duration) {
			}

			@Override
			public void onPlayStart() {
			}

			@Override
			public void onPlayStop() {
			}

			@Override
			public void onRecordStart() {
			}

			@Override
			public void onRecordStop() {
			}

			@Override
			public void onTapeLevelChanged(final short level) {
			}
		};
	}

	private void foregroundModeOff() {
		// if (foreground) {
		// stopForeground(true);
		// foreground = false;
		// }
	}

	/**
	 * Set service as foreground priority, if a background task is running,
	 * otherwise remove foreground priority. This method shoud be called, every
	 * time a async task has been started or stopped.
	 * 
	 * @param notification
	 *            Notification must be set, if called for starting a task. For
	 *            stopping task, value will be ignored.
	 */
	private void foregroundModeOn() {
		// if (!foreground) {
		// final Notification notification = new Notification(
		// R.drawable.ic_launcher, "Active",
		// System.currentTimeMillis());
		// final Intent notificationIntent = new Intent(SessionService.this,
		// MainActivity.class);
		// final PendingIntent pendingIntent = PendingIntent.getActivity(
		// SessionService.this, 0, notificationIntent, 0);
		// notification.setLatestEventInfo(SessionService.this, "Singnal",
		// "Deactivated", pendingIntent);
		//
		// startForeground(ONGOING_RECORD_NOTIFICATION, notification);
		// foreground = true;
		// }

	}

	public int getCountInActualTime() {
		return recordStarter.getActualTime();
	}

	public int getCountInDuration() {
		return recordStarter.getDuration();
	}

	public int getLiveVolume() {
		return liveDestination.getVolume();
	}

	public int getPlayerActualTime() {
		return player.getActualTime();
	}

	public int getPlayerDuration() {
		return player.getDuration();
	}

	public int getRecorderActualTime() {
		return recorder.getActualTime();
	}

	public int getTapeVolume() {
		return tapeDestination.getVolume();
	}

	/**
	 * Must be called directly after construction the service. Initialize the
	 * service for the very first time.
	 * 
	 * @param context
	 *            Activitie's context, needed for persisting actual settings
	 */
	public void initialize(final Context context) {
		Log.v(TAG, "initialize() with initialize=" + initialize);

		prefs = PreferenceManager.getDefaultSharedPreferences(context);

		// Only initialize once per lifetime
		if (!initialize) {

			recorder = new RecorderDestination(this);

			tapeDestination = new SpeakerDestination();
			tapeDestination.setVolume(prefs.getInt(
					Constants.PersistantValues.TapeChannel.VOLUME, 100));
			tapeDestination.mute(prefs.getBoolean(
					Constants.PersistantValues.TapeChannel.MUTED, false));

			liveDestination = new SpeakerDestination();
			liveDestination.setVolume(prefs.getInt(
					Constants.PersistantValues.LiveChannel.VOLUME, 100));
			liveDestination.mute(prefs.getBoolean(
					Constants.PersistantValues.LiveChannel.MUTED, true));

			recordStarter = new RecordStarter();

			initialize = true;
		}

		if (!isPlaying() && !isRecording())
			switchIn(prefs.getBoolean(
					Constants.PersistantValues.InChannel.SWITCH, false));
	}

	public boolean isCountInEnabled() {
		return recordStarter.isEnabled();
	}

	public boolean isCountingIn() {
		return recordStarter.isCounting();
	}

	public boolean isInOpen() {
		return micTask != null && micTask.getStatus() == Status.RUNNING;
	}

	public boolean isLiveMuted() {
		return liveDestination.isMuted();
	}

	public boolean isPlaying() {
		return playerTask != null
				&& playerTask.getStatus() == AsyncTask.Status.RUNNING;
	}

	public boolean isRecording() {
		return recorder != null && recorder.isOpen();
	}

	public boolean isTapeMuted() {
		return tapeDestination.isMuted();
	}

	@Override
	public IBinder onBind(final Intent intent) {
		Log.v(TAG, "onBind()");

		return mBinder;
	}

	@Override
	public void onRebind(final Intent intent) {
		Log.v(TAG, "onRebind()");
		stopForeground(true);

		super.onRebind(intent);
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		Log.v(TAG, "onStartCommand( ) ");

		// We don't want this service to continue running after canceled by the
		// system.
		return START_NOT_STICKY;
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		Log.v(TAG, "onUnbind()");

		// end service, if not in use
		if (!isPlaying() && !isRecording()
		// && !(micTask != null && micTask.getStatus() ==
		// AsyncTask.Status.RUNNING)
		) {
			Log.v(TAG, "onUnbind(): Stop service.");
			initialize = false;
			// stop in channel is open
			if (micTask != null
					&& micTask.getStatus() == AsyncTask.Status.RUNNING)
				micTask.cancel(true);
			stopSelf();
		}

		// Make notification
		else {
			final NotificationCompat.Builder builder = new NotificationCompat.Builder(
					getApplicationContext())
					.setSmallIcon(R.drawable.ic_notification)
					.setContentTitle(getText(R.string.notificationTitle))
					.setContentText(getText(R.string.notificationText));

			// Creates an explicit intent for an Activity in your app
			final Intent resultIntent = new Intent(this, MainActivity.class);

			// The stack builder object will contain an artificial back stack
			// for the
			// started Activity.
			// This ensures that navigating backward from the Activity leads out
			// of
			// your application to the Home screen.
			final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			// Adds the back stack for the Intent (but not the Intent itself)
			stackBuilder.addParentStack(MainActivity.class);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);
			final PendingIntent resultPendingIntent = stackBuilder
					.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(resultPendingIntent);
			// final NotificationManager mNotificationManager =
			// (NotificationManager)
			// getSystemService(Context.NOTIFICATION_SERVICE);
			// // mId allows you to update the notification later on.
			// mNotificationManager.notify(NOTIFICATION_ID, builder.build());

			Log.v(TAG, "onUnbind(): calling startForeground.");
			startForeground(NOTIFICATION_ID, builder.build());
		}

		return true;
	}

	public void removeSessionEventListener() {
		this.sessionEventListener = createSessionEventListenerDummy();
	}

	// /** Must set set before first usage of live in switching on/off */
	// public void setAudioService(final AudioService audioService) {
	// // tapemachine.setAudioService(audioService);
	// }

	/** Set to TRUE for switching on the count in. */
	public void setCountIn(final boolean on) {
		recordStarter.setEnable(on);

	}

	/**
	 * If TRUE, mutes the play signal, otherwise un-mute. Only the audio
	 * destination will be set to mute, so the level is still fired.
	 */
	public void setLiveMute(final boolean mute) {
		liveDestination.mute(mute);
		prefs.edit()
				.putBoolean(Constants.PersistantValues.LiveChannel.MUTED,
						liveDestination.isMuted()).commit();
	}

	/** Adjust tape's output volume. */
	public void setLiveVolume(final int volume) {
		liveDestination.setVolume(volume);
		prefs.edit()
				.putInt(Constants.PersistantValues.LiveChannel.VOLUME,
						liveDestination.getVolume()).commit();

	}

	public void setSessionEventListener(
			final SessionEventListener sessionEventListener) {
		this.sessionEventListener = sessionEventListener;

	}

	/**
	 * If TRUE, mutes the play signal, otherwise un-mute. Only the audio
	 * destination will be set to mute, so the level is still fired.
	 */
	public void setTapeMute(final boolean mute) {
		tapeDestination.mute(mute);
		prefs.edit()
				.putBoolean(Constants.PersistantValues.TapeChannel.MUTED,
						tapeDestination.isMuted()).commit();
	}

	/** Adjust tape's output volume. */
	public void setTapeVolume(final int volume) {
		tapeDestination.setVolume(volume);
		prefs.edit()
				.putInt(Constants.PersistantValues.TapeChannel.VOLUME,
						tapeDestination.getVolume()).commit();
	}

	/** Switch in signal on or off */
	public void switchIn(final boolean on) {
		Log.v(TAG, "switchIn() " + on);

		if (on) {
			if (micTask == null
					|| micTask.getStatus() == AsyncTask.Status.FINISHED) {
				Log.v(TAG, "Mic not running.Starting...");
				micTask = new MicTask();
				micTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
						new Null());
				// Always open speaker output for this channel
				liveDestination.open();
				sessionEventListener.onInSwitched(true);
			} else {
				Log.v(TAG,
						"Mic still running. Waiting for finishing cancel process.");
				restartMic = true;
			}

		} else {
			if (micTask != null) {
				micTask.cancel(true);

				// Always close speaker out for this channel
				liveDestination.close();
				sessionEventListener.onInSwitched(false);
			}
			// else Nothing to do, already off.
		}

		// Save state
		prefs.edit()
				.putBoolean(Constants.PersistantValues.InChannel.SWITCH, on)
				.commit();

	}

	public void tapePlay() {
		Log.v(TAG, "tapePlay()");
		// Debug.startMethodTracing("/sdcard/tapePlay.trace");
		// Stop actual recording
		if (recorder.isOpen()) {
			recorder.close();
			sessionEventListener.onRecordStop();
		}

		tapeDestination.open();

		if (playerTask != null
				&& playerTask.getStatus() == AsyncTask.Status.RUNNING) {
			Log.v(TAG, "PlayerTask already playing: leave tapePlay()");

			return;
		}

		player = new PlayerSource(this);
		playerTask = new PlayerTask(player);
		playerTask
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Null());

		sessionEventListener.onPlayStart();

	}

	/**
	 * Starts the recording. If count in is ON, then the count in will start
	 * first. The recorder setup will be done here and the start mechanism will
	 * be delegate to the RecordStarter class.
	 */
	public void tapeRecord() {
		Log.v(TAG, "tapeRecord()");
		// Debug.startMethodTracing("/sdcard/tapeRecord.trace");

		if (playerTask != null
				&& playerTask.getStatus() != AsyncTask.Status.FINISHED) {
			playerTask.cancel(true);
			sessionEventListener.onPlayStop();
		}

		if (recorder.isOpen())
			recorder.close();

		recordStarter.start();

	}

	public void tapeStop() {
		Log.v(TAG, "tapeStop()");

		if (recordStarter.isCounting())
			recordStarter.reset();

		if (recorder.isOpen()) {
			recorder.close();
			sessionEventListener.onRecordStop();
		}

		if (playerTask != null) {
			playerTask.cancel(true);
			sessionEventListener.onPlayStop();
		}
		// Debug.stopMethodTracing();

	}
}
