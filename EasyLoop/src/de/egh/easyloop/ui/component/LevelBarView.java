package de.egh.easyloop.ui.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/** Use VuMeter instead */
@Deprecated
class LevelBarView extends View {

	private Bitmap bitmap;
	private Canvas canvas;
	private Paint paint;
	private short actLevel = 0;
	private final static String TAG = "LevelBarView";

	public LevelBarView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		Log.v(TAG, "LevelBarView()");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		// Initialize once the objects for perfomance reasons
		if (bitmap == null) {
			bitmap = Bitmap.createBitmap(getWidth(), getHeight(),
					Bitmap.Config.ARGB_8888);
			paint = new Paint();
			this.canvas = canvas;
		}

		// level bar
		paint.reset();
		paint.setColor(0xAFFFFFFF);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);

		canvas.drawRect(1, 1, getWidth() * actLevel / 50000, getHeight() - 1,
				paint);
	}

	public void setLevel(final short level) {
		this.actLevel = level;
		invalidate();
	}
}
