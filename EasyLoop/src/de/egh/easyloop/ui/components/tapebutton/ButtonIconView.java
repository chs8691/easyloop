package de.egh.easyloop.ui.components.tapebutton;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import de.egh.easyloop.ui.components.tapebutton.TapeButtonView.ButtonType;
import de.egh.easyloop.ui.components.tapebutton.TapeButtonView.Status;

/**
 * Icon for two types of button: Record and Play. Set the type in the layout
 * with attribute tapeButtonType.
 */
public class ButtonIconView extends View {

	private static final int ALPHA_ACTIVE = 0xFF;
	private static final int ALPHA_INACTIVE = 0x44;
	private static final int COUNT_IN_COLOR = 0xFFB938;
	private static final int PLAY_COLOR = 0x1CAD09;
	private static final int PLAY_COLOR_STOP = 0x838F82;
	private static final int RECORD_COLOR = 0xAD0C0C;
	private static final int RECORD_COLOR_STOP = 0x8A7C7C;

	private boolean enabled;
	private final Paint paint;

	private final Path playPath;
	private int rectWidth;
	private Status status;

	private TapeButtonView.ButtonType type;

	public ButtonIconView(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();
		playPath = new Path();

		// If SetType is called not before first onDraw to avoid exception.
		type = TapeButtonView.ButtonType.PLAY;

		// As default, the status is inactive
		status = Status.STOPPED;
		enabled = true;

	}

	private int getColor() {
		int color;

		// More transparent if inactive
		if (!enabled)
			color = ALPHA_INACTIVE << 24;
		else
			color = ALPHA_ACTIVE << 24;

		if (status.equals(Status.COUNT_IN))
			color = color | COUNT_IN_COLOR;
		else {

			// Base color for play button
			if (type.equals(ButtonType.PLAY))
				if (status.equals(Status.STOPPED))
					color = color | PLAY_COLOR_STOP;
				else
					color = color | PLAY_COLOR;

			// Record button has to states
			else if (status.equals(Status.STOPPED))
				color = color | RECORD_COLOR_STOP;
			else
				color = color | RECORD_COLOR;
		}
		return color;
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		if (type.equals(TapeButtonView.ButtonType.PLAY)) {
			canvas.drawPath(playPath, paint);
		} else {
			canvas.drawCircle(rectWidth / 2, rectWidth / 2, rectWidth / 2,
					paint);
		}

	}

	@Override
	protected void onLayout(final boolean changed, final int left,
			final int top, final int right, final int bottom) {
		rectWidth = Math.min(getWidth(), getHeight());

		paint.reset();
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(getColor());

		// Only for Play Buttons
		playPath.reset();
		playPath.moveTo(0, 0);
		playPath.lineTo(0, rectWidth);
		playPath.lineTo(rectWidth, rectWidth / 2);
		playPath.close();

	}

	private void refresh() {
		if (paint != null) {
			paint.setColor(getColor());
			invalidate();
		}

	}

	@Override
	public void setEnabled(final boolean enable) {
		this.enabled = enable;
		refresh();
	}

	/**
	 * Update appearance. If called before onLayout() is called, only the status
	 * will be stored. Otherwise, if called after first displaying, redrawing
	 * will be started.
	 */
	public void setStatus(final Status status) {
		this.status = status;
		refresh();
	}

	/**
	 * Determines the button type. If called before onLayout() is called, only
	 * the type will be stored. Otherwise, if called after first displaying,
	 * redrawing will be started.
	 */
	public void setType(final ButtonType buttonType) {
		type = buttonType;

		if (paint != null)
			invalidate();
	}

}
