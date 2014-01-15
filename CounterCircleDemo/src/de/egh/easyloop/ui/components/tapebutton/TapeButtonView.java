package de.egh.easyloop.ui.components.tapebutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.example.countercircledemo.R;

import de.egh.easyloop.ui.components.tapebutton.CircleCounterView.Type;

/**
 * Button for a tape machine. Can be a record or a play button. Has an icon an a
 * animation for running tme. State:
 * http://www.charlesharley.com/2012/programming/custom-drawable-states-in
 * -android/
 */
public class TapeButtonView extends RelativeLayout {

	/** The tape machine has two types of buttons. */
	enum ButtonType {
		/** Play button */
		PLAY,
		/** Record button */
		RECORD
	}

	/**
	 * Listener for all button events. Use this instead of View.OnClickListener.
	 */
	public interface EventListener {

		/** Button was pressed to run. */
		public void onRun();

		/** Button was pressed to stop. */
		public void onStop();
	}

	public enum Status {
		/**
		 * For Recording only: Latency time before starting the recording.
		 * Stopped is a checked status.
		 */
		COUNT_IN,
		/** Disabled button */
		INACTIVE,
		/** Pressed button */
		RUNNING,
		/** Enabled but not running. Running is a checked status. */
		STOPPED;

		public Status copy() {
			if (this.equals(Status.INACTIVE))
				return Status.INACTIVE;
			else if (this.equals(Status.RUNNING))
				return Status.RUNNING;
			else if (this.equals(Status.STOPPED))
				return Status.STOPPED;
			else if (this.equals(Status.COUNT_IN))
				return Status.COUNT_IN;
			else
				return null;
		}
	}

	private static final int[] STATE_CHECKED = { R.attr.tapeButtonStateChecked };

	private final ButtonIconView buttonIconView;;

	private final CircleCounterView circleCounterView;

	/** TRUE, if count in is enabled */
	private boolean countIn;

	private EventListener eventListener;

	private Status status = Status.STOPPED;

	private final ButtonType type;

	public TapeButtonView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		inflate(context, R.layout.custom_tape_button, this);

		countIn = false;

		final TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.tapeButton);

		if (a.getString(R.styleable.tapeButton_tapeButtonType).equals("play"))
			type = ButtonType.PLAY;
		else
			type = ButtonType.RECORD;

		// we don't need a anymore
		a.recycle();

		circleCounterView = (CircleCounterView) findViewById(R.id.customTapebuttonCircleCounter);
		circleCounterView
				.setEventListener(new CircleCounterView.EventListener() {

					@Override
					public void onCountInFinish() {

						// Precondition
						if (!status.equals(Status.COUNT_IN))
							return;

						// Switch to running
						status = Status.RUNNING;
						buttonIconView.setStatus(status);
						circleCounterView.setStatus(status);

						eventListener.onRun();
					}
				});

		buttonIconView = (ButtonIconView) findViewById(R.id.customTapebuttonButtonIcon);
		buttonIconView.setType(type);

		// Initialize button to a defined status
		becomeActive(false);

		// Create Null-object
		setEventListener(null);

		this.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {
				onButtonClicked();
			}
		});

	}

	/**
	 * Request to active or deactivate the button. If switched to inactive, a
	 * possible running status will be canceled.
	 */
	public void becomeActive(final boolean active) {

		// Nothing to do
		if (!active && status.equals(Status.INACTIVE)
				|| (active && !status.equals(Status.INACTIVE)))
			return;

		// Active button
		if (active) {
			this.setEnabled(true);
			status = Status.STOPPED;
		}

		// Inactive button
		else {
			this.setEnabled(false);
			status = Status.INACTIVE;
		}

		buttonIconView.setStatus(status);
		circleCounterView.setStatus(status);
		refreshDrawableState();

	}

	/** Stop the active button. Button must be active. */
	public void becomeStop() {

		if (status.equals(Status.STOPPED) || status.equals(Status.INACTIVE))
			return;

		onButtonClicked();

	}

	/** Returns a cloned status. */
	public Status getStatus() {
		return status.copy();
	}

	/** Change status and update view children. Status must not be inactive. */
	private void onButtonClicked() {

		if (status.equals(Status.STOPPED)) {
			if (countIn) {
				status = Status.COUNT_IN;
				buttonIconView.setStatus(status);
				circleCounterView.setStatus(status);
				// onRun will be communicated, when count in has finished.
			} else {
				status = Status.RUNNING;
				buttonIconView.setStatus(status);
				circleCounterView.setStatus(status);
				eventListener.onRun();
			}
		} else {
			status = Status.STOPPED;
			buttonIconView.setStatus(status);
			circleCounterView.setStatus(status);
			eventListener.onStop();
		}

		refreshDrawableState();
	}

	@Override
	protected int[] onCreateDrawableState(final int extraSpace) {
		// If the actual status is a 'checked' one then we merge our custom
		// message unread
		// state into
		// the existing drawable state before returning it.
		// We are going to add 1 extra state.
		if (status != null
				&& (status.equals(Status.RUNNING) || status
						.equals(Status.COUNT_IN))) {
			final int[] drawableState = super
					.onCreateDrawableState(extraSpace + 1);

			mergeDrawableStates(drawableState, STATE_CHECKED);

			return drawableState;
		} else {
			return super.onCreateDrawableState(extraSpace);
		}

	}

	/**
	 * Enables count in time, starting by pressing the button. OnClick() will be
	 * fired, after count in. To switch off, set countInTime to 0.
	 */
	public void setCountInTime(final int countInTime) {
		if (countInTime < 0)
			throw new RuntimeException("CountInTime must be >= 0, but is "
					+ countInTime);

		countIn = countInTime > 0;
		circleCounterView.setCountInTime(countInTime);
	}

	public void setEventListener(final EventListener eventListener) {
		// Null-Object
		if (eventListener == null)
			this.eventListener = new EventListener() {

				@Override
				public void onRun() {
					// Nothing to do.
				}

				@Override
				public void onStop() {
					// Nothing to do.
				}
			};
		else
			this.eventListener = eventListener;
	}

	private void startAnimation() {

	}

	/**
	 * Resets and starts the counter for button 'Play'. A valid duration in
	 * milliseconds must >= 0.
	 * 
	 * @throws RuntimeException
	 *             if status != RUNNING or type != PLAY or duration < 0
	 */
	public void startCounterPlay(final int duration) {
		if (!status.equals(Status.RUNNING))
			throw new RuntimeException("Button is not in status running: "
					+ status.name());
		if (!type.equals(ButtonType.PLAY))
			throw new RuntimeException("Type is not Play: " + status.name());
		if (duration < 0)
			throw new RuntimeException("Duration not positive: " + duration);

		circleCounterView.startAnimation(duration, Type.ONE_ROUND);
	}

	/**
	 * Resets and starts the counter for button 'Record'.
	 * 
	 * @throws RuntimeException
	 *             if status != RUNNING or type != PLAY or duration < 0
	 */
	public void startCounterRecord() {
		if (!status.equals(Status.RUNNING))
			throw new RuntimeException("Button is not in status running: "
					+ status.name());
		if (!type.equals(ButtonType.RECORD))
			throw new RuntimeException("Type is not Record: " + status.name());

		circleCounterView.startAnimation(0, Type.SECONDS);
	}
}
