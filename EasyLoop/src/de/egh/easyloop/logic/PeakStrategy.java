package de.egh.easyloop.logic;

import android.util.Log;

/**
 * Slow down the quick changing level values for a better visualization. First
 * simple implementation without own timer task.
 * 
 * @author ChristianSchulzendor
 * 
 */
public class PeakStrategy {

	private final static String TAG = "PeakStrategy";
	// Biggest index in maxHistory
	// Ugly trap: The array size is fixed, but the buffer size will be
	// calculated at runtime
	private final short[] maxHistory = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0 };

	private final int peakHistorySize;

	/**
	 * Creates a very easy peak strategy for the given buffer size.
	 * BufferSizeInByte is used to calculate the range of history data, the
	 * calculation is based on: For a big buffer size, a small history will be
	 * used; for a small buffer size a long history is needed. So the consumer
	 * can adjust the strategy by this value.
	 * 
	 * @param bufferSizeInByte
	 *            Integer with size of buffered audio signal, used for getMax().
	 *            A small value makes the needle more nervous than a large
	 *            value.
	 */
	public PeakStrategy(final int bufferSizeInByte) {
		// TODO hier geht's weiter.
		peakHistorySize = calculatePeakHistorySize(bufferSizeInByte);
		Log.v(TAG, "peakHistorySize=" + peakHistorySize);
	}

	/**
	 * Calculates the number of values, that will be used to calculate the
	 * maximum value. Every value is the maximum of a buffer.
	 * 
	 * @param bufferSizeInByte
	 *            Integer with the size of a buffer in Bytes.
	 * @return Integer with the calculate number of maximum values, a history is
	 *         build of. Between [1..20].
	 */
	private int calculatePeakHistorySize(final int bufferSizeInByte) {
		if (bufferSizeInByte > 100000)
			return 1;
		else
			return Math.min(((100000 - bufferSizeInByte) / 20000) + 1, 20);

	}

	/** Calculates the maximum value. */
	public short getMax(final short[] level) {
		// remove oldest max value
		// buffer size * history size is constant
		for (int i = peakHistorySize - 1; i >= 1; i--) {
			maxHistory[i] = maxHistory[i - 1];
		}

		// This is our PeakStrategy:
		// Max of the last 10 buffers
		short actMax = 0;
		for (int i = 0; i < level.length; i++)
			if (level[i] > actMax)
				actMax = level[i];

		// Store max of actual buffer in our max history
		maxHistory[0] = actMax;

		// find max in history
		short newMax = 0;
		for (int i = 0; i < peakHistorySize; i++)
			if (maxHistory[i] > newMax)
				newMax = maxHistory[i];

		return newMax;
	}

}
