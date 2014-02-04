package de.egh.easyloop.logic.audio.destination;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import de.egh.easyloop.application.Constants;
import de.egh.easyloop.helper.Util;
import de.egh.easyloop.logic.PeakStrategy;
import de.egh.easyloop.logic.audio.ReadResult;

/** Streams an audio channel into a file. */
public class RecorderDestination implements AudioDestination {

	private static final String TAG = "RecordDestination";
	private BufferedOutputStream bos;
	private final short[] buffer;
	private int bufferSize;
	private final ContextWrapper contextWrapper;
	private DataOutputStream dos;
	private boolean fadeIn;
	private FileOutputStream fos;
	private boolean mute;
	private boolean open = false;
	private PeakStrategy peakStrategy;
	private long startTime;
	private int volume;

	/** The recorder can be created in the UI thread. */
	public RecorderDestination(final ContextWrapper contextWrapper) {
		Log.v(TAG, "RecorderDestination()");
		this.contextWrapper = contextWrapper;
		volume = 100;

		// prevent null pointer exception
		buffer = Util.createBuffer();

		startTime = 0;

	}

	@Override
	public void close() {
		Log.v(TAG, "close()");

		// Nothing to do, already closed.
		if (!open)
			return;

		open = false;
		startTime = 0;

		try {
			// Close file
			dos.close();

			bos.close();
			fos.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final File file = contextWrapper
				.getFileStreamPath(Constants.AudioSettings.RECORD_FILENAME);

		Log.v(TAG, "Wrote to file " + file.getPath() + " " + file.length());

	}

	/**
	 * Don't call this before the first write(), because the PeakStrategy will
	 * be set there.
	 */
	@Override
	public short getActualMaxLevel() {

		if (peakStrategy == null)
			return 0;
		else
			return peakStrategy.getMax(buffer, bufferSize);
	}

	public int getActualTime() {
		if (isOpen())
			return (int) (System.currentTimeMillis() - startTime);
		else
			return 0;
	}

	@Override
	public int getVolume() {
		return volume;
	}

	@Override
	public boolean isMuted() {
		return mute;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void mute(final boolean mute) {
		this.mute = mute;
	}

	@Override
	public void open() {
		Log.v(TAG, "open()");

		// Reopening: close existing one
		if (open)
			close();

		try {

			fos = contextWrapper.openFileOutput(
					Constants.AudioSettings.RECORD_FILENAME,
					Context.MODE_PRIVATE);

			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
		} catch (final FileNotFoundException e) {
			this.open = false;
			e.printStackTrace();
		}
		this.open = true;
		startTime = System.currentTimeMillis();
		fadeIn = true;

	}

	@Override
	public void setVolume(final int volume) {
		this.volume = volume;
	}

	/** Copies read result and updates volume value */
	private void storeReadResult(final ReadResult readResult) {
		// buffer = readResult.getBuffer();
		readResult.copy(buffer);
		bufferSize = readResult.getSize();

		// Implement linear volume control
		if (volume < 100 && bufferSize > 0)
			for (int i = 0; i < bufferSize; i++)
				buffer[i] = (short) (buffer[i] * volume / 100);
	}

	@Override
	public void write(final ReadResult readResult) {

		// The Strategy depends of the source buffer size, so we have to set it
		// once here.
		if (peakStrategy == null)
			// peakStrategy = new
			// PeakStrategy(readResult.getBufferSizeInByte());
			peakStrategy = new PeakStrategy(Util.getBufferSizeInByte());

		storeReadResult(readResult);

		if (!open)
			throw new RuntimeException("Audio destination is not active.");

		// if (fadeIn) {
		// final int fadeSize = Math.min(bufferSize, 2000);
		// // Fade in
		// try {
		// for (int i = 0; i < fadeSize; i++)
		// dos.writeShort(0);
		// for (int i = fadeSize; i < bufferSize; i++) {
		// if (mute)
		// dos.writeShort(0);
		// else
		// dos.writeShort(buffer[i]);
		// }
		// } catch (final IOException e) {
		// e.printStackTrace();
		// }
		// }
		// // normal output
		// else
		for (int i = 0; i < bufferSize; i++) {
			try {
				if (mute)
					dos.writeShort(0);
				else
					dos.writeShort(buffer[i]);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

	}

}
