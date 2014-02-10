package de.egh.easyloop.ui.component;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
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
		 * Fires, when mute button has been toggled.
		 * 
		 * @param set
		 *            Boolean wit TRUE, if now is on, otherwise FALSE.
		 */
		public void onMuteButtonToggled(boolean set);

		/** Fires, when switch button has been toggled. */
		public void onSwitchButtonToggled(boolean set);

		/**
		 * Fires, when seek bar slider has been moved
		 * 
		 * @param volume
		 *            integer with new percentage value [0 .. 100]
		 */
		public void onVolumeChanged(int volume);
	}

	private EventListener eventListener;

	/** !MuteButton,not very nice. */
	private final ToggleButton openButton;
	private final SeekBar seekBar;
	private final ToggleButton switchButton;
	private final TextView title;
	private final VuMeter vuMeterView;

	public SliderSlot(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		inflate(context, R.layout.slider_slot_view, this);

		openButton = (ToggleButton) findViewById(R.id.sliderView_muteToggle);
		switchButton = (ToggleButton) findViewById(R.id.sliderView_switchToggle);
		vuMeterView = (VuMeter) findViewById(R.id.sliderView_vuMeter);
		title = (TextView) findViewById(R.id.sliderView_titleText);

		openButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
					final boolean isChecked) {
				if (eventListener != null) {
					eventListener.onMuteButtonToggled(!isChecked);
					vuMeterView.setMute(!isChecked);
				}
			}

		});

		switchButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
					final boolean isChecked) {
				if (eventListener != null) {
					eventListener.onSwitchButtonToggled(isChecked);
					vuMeterView.setMute(!isChecked);
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
		seekBar.setProgress(100);

		// we don't need a anymore
		a.recycle();

	}

	/**
	 * Hides this button in the UI. For setting activate to FALSE, mute will be
	 * set to FALSE. Don't use mute and switch button together!
	 */
	public void activateMuteButton(final boolean activated) {
		if (activated) {
			openButton.setEnabled(true);
			openButton.setVisibility(View.VISIBLE);
		} else {
			openButton.setChecked(false);
			openButton.setEnabled(false);
			openButton.setVisibility(View.GONE);
		}

	}

	/**
	 * Hides this button in the UI. For setting activated to FALSE, switch will
	 * be set to TRUE. Don't use mute and switch button together!
	 */
	public void activateSwitchButton(final boolean activated) {
		if (activated) {
			switchButton.setEnabled(true);
			switchButton.setVisibility(View.VISIBLE);

			// Switch button is off in default, so disable vu meter, too.
			vuMeterView.setMute(true);

		} else {
			switchButton.setChecked(true);
			switchButton.setEnabled(false);
			switchButton.setVisibility(View.GONE);

		}

	}

	/** En- or disables the switch button. */
	public void enableSwitchButton(final boolean enabled) {
		switchButton.setEnabled(enabled);
	}

	/** Returns actual value of the seek bar in percentage. */
	public int getVolume() {
		return seekBar.getProgress();
	}

	/** TRUE, if mute button is set, otherwise false */
	public boolean isMuted() {
		return !openButton.isChecked();
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

	/** Initializes muting without firing this as event. */
	public void setMute(final boolean mute) {
		openButton.setChecked(!mute);
		vuMeterView.setMute(mute);
	}

	/** Set to FALSE, if no SeekBar is required */
	public void setSeekBarEnable(final boolean enabled) {
		if (enabled) {
			seekBar.setEnabled(true);
			seekBar.setVisibility(View.VISIBLE);
		} else {
			seekBar.setProgress(seekBar.getMax());
			if (eventListener != null)
				eventListener.onVolumeChanged(seekBar.getProgress());
			seekBar.setEnabled(false);
			seekBar.setVisibility(View.GONE);
		}
	}

	public void setSwitchButton(final boolean on) {
		switchButton.setChecked(on);
		vuMeterView.setMute(!on);
	}

	public void setVolume(final int volume) {
		seekBar.setProgress(volume);
	}
}
