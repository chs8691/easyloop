package de.egh.easyloop.ui.component;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import de.egh.easyloop.R;

/** UI component for a signal slot */
public class SliderSlot extends RelativeLayout {

	/** Events for consumer. */
	public interface EventListener {
		/**
		 * Fires, when mute button was pressed
		 * 
		 * @param toMute
		 *            Boolean wit TRUE, if now mute is on, otherwise FALSE.
		 */
		public void onMuteToggled(boolean mute);

		/**
		 * Fires, when seek bar slider has been moved
		 * 
		 * @param volume
		 *            integer with new percentage value [0 .. 100]
		 */
		public void onVolumeChanged(int volume);
	}

	private final TextView title;
	private final VuMeter vuMeterView;
	private final TextView levelTextView;
	private final ToggleButton muteButton;
	private final SeekBar seekBar;
	private EventListener eventListener;

	public SliderSlot(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		inflate(context, R.layout.slider_slot_view, this);
		title = (TextView) findViewById(R.id.sliderView_titleText);
		vuMeterView = (VuMeter) findViewById(R.id.sliderView_vuMeter);
		levelTextView = (TextView) findViewById(R.id.sliderView_level);
		muteButton = (ToggleButton) findViewById(R.id.sliderView_muteToggle);

		muteButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
					final boolean isChecked) {
				if (eventListener != null) {
					eventListener.onMuteToggled(isChecked);
					vuMeterView.setMute(isChecked);
				}
			}

		});

		seekBar = (SeekBar) findViewById(R.id.sliderView_seekBar);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(final SeekBar seekBar,
					final int progress, final boolean fromUser) {
				if (eventListener != null) {
					eventListener.onVolumeChanged(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// Nothing to do
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				// Nothing to do
			}
		});

		final TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.sliderSlot);

		// change views with attributes
		title.setText(a.getString(R.styleable.sliderSlot_sliderSlot_title));

		seekBar.setEnabled(a.getBoolean(
				R.styleable.sliderSlot_sliderSlot_seekBarEnabled, true));

		// we don't need a anymore
		a.recycle();

	}

	/** TRUE, if mute button is set, otherwise false */
	public boolean getMute() {
		return muteButton.isChecked();
	}

	/** Returns actual value of the seek bar in percentage. */
	public int getVolume() {
		return seekBar.getProgress();
	}

	public void setEventListener(final EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Update output level
	 * 
	 * @param level
	 *            integer with new level
	 */
	public void setLevel(final short level) {

		vuMeterView.setValue(level);
	}

	/**
	 * To calibrate the VU meter, the maximum value must be set once before
	 * using setLevel.
	 */
	public void setMaxLevel(final short value) {
		vuMeterView.setMaxValue(value);

	}
}
