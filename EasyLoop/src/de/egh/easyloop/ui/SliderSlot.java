package de.egh.easyloop.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import de.egh.easyloop.R;
import de.egh.easyloop.R.id;
import de.egh.easyloop.R.layout;
import de.egh.easyloop.R.styleable;
import de.egh.easyloop.ui.component.VuMeter;

public class SliderSlot extends RelativeLayout {

	private final TextView title;
	private final VuMeter vuMeterView;
	private final TextView levelTextView;

	public SliderSlot(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		inflate(context, R.layout.slider_slot_view, this);
		title = (TextView) findViewById(R.id.sliderView_titleText);
		final SeekBar seekBar = (SeekBar) findViewById(R.id.sliderView_seekBar);
		vuMeterView = (VuMeter) findViewById(R.id.sliderView_vuMeter);
		levelTextView = (TextView) findViewById(R.id.sliderView_level);

		final TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.sliderSlot);

		// change views with attributes
		title.setText(a.getString(R.styleable.sliderSlot_sliderSlot_title));

		seekBar.setEnabled(a.getBoolean(
				R.styleable.sliderSlot_sliderSlot_seekBarEnabled, true));

		// we don't need a anymore
		a.recycle();

	}

	/**
	 * Update output level
	 * 
	 * @param level
	 *            integer with new level
	 */
	public void setLevel(final short level) {
	}
}
