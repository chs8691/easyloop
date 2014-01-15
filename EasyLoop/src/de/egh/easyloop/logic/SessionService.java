package de.egh.easyloop.logic;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.egh.easyloop.logic.audio.destination.AudioDestination;
import de.egh.easyloop.logic.audio.destination.RecorderDestination;
import de.egh.easyloop.logic.audio.destination.SpeakerDestination;
import de.egh.easyloop.logic.audio.source.AudioSource;
import de.egh.easyloop.logic.audio.source.AudioSource.LoopEventListener;
import de.egh.easyloop.logic.audio.source.MicSource;
import de.egh.easyloop.logic.audio.source.PlayerSource;

public class SessionService extends Service {

	/** Task for reading and dispatching the input signal. */
	private class MicTask extends AsyncTask<Null, Null, Null> {
		private static final String TAG = "MicTask";
		private final Null aNull = new Null();
		private AudioSource micSource;

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

					publishProgress(aNull);
				}

				Log.v(TAG, "In cancelled. Stop mic now...");
				micSource.stop();

				// Last value event must be 0
				publishProgress(aNull);

			} catch (final Exception e) {
				e.printStackTrace();
			}

			Log.v(TAG, "doInBackgroundTask() leave.");
			return aNull;
		}

		@Override
		protected void onPreExecute() {
			Log.v(TAG, "onPreExecute()");

			micSource = new MicSource();
		}

		@Override
		protected void onProgressUpdate(final Null... params) {
			if (recorder.isOpen())
				sessionEventListener.onTapeLevelChanged(recorder
						.getActualMaxLevel());
			if (liveDestination.isOpen())
				sessionEventListener.onLiveLevelChanged(liveDestination
						.getActualMaxLevel());

			sessionEventListener
					.onInLevelChanged(micSource.getActualMaxLevel());
		}

	}

	/** Dummy class */
	private class Null {
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

		/** Propagated loop counter. Will be set in the UI task. */
		public int uiTaskProgressCount = 0;
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

					publishProgress(playerProgress);
				}

				tapeDestination.close();

				player.stop();

				Log.v(TAG, "doInBackgroundTask() leave.");

				publishProgress(playerProgress);

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
		protected void onProgressUpdate(
				final PlayerProgress... playerProgresses) {

			// Update level view with every new buffer content
			if (tapeDestination.isOpen())
				sessionEventListener.onTapeLevelChanged(tapeDestination
						.getActualMaxLevel());

			// Start counter with every new loop
			// If new loop fire its duration
			if (playerProgresses[0].asyncTaskDuration != AudioSource.NO_DURATION
					&& playerProgresses[0].asyncTaskloopCount > playerProgresses[0].uiTaskProgressCount) {
				// Mark playerProgress as processed
				playerProgresses[0].uiTaskProgressCount = playerProgresses[0].asyncTaskloopCount;
				sessionEventListener
						.onLoopStart(playerProgresses[0].asyncTaskDuration);
			}

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
		 * In channel signal has been changed. Use this for showing the level.
		 */
		public void onInLevelChanged(short level);

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

		/** Recording started */
		public void onRecordStart();

		/** Recording stopped */
		public void onRecordStop();

		/**
		 * tape output signal has been changed. Use this for showing the level.
		 */
		public void onTapeLevelChanged(short Level);
	}

	private static final int ONGOING_PLAY_NOTIFICATION = 1;

	private static final int ONGOING_RECORD_NOTIFICATION = 2;

	private static final String TAG = "SessionService";

	private boolean foreground;
	private final AudioDestination liveDestination;

	private final IBinder mBinder = new ServiceBinder();

	private MicTask micTask;

	private PlayerSource player;

	private PlayerTask playerTask;

	private final RecorderDestination recorder;
	private SessionEventListener sessionEventListener;

	private final AudioDestination tapeDestination;

	public SessionService() {
		Log.v(TAG, "SessionService()");

		sessionEventListener = createSessionEventListenerDummy();

		recorder = new RecorderDestination(this);

		tapeDestination = new SpeakerDestination();
		tapeDestination.setVolume(100);
		tapeDestination.mute(false);

		liveDestination = new SpeakerDestination();
		liveDestination.setVolume(100);
		liveDestination.mute(true);

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
			public void onInLevelChanged(final short level) {
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
		return recorder.isOpen();
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
		if (!isPlaying() && !isRecording()) {
			stopSelf();
		}

		// Make notification
		else {
			final NotificationCompat.Builder builder = new NotificationCompat.Builder(
					getApplicationContext()).setSmallIcon(R.d)
			
			// startForeground(ONGOING_NOTIFICATION_ID, notification);
		}

		return super.onUnbind(intent);
	}

	public void removeSessionEventListener() {
		this.sessionEventListener = createSessionEventListenerDummy();
	}

	/** Must set set before first usage of live in switching on/off */
	public void setAudioService(final AudioService audioService) {
		// tapemachine.setAudioService(audioService);
	}

	/**
	 * If TRUE, mutes the play signal, otherwise un-mute. Only the audio
	 * destination will be set to mute, so the level is still fired.
	 */
	public void setLiveMute(final boolean mute) {
		liveDestination.mute(mute);
	}

	/** Adjust tape's output volume. */
	public void setLiveVolume(final int volume) {
		liveDestination.setVolume(volume);
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
	}

	/** Adjust tape's output volume. */
	public void setTapeVolume(final int volume) {
		tapeDestination.setVolume(volume);
	}

	/** Switch in signal on or off */
	public void switchIn(final boolean on) {
		Log.v(TAG, "switchIn() " + on);

		if (on) {
			if (micTask == null
					|| micTask.getStatus() == AsyncTask.Status.FINISHED) {
				micTask = new MicTask();
				micTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
						new Null());
				// Always open speaker output for this channel
				liveDestination.open();
			}
			// else inTask != null: Already running, nothing to do

		} else {
			if (micTask != null) {
				micTask.cancel(true);

				// Always close speaker out for this channel
				liveDestination.close();
			}
			// else Nothing to do, already off.
		}

	}

	public void tapePlay() {
		Log.v(TAG, "tapePlay()");
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

	public void tapeRecord() {
		Log.v(TAG, "tapeRecord()");
		if (playerTask != null
				&& playerTask.getStatus() != AsyncTask.Status.FINISHED) {
			playerTask.cancel(true);
			sessionEventListener.onPlayStop();
		}

		if (recorder.isOpen())
			recorder.close();
		recorder.open();

		sessionEventListener.onRecordStart();

	}

	public void tapeStop() {
		Log.v(TAG, "tapeStop()");
		if (recorder.isOpen()) {
			recorder.close();
			sessionEventListener.onRecordStop();
		}

		if (playerTask != null) {
			playerTask.cancel(true);
			sessionEventListener.onPlayStop();
		}

	}

}
