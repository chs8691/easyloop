package de.egh.easyloop.logic.audio.source;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import de.egh.easyloop.helper.Constants;
import de.egh.easyloop.logic.PeakStrategy;
import de.egh.easyloop.logic.audio.ReadResult;

/**
 * The InSignal knows the input source, and can receive and dispatch it's
 * signals
 */
public class MicSource implements AudioSource {

	private static final int BUFFER_FACTOR = 1;
	private static final String TAG = "MicSource";

	private AudioRecord audioRecord;
	private short[] bufferShort;
	private int bufferSizeInByte;
	private LoopEventListener loopEventListener;
	private boolean open;
	private PeakStrategy peakStrategy;
	private final ReadResultImplementation readResult;
	private int readSize;

	public MicSource() {
		Log.v(TAG, "MicSource()");

		open = false;

		try {

			bufferSizeInByte = AudioRecord.getMinBufferSize(
					Constants.AudioSettings.FREQUENCY,
					Constants.AudioSettings.CHANNEL_IN_CONFIG,
					Constants.AudioSettings.AUDIO_ENCODING) //
					* BUFFER_FACTOR;

			Log.v(TAG, "Set recordBufferSizeInBye=" + bufferSizeInByte);

			// A short has 2 bytes
			bufferShort = new short[bufferSizeInByte / 2];

			peakStrategy = new PeakStrategy(bufferSizeInByte);

		} catch (final Throwable t) {
			Log.v(TAG,
					"An error occured during recording initialization"
							+ t.getMessage());
		}

		readResult = new ReadResultImplementation();

		setLoopEventListener(null);
	}

	/**
	 * If running, the maximum of the last buffered signal will be returned,
	 * otherwise 0.
	 */
	@Override
	public short getActualMaxLevel() {
		if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
			return peakStrategy.getMax(bufferShort);
		else
			return 0;
	}

	@Override
	public ReadResult getReadResult() {
		readResult.setBuffer(bufferShort, readSize);
		return readResult;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void read() {
		readSize = audioRecord.read(bufferShort, 0, bufferSizeInByte / 4);
	}

	@Override
	public void setLoopEventListener(final LoopEventListener loopEventListener) {
		if (loopEventListener != null)
			this.loopEventListener = loopEventListener;
		else
			// Null-Object
			this.loopEventListener = new LoopEventListener() {

				@Override
				public void onNewLoopStart(final int duration) {
				}
			};
	}

	@Override
	public void start() {
		Log.v(TAG, "start()");

		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				Constants.AudioSettings.FREQUENCY,
				Constants.AudioSettings.CHANNEL_IN_CONFIG,
				Constants.AudioSettings.AUDIO_ENCODING, bufferSizeInByte);
		audioRecord.startRecording();
		open = audioRecord.getState() == AudioRecord.STATE_INITIALIZED;

		// Not really useful, but make it conform to the player source
		loopEventListener.onNewLoopStart(AudioSource.NO_DURATION);
	}

	@Override
	public void stop() {
		Log.v(TAG, "stop()");

		audioRecord.stop();
		audioRecord.release();
		audioRecord = null;

		open = false;

	}
}
