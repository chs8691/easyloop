package de.egh.easyloop.logic;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import de.egh.easyloop.MainActivity;
import de.egh.easyloop.R;
import de.egh.easyloop.logic.audio.source.MicSource;

@Deprecated
public class SessionServiceOld extends Service {
	interface EventListener extends Tapemachine.TapeEventListener {

	}

	/** Task for reading and dispatching the input signal. */
	private class InTask extends AsyncTask<Null, Null, Null> {
		private MicSource inSignal;
		private final Null aNull = new Null();

		@Override
		protected Null doInBackground(final Null... params) {

			try {

				inSignal.start();

				while (!isCancelled()) {

					inSignal.read();
					publishProgress(aNull);
				}

				inSignal.stop();

				// Last value event must be 0
				publishProgress(aNull);

			} catch (final Exception e) {
				e.printStackTrace();
			}

			return aNull;
		}

		@Override
		protected void onPreExecute() {
			inSignal = new MicSource();
		}

		@Override
		protected void onProgressUpdate(final Null... params) {
			sessionEventListener.onInLevelChanged(inSignal.getActualMaxLevel());
		}

	}

	/** Dummy class */
	private class Null {
	}

	/** Null-Object for convenience */
	private class NullSessionEventListener implements SessionEventListener {

		@Override
		public void onInLevelChanged(final short level) {
		}

		@Override
		public void onLiveLevelChanged(final short Level) {
		}

		@Override
		public void onPlay() {
		}

		@Override
		public void onRecording() {
		}

		@Override
		public void onStop() {
		}

		@Override
		public void onTapeLevelChanged(final short level) {
		}

	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class ServiceBinder extends Binder {
		public SessionServiceOld getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return SessionServiceOld.this;
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

		/** Started playing */
		public void onPlay();

		/** Recording started */
		public void onRecording();

		/** Tape machine stopped (playing or recording) */
		public void onStop();

		/**
		 * tape output signal has been changed. Use this for showing the level.
		 */
		public void onTapeLevelChanged(short Level);
	}

	private static final String TAG = "SessionService";

	private final IBinder mBinder = new ServiceBinder();
	private static final int ONGOING_PLAY_NOTIFICATION = 1;

	private static final int ONGOING_RECORD_NOTIFICATION = 2;
	private final Tapemachine tapemachine;
	// private final Mainsignal mainsignal;

	private SessionEventListener sessionEventListener;

	private boolean foreground;

	private InTask inTask;

