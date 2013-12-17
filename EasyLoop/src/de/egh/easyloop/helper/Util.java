package de.egh.easyloop.helper;

import android.media.AudioTrack;

public abstract class Util {
	/** Returns the most possible audio level in our little loop station world. */
	public static short getMaxLevel() {
		return (short) (Short.MAX_VALUE * AudioTrack.getMaxVolume());
	}
}
