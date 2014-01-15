package com.example.countercircledemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import de.egh.easyloop.ui.components.tapebutton.TapeButtonView;
import de.egh.easyloop.ui.components.tapebutton.TapeButtonView.Status;

public class MainActivity extends Activity {

	private TapeButtonView recordButtonView;
	private TapeButtonView tapeButtonView;

	public void onClickClearCountIn(final View view) {
		tapeButtonView.setCountInTime(0);
		recordButtonView.setCountInTime(0);
	}

	public void onClickRecord(final View view) {
	}

	public void onClickSetCountIn(final View view) {
		tapeButtonView.setCountInTime(2000);
		recordButtonView.setCountInTime(3000);
	}

	public void onClickStop(final View view) {
		tapeButtonView.becomeStop();
		recordButtonView.becomeStop();
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tapeButtonView = (TapeButtonView) findViewById(R.id.tapeButton);
		tapeButtonView.setEventListener(new TapeButtonView.EventListener() {

			@Override
			public void onRun() {
				// Simulate tape machine
				tapeButtonView.startCounterPlay(5000);
			}

			@Override
			public void onStop() {
				// TODO Auto-generated method stub

			}
		});

		recordButtonView = (TapeButtonView) findViewById(R.id.recordButton);
		recordButtonView.setEventListener(new TapeButtonView.EventListener() {

			@Override
			public void onRun() {
				// Simulate tape machine
				recordButtonView.startCounterRecord();
			}

			@Override
			public void onStop() {
				// TODO Auto-generated method stub

			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onToggleActive(final View view) {
		tapeButtonView.becomeActive(tapeButtonView.getStatus().equals(
				Status.INACTIVE));
		recordButtonView.becomeActive(recordButtonView.getStatus().equals(
				Status.INACTIVE));
	}
}