	public SessionServiceOld() {
		Log.v(TAG, "SessionService()");

		tapemachine = new Tapemachine(new Tapemachine.TapeEventListener() {

			@Override
			public void onLevelChange(final short level) {
				sessionEventListener.onTapeLevelChanged(level);
			}

			@Override
			public void onStartPlaying() {
				// final Notification notification = new Notification(
				// R.drawable.ic_launcher, "Playing",
				// System.currentTimeMillis());
				// final Intent notificationIntent = new Intent(
				// SessionService.this, MainActivity.class);
				// final PendingIntent pendingIntent =
				// PendingIntent.getActivity(
				// SessionService.this, 0, notificationIntent, 0);
				// notification.setLatestEventInfo(SessionService.this,
				// "Playing",
				// "Play in loop mode", pendingIntent);
				// manageForegroudMode(notification);
				foregroundModeOn();
				sessionEventListener.onPlay();

			}

			@Override
			public void onStartRecording() {
				// final Notification notification = new Notification(
				// R.drawable.ic_launcher, "Recording",
				// System.currentTimeMillis());
				// final Intent notificationIntent = new Intent(
				// SessionService.this, MainActivity.class);
				// final PendingIntent pendingIntent =
				// PendingIntent.getActivity(
				// SessionService.this, 0, notificationIntent, 0);
				// notification.setLatestEventInfo(SessionService.this,
				// "Recording", "Recording from mic/line in",
				// pendingIntent);
				// manageForegroudMode(notification);
				foregroundModeOn();
				sessionEventListener.onRecording();
			}

			@Override
			public void onStop(final Tapemachine.Mode fromMode) {
				// manageForegroudMode(null);
				foregroundModeOff();
				sessionEventListener.onStop();

			}
		}, new Tapemachine.LiveEventListener() {

			@Override
			public void onLevelChange(final short level) {
				sessionEventListener.onLiveLevelChanged(level);
			}

			@Override
			public void onStart() {
				Log.v(TAG, "LiveEventListener.onStart() ");

				final Notification notification = new Notification(
						R.drawable.ic_launcher, "Input on",
						System.currentTimeMillis());
				final Intent notificationIntent = new Intent(
						SessionServiceOld.this, MainActivity.class);
				final PendingIntent pendingIntent = PendingIntent.getActivity(
						SessionServiceOld.this, 0, notificationIntent, 0);
				notification.setLatestEventInfo(SessionServiceOld.this,
						"Input on", "Input signal on", pendingIntent);

				// manageForegroudMode(notification);
				foregroundModeOn();

			}

			@Override
			public void onStop() {
				Log.v(TAG, "LiveEventListener.onStop()");
				// manageForegroudMode(null);
				foregroundModeOff();
			}
		}

		, this);

		sessionEventListener = new NullSessionEventListener();

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

	/** Returns the most possible value */
	public short getMaxLevel() {
		return tapemachine.getMaxValue();
	}

	public boolean isPlaying() {
		return tapemachine.isTapePlaying();
	}

	public boolean isRecording() {
		return tapemachine.isTapeRecording();
	}

	public void liveOff() {
		tapemachine.becomeLiveStop();
	}

	// if (tapemachine.isLiveOn() || tapemachine.isTapeActive()) {
	// Log.v(TAG, "manageForegroundMode:start");
	// foreground = true;
	// startForeground(ONGOING_RECORD_NOTIFICATION, notification);
	// } else {
	// Log.v(TAG, "manageForegroundMode:stop");
	// stopForeground(true);
	// }
	//
	// }

	public void liveOn() {
		tapemachine.becomeLiveOn();
	}

	@Override
	public IBinder onBind(final Intent intent) {
		Log.v(TAG, "onBind()");
		return mBinder;
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		Log.v(TAG, "onStartCommand()");

		// We don't want this service to continue running after canceled by the
		// system.
		return START_NOT_STICKY;
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		Log.v(TAG, "onUnbind()");

		// end service, if not in use
		if (!tapemachine.isTapeActive()) {
			stopSelf();
		}

		return super.onUnbind(intent);
	}

	public void removeSessionEventListener() {
		this.sessionEventListener = new NullSessionEventListener();
	}

	/** Must set set before first usage of live in switching on/off */
	public void setAudioService(final AudioService audioService) {
		tapemachine.setAudioService(audioService);
	}

	public void setSessionEventListener(
			final SessionEventListener sessionEventListener) {
		this.sessionEventListener = sessionEventListener;

	}

	/** If TRUE, mute the play signal, otherwise un-mute. */
	public void setTapeMute(final boolean mute) {
		tapemachine.setTapeMute(mute);
	}

	/** */
	public void setTapeVolume(final int volume) {
		tapemachine.setTapeVolume(volume / 100f);
	}

	/** Switch in signal on or off */
	public void switchIn(final boolean on) {
		if (on) {
			if (inTask == null
					|| inTask.getStatus() == AsyncTask.Status.FINISHED) {
				inTask = new InTask();
				inTask.execute(new Null());
			}
			// else inTask != null: Already running, nothing to do

		} else {
			if (inTask != null) {
				inTask.cancel(true);
			}
			// else Nothing to do, already off.
		}

	}

	public void tapePlay() {
		tapemachine.becomePlaying();
	}

	public void tapeRecord() {
		tapemachine.becomeTapeRecording();
	}

	public void tapeStop() {
		tapemachine.becomeTapeStop();
	}
}
