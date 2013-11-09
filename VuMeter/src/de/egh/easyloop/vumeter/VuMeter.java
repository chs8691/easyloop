package de.egh.easyloop.vumeter;

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
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Shows vertical line of the last touch. */
public class VuMeter extends View implements
		ValueAnimator.AnimatorUpdateListener {
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

	private Bitmap bitmap;

	private Paint paint;

	// max input value
	private int maxValue = 0;

	private ShapeHolder needle;

	private AnimatorSet s1;

	public VuMeter(final Context context, final AttributeSet attrs) {
		super(context, attrs);

	}

	private float calculateToX(final int value) {
		return value * getWidth() / maxValue;
	}

	/**
	 * Create needle at position x. Must be called after view dimensions are
	 * available and before first onDraw
	 */
	private ShapeHolder createNeedle(final float x) {
		final RectShape needle = new RectShape();

		// Set dimension
		needle.resize(10, getHeight() - 20);

		final ShapeDrawable drawable = new ShapeDrawable(needle);

		final ShapeHolder shapeHolder = new ShapeHolder(drawable);
		// Set initial position
		shapeHolder.setX(x);
		shapeHolder.setY(10);

		final Paint paint = drawable.getPaint(); // new
													// Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(0xAFFFFFFF);
		paint.setStyle(Paint.Style.FILL);
		shapeHolder.setPaint(paint);

		return shapeHolder;
	}

	/** Calculate duration for linear velocity. For 100 % the duration is 500 ms */
	private int durationFall(final int value) {
		// return 500;
		return value * 1000 / maxValue;
	}

	private int durationRaise(final int value) {
		// return 250;
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

		canvas.save();
		canvas.translate(needle.getX(), needle.getY());
		needle.getShape().draw(canvas);
		canvas.restore();

	}

	/** Set this once before using setValue(). Must a positive integer */
	public void setMaxValue(final int maxValue) {
		if (maxValue <= 0)
			throw new InvalidParameterException(
					"maxValue must be positive, but was" + maxValue);

		this.maxValue = maxValue;
	}

	/** New value for the VU meter. setValueMax must be set before */
	public void setValue(final int value) {
		if (value < 0 || value > maxValue)
			throw new InvalidParameterException(
					"Value must be positive and LE maxValue, but was" + value);

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
