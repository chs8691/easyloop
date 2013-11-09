package de.egh.easyloop.logic;

/**
 * Slow down the quick changing level values for a better visualization. First
 * simple implementation without own timer task.
 * 
 * @author ChristianSchulzendor
 * 
 */
@Deprecated
class PeakStrategy {

	/**
	 * Provided Events.
	 * 
	 * @author ChristianSchulzendor
	 * 
	 */
	public interface PeakEventListener {
		/**
		 * Will be called when the strategy communicates a new level value.
		 */
		public void onPeakChanged(short level);
	}

	private final short[] levelArray = new short[1000];
	private int index = 0;

	private final PeakEventListener peakEventListener;

	public PeakStrategy(final PeakEventListener peakEventListener) {
		this.peakEventListener = peakEventListener;
	}

	/**
	 * Unfiltered incoming value
	 * 
	 * @param level
	 *            short with actual level value
	 */
	public void setLevel(final short level) {

		// Store last values in a ring buffer
		if (index >= levelArray.length)
			index = 0;
		levelArray[index++] = level;

		// Simple replacement for a timer
		if (index == 0) {
			short max = 0;
			for (int i = 0; i < levelArray.length; i++) {
				max = (short) Math.max(max, levelArray[0]);
			}
			peakEventListener.onPeakChanged(max);
		}

	}

}
