package de.egh.easyloop.ui.component;

import java.security.InvalidParameterException;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Shows vertical line of the last touch. */
public class VuMeter extends View implements
		ValueAnimator.AnimatorUpdateListener {

	private class Color {
		private class Background {
			private static final int UNMUTED = 0xff000000;
			private static final int MUTED = 0xff7D7D7D;
		}

		private class Needle {
			private static final int VALUE = 0xDB;
			private static final int BASE = 0xFF000000 | VALUE << 16
					| VALUE << 8 | VALUE;
		}
	}

	/**
	 * A data structure that holds a Shape and various properties that can be
	 * used to define how the shape is drawn.
	 */
	public class ShapeHolder {
		private float x = 0, y = 0;
		private ShapeDrawable shape;
		private int color;
		private RadialGradient gradient;
		private float alpha = 1f;
		private Paint paint;

		public ShapeHolder(final ShapeDrawable s) {
			shape = s;
		}

		public int getColor() {
			return color;
		}

		public RadialGradient getGradient() {
			return gradient;
		}

		public float getHeight() {
			return shape.getShape().getHeight();
		}

		public Paint getPaint() {
			return paint;
		}

		public ShapeDrawable getShape() {
			return shape;
		}

		public float getWidth() {
			return shape.getShape().getWidth();
		}

		public float getX() {
			return x;
		}

		public float getY() {
			return y;
		}

		public void setAlpha(final float alpha) {
			this.alpha = alpha;
			shape.setAlpha((int) ((alpha * 255f) + .5f));
		}

		public void setColor(final int value) {
			shape.getPaint().setColor(value);
			color = value;
		}

		public void setGradient(final RadialGradient value) {
			gradient = value;
		}

		public void setHeight(final float height) {
			final Shape s = shape.getShape();
			s.resize(s.getWidth(), height);
		}

		public void setPaint(final Paint value) {
			paint = value;
		}

		public void setShape(final ShapeDrawable value) {
			shape = value;
		}

		public void setWidth(final float width) {
			final Shape s = shape.getShape();
			s.resize(width, s.getHeight());
		}

		public void setX(final float value) {
			x = value;
		}

		public void setY(final float value) {
			y = value;
		}
	}

	private static final int NEEDLE_WIDTH = 10;

	private final static String TAG = "VuMeter";

	private Bitmap bitmap;

	private Paint paint;

	// max input value
	private int maxValue = 0;

	private ShapeHolder needle;

	private AnimatorSet s1;

	private boolean mute;

	private int threshold;

	public VuMeter(final Context context, final AttributeSet attrs) {
		super(context, attrs);

	}

	private float calculateToX(final int value) {
		return value * (getWidth() - NEEDLE_WIDTH) / maxValue;
	}

	/**
	 * Create needle at position x. Must be called after view dimensions are
	 * available and before first onDraw
	 */
	private ShapeHolder createNeedle(final float x) {
		final RectShape needle = new RectShape();

		// Set dimension
		needle.resize(NEEDLE_WIDTH, getHeight() - 4);

		final ShapeDrawable drawable = new ShapeDrawable(needle);

		final ShapeHolder shapeHolder = new ShapeHolder(drawable);
		// Set initial position
		shapeHolder.setX(x);
		shapeHolder.setY(2);

		final Paint paint = drawable.getPaint(); // new
													// Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.Needle.BASE);
		paint.setStyle(Paint.Style.FILL);
		shapeHolder.setPaint(paint);

		return shapeHolder;
	}

	/** Calculate duration for linear velocity. For 100 % the duration is 500 ms */
	private int durationFall(final int value) {

		return value * 2000 / maxValue;
	}

	private int durationRaise(final int value) {

		return value * 250 / maxValue;
	}

	@Override
	public void onAnimationUpdate(final ValueAnimator animation) {
		invalidate();
	}

	@Override
	protected void onDraw(final Canvas canvas) {

		// There must be a better place to create this object, but I still
		// havn't found what I looking for...
		if (needle == null)
			needle = createNeedle(0);

		// mute mode: more alpha
		if (mute)
			needle.setAlpha(0.3f);
		else
			needle.setAlpha(1f);

		// Overdrive: red needle for the loudest 10 %
		// int darkColor = 0xff000000 | red/4 << 16 | green/4 << 8 | blue/4;
		final int color = Color.Needle.BASE;
		final int offset;

		if (needle.getX() > threshold) {
			final float width = (getWidth() - NEEDLE_WIDTH - threshold);
			final float x = needle.getX() - threshold;
			offset = (int) (Color.Needle.VALUE * (1f - (x / width)));
			needle.setColor(0xFF000000 | Color.Needle.VALUE << 16 | offset << 8
					| offset);
		} else
			needle.setColor(color);

		canvas.save();
		canvas.translate(needle.getX(), needle.getY());
		needle.getShape().draw(canvas);
		canvas.restore();

	}

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw,
			final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		// Set Threshold for red colored needle range
		threshold = (w - NEEDLE_WIDTH) * 8 / 10;

	}

	/** Set this once before using setValue(). Must a positive integer */
	public void setMaxValue(final int maxValue) {
		Log.v(TAG, "SetMaxValue() " + maxValue);

		if (maxValue <= 0)
			throw new InvalidParameterException(
					"maxValue must be positive, but was " + maxValue);

		this.maxValue = maxValue;

	}

	/**
	 * In mute mode the audio level will be still updated, but the colors will
	 * be muted.
	 * 
	 * @param mute
	 *            boolean with TRUE, FALSE normal mode.
	 */
	public void setMute(final boolean mute) {
		this.mute = mute;
		if (mute)
			this.setBackgroundColor(Color.Background.MUTED);
		else
			this.setBackgroundColor(Color.Background.UNMUTED);

	}

	/** New value for the VU meter. setValueMax must be set before */
	public void setValue(final int value) {

		if (value < 0 || value > maxValue)
			throw new InvalidParameterException(
					"Value must be positive and <= " + maxValue + ", but was "
							+ value);

		final float fromX = needle.getX();
		final float toX = calculateToX(value);

		// Do nothing, if new value is lower than the actual position
		if (toX < fromX)
			return;

		// Stop running animation
		if (s1 != null) {
			s1.cancel();
		}

		// create new animation, starting from actual x position
		final ObjectAnimator raise = ObjectAnimator.ofFloat(needle, "x", fromX,
				toX).setDuration(durationRaise(value));
		raise.setInterpolator(new LinearInterpolator());

		final ObjectAnimator fall = ObjectAnimator
				.ofFloat(needle, "x", toX, 0f).setDuration(durationFall(value));
		raise.setInterpolator(new LinearInterpolator());
		s1 = new AnimatorSet();
		s1.playSequentially(raise, fall);
		raise.addUpdateListener(this);
		fall.addUpdateListener(this);

		s1.start();

	}

}
