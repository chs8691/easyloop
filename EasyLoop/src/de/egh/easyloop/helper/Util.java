package de.egh.easyloop.helper;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import de.egh.easyloop.application.Constants;
import de.egh.easyloop.application.SettingsBuffer;

public abstract class Util {

	/** Use this instead of constructing a short[] of yourself. */
	public static short[] createBuffer() {
		return new short[getBufferSizeInByte() / 2];
	}

	/**
	 * Calculates the buffer size in Byte for all audio buffers. Take care, that
	 * all used buffer have this size
	 **/
	public static int getBufferSizeInByte() {

		final long size = Math
				.max((int) (SettingsBuffer.getInstance().getBufferSizeFactor() * AudioRecord
						.getMinBufferSize(Constants.AudioSettings.FREQUENCY,
								Constants.AudioSettings.CHANNEL_IN_CONFIG,
								Constants.AudioSettings.AUDIO_ENCODING) //
				),
						(int) (SettingsBuffer.getInstance()
								.getBufferSizeFactor() * AudioTrack
								.getMinBufferSize(
										Constants.AudioSettings.FREQUENCY,
										AudioFormat.CHANNEL_OUT_MONO,
										Constants.AudioSettings.AUDIO_ENCODING)));

		return (int) size;
	}

	/** Returns the most possible audio level in our little loop station world. */
	public static short getMaxLevel() {
		return (short) (Short.MAX_VALUE * AudioTrack.getMaxVolume());
	}
}
