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

		// All this random access stuff is much to slow
		// // Fade in and out
		// try {
		// final RandomAccessFile raf = new RandomAccessFile(file, "rw");
		//
		// if (raf.length() > (Constants.AudioSettings.FADE_SIZE_SHORT * 2)) {
		// final float factor = 1.0f / Constants.AudioSettings.FADE_SIZE_SHORT;
		// short value;
		//
		// // Fade in
		// for (int i = 0; i <= Constants.AudioSettings.FADE_SIZE_SHORT; i++) {
		// // Point to the next short value
		// raf.seek(2 * i);
		// value = (short) (factor * i * raf.readShort());
		// raf.writeShort(value);
		// }
		//
		// // Fade out
		// // Set pointer to start position (in bytes)
		// long pos = raf.length()
		// - (Constants.AudioSettings.FADE_SIZE_SHORT * 2);
		// for (int i = Constants.AudioSettings.FADE_SIZE_SHORT; i >= 0; i--) {
		// raf.seek(pos);
		// value = (short) (factor * i * raf.readShort());
		// raf.writeShort(value);
		// // Set the pointer value to the next short
		// pos += 2;
		// }
		//
		// }
		// // Very, very small files will be completely nulled
		// else {
		// final byte[] buffer = new byte[(int) raf.length()];
		// raf.seek(0);
		// raf.write(buffer);
		// }
		// raf.close();
		//
		// } catch (final FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (final IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
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
