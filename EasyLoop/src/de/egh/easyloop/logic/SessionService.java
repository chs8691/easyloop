package de.egh.easyloop.logic;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import de.egh.easyloop.MainActivity;
import de.egh.easyloop.R;

public class SessionService extends Service {
	interface EventListener extends Tapemachine.EventListener {

	}

	/** Null-Object for convenience */
	private class NullSessionEventListener implements SessionEventListener {

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
		public SessionService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return SessionService.this;
		}
	}

	/** Events, which occurred in SessionService */
	public interface SessionEventListener {
		/** Playin started */
		public void onPlay();

		/** Recording started */
		public void onRecording();

		/** Tape machine stopped (playing or recording) */
		public void onStop();

		/**
		 * tape machines output signal has been changed. Use this for showing
		 * the level.
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

	public SessionService() {
		Log.v(TAG, "SessionService()");
		tapemachine = new Tapemachine(new Tapemachine.EventListener() {

			@Override
			public void onLevelChanged(final short level) {
				sessionEventListener.onTapeLevelChanged(level);
			}

			@Override
			public void onStartPlaying() {
				final Notification notification = new Notification(
						R.drawable.ic_launcher, "Playing",
						System.currentTimeMillis());
				final Intent notificationIntent = new Intent(
						SessionService.this, MainActivity.class);
				final PendingIntent pendingIntent = PendingIntent.getActivity(
						SessionService.this, 0, notificationIntent, 0);
				notification.setLatestEventInfo(SessionService.this, "Playing",
						"Play in loop mode", pendingIntent);
				startForeground(ONGOING_PLAY_NOTIFICATION, notification);
				sessionEventListener.onPlay();

			}

			@Override
			public void onStartRecording() {
				final Notification notification = new Notification(
						R.drawable.ic_launcher, "Recording",
						System.currentTimeMillis());
				final Intent notificationIntent = new Intent(
						SessionService.this, MainActivity.class);
				final PendingIntent pendingIntent = PendingIntent.getActivity(
						SessionService.this, 0, notificationIntent, 0);
				notification.setLatestEventInfo(SessionService.this,
						"Recording", "Recording from mic/line in",
						pendingIntent);
				startForeground(ONGOING_RECORD_NOTIFICATION, notification);
				sessionEventListener.onRecording();
			}

			@Override
			public void onStopped(final Tapemachine.Mode fromMode) {
				stopForeground(true);
				sessionEventListener.onStop();

			}
		}, this);
		// mainsignal = new Mainsignal();

		sessionEventListener = new NullSessionEventListener();

	}

	/** Returns the most possible value */
	public short getMaxLevel() {
		return tapemachine.getMaxValue();
	}

	public boolean isPlaying() {
		return tapemachine.isPlaying();
	}

	public boolean isRecording() {
		return tapemachine.isRecording();
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
		if (!tapemachine.isActive()) {
			stopSelf();
		}

		return super.onUnbind(intent);
	}

	public void play() {
		tapemachine.becomePlaying();
	}

	public void record() {
		tapemachine.becomeRecording();
	}

	public void removeSessionEventListener() {
		this.sessionEventListener = new NullSessionEventListener();
	}

	/** If TRUE, mute the play signal, otherwise un-mute. */
	public void setMute(final boolean mute) {
		tapemachine.setMute(mute);
	}

	public void setSessionEventListener(
			final SessionEventListener sessionEventListener) {
		this.sessionEventListener = sessionEventListener;

	}

	/** */
	public void setVolume(final int volume) {
		tapemachine.setVolume(volume / 100f);
	}

	public void stop() {
		tapemachine.becomeStop();
	}
}
