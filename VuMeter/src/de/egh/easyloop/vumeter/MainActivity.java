package de.egh.easyloop.vumeter;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class MainActivity extends Activity {

	private InputPad inputPad;
	private VuMeter vuMeter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		inputPad = (InputPad) findViewById(R.id.activityMainInputPad);
		vuMeter = (VuMeter) findViewById(R.id.activityMainVuMeter);

		inputPad.setListener(new InputPad.Listener() {

			@Override
			public void onTouchEvent(final int xPos) {
				// Not good: Has to be set only on (or until next rotation or
				// something else)
				vuMeter.setMaxValue(inputPad.getMax());

				// Update VU meter
				vuMeter.setValue(xPos);
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
