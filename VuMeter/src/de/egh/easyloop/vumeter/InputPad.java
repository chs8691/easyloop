package de.egh.easyloop.vumeter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Shows vertical line of the last touch */
public class InputPad extends View {

	public interface Listener {
		public void onTouchEvent(int xPos);
	}

	private Bitmap bitmap;
	private Paint paint;

	// Last touch
	private int xPos = 0;
	private Listener listener;

	public InputPad(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public int getMax() {
		final int i = getWidth();
		return i;
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		// Initialize once the objects for perfomance reasons
		if (bitmap == null) {
			bitmap = Bitmap.createBitmap(getWidth(), getHeight(),
					Bitmap.Config.ARGB_8888);
			paint = new Paint();
			paint.setColor(0xAFFFFFFF);
			paint.setStyle(Style.FILL);
			paint.setAntiAlias(true);
			paint.setStyle(Style.FILL_AND_STROKE);
			paint.setStrokeWidth(10);

		}

		// Vertical line og last touch event
		if (xPos != 0) {
			canvas.drawRect(xPos - 5, 0, xPos + 5, getHeight(), paint);
		}
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			xPos = (int) event.getX();
			// Fire event to listener
			if (listener != null)
				listener.onTouchEvent(xPos);

			invalidate();
			break;
		}

		return true;

	}

	public void setListener(final Listener listener) {
		this.listener = listener;
	}
}
