package de.egh.easyloop.ui.components.tapebutton;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.ArcShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import de.egh.easyloop.ui.components.tapebutton.TapeButtonView.Status;

public class CircleCounterView extends View implements AnimatorUpdateListener {

	/** Listener for events of CircleCounterView. */
	public interface EventListener {

		/** Fired, when count in has finished. */
		public void onCountInFinish();
	}

	/**
	 * A data structure that holds a Shape and various properties that can be
	 * used to define how the shape is drawn.
	 */
	public class ShapeHolder {
		private float alpha = 1f;
		private float angle = 0;
		private int color;
		private RadialGradient gradient;
		private Paint paint;
		private ShapeDrawable shape;
		private float x = 0, y = 0;

		public ShapeHolder(final ShapeDrawable s) {
			shape = s;
		}

		public float getAngle() {
			return angle;
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

		public void setAngle(final float angle) {
			this.angle = angle;

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

	/** For playing, recording and count in there are three different styles. */
	public enum Type {
		/** Count backwards until 0°, counterclockwise */
		COUNT_BACKWARDS,

		/** Run 360°, clockwise */
		ONE_ROUND,

		/** Counter as seconds, clockwise */
		SECONDS;

	}

	private static final int ALPHA_ACTIVE = 0xFF;;

	private static final int ALPHA_INACTIVE = 0x44;

	private static final int NEEDLE = 0xFFFFFF;
	private static final int PANE = 0x1111111;
	private static final int RING = PANE;
	/** Count in time in ms. Enables count in, if duration > 0. */
	private int countInTime;
	private EventListener eventListener;

	private ShapeHolder needle;
	private ObjectAnimator oa;
	private final Paint panePaint;

	private final RectF paneRect;
	private int rectWidth;
	private final Paint ringPaint;
	private final RectF ringRect;
	private Status status;

	private Type type;

	public CircleCounterView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		ringPaint = new Paint();
		ringRect = new RectF();
		panePaint = new Paint();
		paneRect = new RectF();

		// As default, the status is inactive
		status = Status.INACTIVE;

	}

	private void animationBecomeStop() {
		if (oa != null && oa.isStarted())
			oa.end();

	}

	/**
	 * Create needle. Must be called after view dimensions are available and
	 * before first onDraw
	 */
	private ShapeHolder createNeedle() {

		// Set dimension

		final ArcShape needle = new ArcShape(265, 10);
		needle.resize(rectWidth, rectWidth);

		final ShapeDrawable drawable = new ShapeDrawable(needle);

		final ShapeHolder shapeHolder = new ShapeHolder(drawable);
		// Set center
		shapeHolder.setX(rectWidth / 2);
		shapeHolder.setY(rectWidth / 2);

		final Paint paint = drawable.getPaint(); // new
													// Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(getColorNeedle());
		paint.setStyle(Paint.Style.FILL);
		shapeHolder.setPaint(paint);

		return shapeHolder;
	}

	private int getColorNeedle() {
		int needle;

		// More transparent if inactive
		if (status.equals(Status.INACTIVE)) {
			needle = ALPHA_INACTIVE << 24;
		} else {
			needle = ALPHA_ACTIVE << 24;
		}

		needle = needle | NEEDLE;

		return needle;
	}

	private int getColorPane() {
		int pane;

		// More transparent if inactive
		if (status.equals(Status.INACTIVE)) {
			pane = ALPHA_INACTIVE << 24;
		} else {
			pane = ALPHA_ACTIVE << 24;
		}

		pane = pane | PANE;

		return pane;
	}

	private int getColorRing() {
		int ring;

		// More transparent if inactive
		if (status.equals(Status.INACTIVE)) {
			ring = ALPHA_INACTIVE << 24;
		} else {
			ring = ALPHA_ACTIVE << 24;
		}

		ring = ring | RING;

		return ring;
	}

	@Override
	public void onAnimationUpdate(final ValueAnimator animation) {
		invalidate();
	}

	@Override
	protected void onDraw(final Canvas canvas) {

		canvas.drawOval(ringRect, ringPaint);

		canvas.save();
		canvas.rotate(needle.getAngle(), rectWidth / 2, rectWidth / 2);
		needle.getShape().draw(canvas);
		canvas.restore();

		canvas.drawOval(paneRect, panePaint);

	}

	@Override
	protected void onLayout(final boolean changed, final int left,
			final int top, final int right, final int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		rectWidth = Math.min(getWidth(), getHeight());

		ringPaint.reset();
		ringPaint.setAntiAlias(true);
		ringPaint.setStyle(Style.FILL);
		ringPaint.setStrokeWidth(0);
		ringPaint.setColor(getColorRing());
		ringRect.set(0, 0, rectWidth, rectWidth);

		final float radius = rectWidth / 4;
		panePaint.reset();
		panePaint.setAntiAlias(true);
		panePaint.setStyle(Style.FILL_AND_STROKE);
		panePaint.setStrokeWidth(0);
		panePaint.setColor(getColorPane());
		paneRect.set(radiusToPoint(radius), radiusToPoint(radius), rectWidth
				- radiusToPoint(radius), rectWidth - radiusToPoint(radius));

		// We are before first onDraw, so onDraw can use the needle.
		if (needle == null)
			needle = createNeedle();
	}

	private float radiusToPoint(final float radius) {
		return (float) (rectWidth / 2.0 - radius);
	}

	public void setCountInTime(final int countInTime) {
		this.countInTime = countInTime;
	}

	/** eventListener can be null. */
	public void setEventListener(final EventListener eventListener) {
		if (eventListener == null)
			this.eventListener = new EventListener() {

				@Override
				public void onCountInFinish() {
					// Do nothing
				}
			};

		else
			this.eventListener = eventListener;
	}

	/**
	 * Update its status. If called before onLayout() is called, only the status
	 * will be stored. Otherwise, if called after first displaying, redrawing
	 * will be started.
	 */
	public void setStatus(final Status status) {

		if (status != this.status)
			animationBecomeStop();

		this.status = status;

		if (needle != null) {
			panePaint.setColor(getColorPane());
			needle.setColor(getColorNeedle());
			ringPaint.setColor(getColorRing());
			invalidate();
		}

		if (status.equals(Status.COUNT_IN))
			startAnimation(countInTime, Type.COUNT_BACKWARDS);

	}

	/**
	 * Resets and starts the counter. Button must be active otherwise nothing
	 * happens. A valid duration in milliseconds for type == COUNT_BACKWARDS is
	 * [0..4000], for type == ONE_ROUND >=0 and for type == SECONDS, duration
	 * will be ignored.
	 */
	public void startAnimation(final int duration, final Type type) {

		// Precondition: Must be active
		if (status.equals(Status.INACTIVE))
			throw new RuntimeException("Counter is inactive.");

		// Playing
		if (type.equals(Type.ONE_ROUND)) {
			oa = ObjectAnimator.ofFloat(needle, "angle", 0, 360);
			oa.setDuration(duration);
		}
		// Count in
		else if (type.equals(Type.COUNT_BACKWARDS)) {
			// final float angle = duration * 90 / 1000;

			oa = ObjectAnimator.ofFloat(needle, "angle", 360, 0);
			oa.setDuration(duration);
		}
		// Recording
		else {
			oa = ObjectAnimator.ofFloat(needle, "angle", 0, 360);
			oa.setDuration(1000);
			oa.setRepeatCount(ValueAnimator.INFINITE);
			oa.setRepeatMode(ValueAnimator.INFINITE);
		}
		oa.setInterpolator(new LinearInterpolator());
		oa.addUpdateListener(this);

		// For count in, we need the end signal to announce it
		oa.addListener(new AnimatorListener() {

			@Override
			public void onAnimationCancel(final Animator animation) {
			}

			@Override
			public void onAnimationEnd(final Animator animation) {
				if (type.equals(Type.COUNT_BACKWARDS))
					eventListener.onCountInFinish();
			}

			@Override
			public void onAnimationRepeat(final Animator animation) {
			}

			@Override
			public void onAnimationStart(final Animator animation) {
			}
		});
		oa.start();
	}

}
