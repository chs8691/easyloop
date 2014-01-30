package de.egh.easyloop.ui.components.tapebutton;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
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
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import de.egh.easyloop.ui.components.tapebutton.TapeButtonView.Status;

public class CircleCounterView extends View implements AnimatorUpdateListener {

	/** Storage for animation properties */
	private class AnimationSetting {
		int actualTime;
		int totalDuration;
		Type type;

		public AnimationSetting(final int totalDuration, final Type type,
				final int actualTime) {
			this.totalDuration = totalDuration;
			this.type = type;
			this.actualTime = actualTime;

		}
	}

	/** Listener for events of CircleCounterView. */
	public interface EventListener {

		/** Fired, when count in has finished. */
		public void onCountInFinish();
	}

	private enum ListenerType {
		NEXT, ONCE, REPEAT
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
	};

	/** Temporary transfer object. */
	private class SplitResult {
		public int duration;
		public int pause;
		public int remainingTime;
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

	// private static final int PANE2 = 0x00 << 24 |
	// R.color.secondary_dark_gray;
	private static final int ALPHA_ACTIVE = 0xFF;
	private static final int ALPHA_INACTIVE = 0x44;
	private static final int NEEDLE = 0xFFFFFF;
	private static final int PANE = 0x00093647;

	private static final int RECORD_DURATION = 4000;
	// private static final int RECORD_QUATER_DURATION = 1000;

	private static final int RING = PANE;

	private static final String TAG = "CircleCounterView";

	private AnimatorSet actualAnimatorSet;
	// private int animationTotalDuration;
	// private Type animationType;

	/** If set, animation will be started when inflating has finished. */
	private AnimationSetting animationSettingMemory;

	/** Count in time in ms. Enables count in, if duration > 0. */
	private int countInTime;

	private boolean enabled;

	private EventListener eventListener;

	private ShapeHolder needle;

	private AnimatorSet nextAnimatorSet;

	private final Paint panePaint;

	private final RectF paneRect;

	private int rectWidth;

	private final Paint ringPaint;

	private final RectF ringRect;

	private Status status;

	// private void buildAnimation(final int totalDuration, final Type type,
	// final int actualTime) {
	//
	// // Needle should pause at 12 o'clock or every second
	// SplitResult split;
	//
	// final AnimatorSet newAnimatorSet = new AnimatorSet();
	// AnimatorListener newAnimatorListener = null;
	//
	// // Playing
	// if (animationType.equals(Type.ONE_ROUND)) {
	// split = splitDuration(animationTotalDuration);
	//
	// final ObjectAnimator oaPause = ObjectAnimator.ofFloat(needle,
	// "angle", 0, 0);
	// oaPause.setDuration(split.pause);
	//
	// ObjectAnimator oaAction1;
	// oaAction1 = ObjectAnimator
	// .ofFloat(
	// needle,
	// "angle",
	// calcualateAngleActual(split.duration, actualTime,
	// 360), 360);
	// oaAction1.setDuration(split.duration);
	// oaAction1.addUpdateListener(this);
	// newAnimatorSet.play(oaAction1).before(oaPause);
	// newAnimatorListener = createAnimationListener(ListenerType.NEXT);
	// }
	//
	// // Count in
	// else if (animationType.equals(Type.COUNT_BACKWARDS)) {
	// split = splitDuration(animationTotalDuration);
	//
	// final ObjectAnimator oaPause = ObjectAnimator.ofFloat(needle,
	// "angle", 0, 0);
	// oaPause.setDuration(split.pause);
	//
	// ObjectAnimator oaAction;
	// oaAction = ObjectAnimator.ofFloat(needle, "angle", 360, 0);
	// oaAction.setDuration(split.duration);
	// oaAction.addUpdateListener(this);
	// newAnimatorSet.play(oaAction).before(oaPause);
	//
	// }
	//
	// // Recording
	// else {
	//
	// split = splitDuration(RECORD_QUATER_DURATION);
	//
	// // Actual position in actual round as milliseconds
	// final int recTime = actualTime % (RECORD_QUATER_DURATION * 4);
	// int startQuadrant;
	// ObjectAnimator oaPause1 = null;
	// ObjectAnimator oaAction1 = null;
	// ObjectAnimator oaPause2 = null;
	// ObjectAnimator oaAction2 = null;
	// ObjectAnimator oaPause3 = null;
	// ObjectAnimator oaAction3 = null;
	// ObjectAnimator oaPause4 = null;
	// ObjectAnimator oaAction4 = null;
	//
	// if (recTime < RECORD_QUATER_DURATION)
	// startQuadrant = 1;
	// else if (recTime < RECORD_QUATER_DURATION * 2)
	// startQuadrant = 2;
	// else if (recTime < RECORD_QUATER_DURATION * 3)
	// startQuadrant = 3;
	// else
	// startQuadrant = 4;
	//
	// // --- 1 ---//
	// if (startQuadrant == 1) {
	// oaPause1 = ObjectAnimator.ofFloat(needle, "angle", 90, 90);
	// oaPause1.setDuration(split.pause);
	// oaAction1 = ObjectAnimator.ofFloat(needle, "angle",
	// calcualateAngleActual(split.duration, recTime, 90), 90);
	// oaAction1.setDuration(split.duration);
	//
	// oaAction1.addUpdateListener(this);
	// }
	//
	// // --- 2 ---//
	// if (startQuadrant <= 2) {
	// oaPause2 = ObjectAnimator.ofFloat(needle, "angle", 180, 180);
	// oaPause2.setDuration(split.pause);
	// oaAction2 = ObjectAnimator.ofFloat(needle, "angle", 90, 180);
	// oaAction2.setDuration(split.duration);
	// oaAction2.addUpdateListener(this);
	// }
	//
	// // --- 3 ---//
	// if (startQuadrant <= 3) {
	// oaPause3 = ObjectAnimator.ofFloat(needle, "angle", 270, 270);
	// oaPause3.setDuration(split.pause);
	// oaAction3 = ObjectAnimator.ofFloat(needle, "angle", 180, 270);
	// oaAction3.setDuration(split.duration);
	// oaAction3.addUpdateListener(this);
	// }
	//
	// // --- 4 ---//
	// if (startQuadrant <= 4) {
	// oaPause4 = ObjectAnimator.ofFloat(needle, "angle", 360, 360);
	// oaPause4.setDuration(split.pause);
	// oaAction4 = ObjectAnimator.ofFloat(needle, "angle", 270, 360);
	// oaAction4.setDuration(split.duration);
	// oaAction4.addUpdateListener(this);
	// }
	//
	// switch (startQuadrant) {
	// default:
	// newAnimatorSet.playSequentially(oaAction1, oaPause1, oaAction2,
	// oaPause2, oaAction3, oaPause3, oaAction4, oaPause4);
	// break;
	// case 2:
	// newAnimatorSet.playSequentially(oaAction2, oaPause2, oaAction3,
	// oaPause3, oaAction4, oaPause4);
	// break;
	// case 3:
	// newAnimatorSet.playSequentially(oaAction3, oaPause3, oaAction4,
	// oaPause4);
	// break;
	// case 4:
	// newAnimatorSet.playSequentially(oaAction4, oaPause4);
	// break;
	// }
	// newAnimatorListener = createAnimationListener(ListenerType.REPEAT);
	//
	// }
	//
	// newAnimatorSet.setInterpolator(new LinearInterpolator());
	//
	// // Previous animation is still running
	// if (actualAnimatorSet != null && actualAnimatorSet.isStarted()) {
	// actualAnimatorSet.removeAllListeners();
	// actualAnimatorSet
	// .addListener(createAnimationListener(ListenerType.NEXT));
	// nextAnimatorSet = newAnimatorSet.clone();
	// nextAnimatorSet.addListener(newAnimatorListener);
	// actualAnimatorSet.cancel();
	// }
	// // No animation running
	// else {
	// actualAnimatorSet = newAnimatorSet.clone();
	// actualAnimatorSet.addListener(newAnimatorListener);
	// actualAnimatorSet.start();
	//
	// }
	//
	// }

	public CircleCounterView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		ringPaint = new Paint();
		ringRect = new RectF();
		panePaint = new Paint();
		paneRect = new RectF();

		// As default, the status is inactive
		status = Status.STOPPED;
		enabled = true;

		// Initialize memory
		animationSettingMemory = null;

	}

	private void animationBecomeStop() {
		Log.v(TAG, "animationBecomeStop()");

		if (actualAnimatorSet != null) {
			actualAnimatorSet.removeAllListeners();
			actualAnimatorSet.end();
		}

	}

	/**
	 * 
	 * @param totalDuration
	 *            int with milliseconds , must be set
	 * @param type
	 * @param actualTime
	 *            int with setup point in milliseconds
	 */
	private void buildAnimation(final int totalDuration, final Type type,
			final int actualTime) {

		// Needle should pause at 12 o'clock or every second
		final SplitResult split = splitDuration(totalDuration, actualTime);
		nextAnimatorSet = new AnimatorSet();

		// Playing
		if (type.equals(Type.ONE_ROUND)) {

			final ObjectAnimator oaPause = ObjectAnimator.ofFloat(needle,
					"angle", 0, 0);
			oaPause.setDuration(split.pause);

			ObjectAnimator oaAction1;
			oaAction1 = ObjectAnimator
					.ofFloat(
							needle,
							"angle",
							calcualateAngleActual(split.duration, actualTime,
									360), 360);
			oaAction1.setDuration(split.remainingTime);
			oaAction1.addUpdateListener(this);
			nextAnimatorSet.play(oaAction1).before(oaPause);
		}

		// Count in
		else if (type.equals(Type.COUNT_BACKWARDS)) {

			final ObjectAnimator oaPause = ObjectAnimator.ofFloat(needle,
					"angle", 0, 0);
			oaPause.setDuration(split.pause);

			ObjectAnimator oaAction;
			oaAction = ObjectAnimator.ofFloat(needle, "angle", 360, 0);
			oaAction.setDuration(split.duration);
			oaAction.addUpdateListener(this);
			nextAnimatorSet.play(oaAction).before(oaPause);

		}

		// Recording: Needs two animations: first the rest of the actual round
		// and second a full round
		else {

			// Actual position in actual round as milliseconds
			final int recTime = actualTime % (totalDuration);
			ObjectAnimator oaAction1 = null;
			ObjectAnimator oaAction2 = null;

			oaAction1 = ObjectAnimator.ofFloat(needle, "angle",
					calcualateAngleActual(totalDuration, recTime, 360), 360);
			oaAction1.setDuration(split.remainingTime);
			oaAction1.addUpdateListener(this);

			oaAction2 = ObjectAnimator.ofFloat(needle, "angle", 0, 360);
			oaAction2.setDuration(totalDuration);
			oaAction2.setRepeatMode(ValueAnimator.INFINITE);
			oaAction2.setRepeatCount(ValueAnimator.INFINITE);
			oaAction2.addUpdateListener(this);

			nextAnimatorSet.play(oaAction2).after(oaAction1);

		}

		nextAnimatorSet.setInterpolator(new LinearInterpolator());

		// Previous animation is still running
		if (actualAnimatorSet != null && actualAnimatorSet.isStarted()) {
			actualAnimatorSet.removeAllListeners();
			actualAnimatorSet
					.addListener(createAnimationListener(ListenerType.NEXT));
			actualAnimatorSet.cancel();
		}
		// No animation running
		else {
			if (nextAnimatorSet == null)
				Log.v(TAG, "nextAnimator is null !!");
			// else
			// for (final Animator a : nextAnimatorSet.getChildAnimations())
			// if (a == null)
			// Log.v(TAG, "nextAnimatorSet: a is null");
			// else
			// Log.v(TAG, "nextAnimatorSet: " + a.toString());

			actualAnimatorSet = nextAnimatorSet.clone();

			if (actualAnimatorSet == null)
				Log.v(TAG, "actualAnimator is null !!");
			// else
			// for (final Animator a : actualAnimatorSet.getChildAnimations())
			// if (a == null)
			// Log.v(TAG, "actualAnimatorSet: a is null");
			// else
			// Log.v(TAG, "actualAnimatorSet: " + a.toString());

			actualAnimatorSet.start();
		}

	}

	private int calcualateAngleActual(final int tTotal, final int tActal,
			final int aTotal) {

		return aTotal * tActal / tTotal;
	}

	private AnimatorListener createAnimationListener(
			final ListenerType listenerType) {
		if (listenerType.equals(ListenerType.NEXT))
			return new AnimatorListener() {

				@Override
				public void onAnimationCancel(final Animator animation) {
					actualAnimatorSet = nextAnimatorSet.clone();
					actualAnimatorSet.start();
				}

				@Override
				public void onAnimationEnd(final Animator animation) {
				}

				@Override
				public void onAnimationRepeat(final Animator animation) {
				}

				@Override
				public void onAnimationStart(final Animator animation) {
				}
			};
		else if (listenerType.equals(ListenerType.REPEAT))
			return new AnimatorListener() {

				@Override
				public void onAnimationCancel(final Animator animation) {
				}

				@Override
				public void onAnimationEnd(final Animator animation) {
					actualAnimatorSet = actualAnimatorSet.clone();
					actualAnimatorSet.start();
				}

				@Override
				public void onAnimationRepeat(final Animator animation) {
				}

				@Override
				public void onAnimationStart(final Animator animation) {
				}
			};

		else
			return null;
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
		if (!enabled) {
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
		if (!enabled) {
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
		if (!enabled) {
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

		Log.v(TAG, "onLayout()");

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

		// Start waiting animation
		if (animationSettingMemory != null) {
			Log.v(TAG, "Starting animation from memory: "
					+ animationSettingMemory.totalDuration + " "
					+ animationSettingMemory.type + " "
					+ animationSettingMemory.actualTime);
			startAnimation(animationSettingMemory.totalDuration,
					animationSettingMemory.type,
					animationSettingMemory.actualTime);
		}
	}

	private float radiusToPoint(final float radius) {
		return (float) (rectWidth / 2.0 - radius);
	}

	private void refresh() {
		if (needle != null) {
			panePaint.setColor(getColorPane());
			needle.setColor(getColorNeedle());
			ringPaint.setColor(getColorRing());
			invalidate();
		}
	}

	public void setCountInTime(final int countInTime) {
		this.countInTime = countInTime;
	}

	@Override
	public void setEnabled(final boolean enable) {
		this.enabled = enable;
		refresh();

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

		this.status = status;

		animationBecomeStop();

		refresh();

		// if (status.equals(Status.COUNT_IN))
		// startAnimation(countInTime, Type.COUNT_BACKWARDS, 0);

	}

	/**
	 * Splits the duration in duration + pause.
	 * 
	 * @param duration
	 *            integer with milliseconds. Will be modified. Must be >= 0.
	 * @return SplitResult new duration and pause in milliseconds
	 */
	private SplitResult splitDuration(final int duration, final int actualTime) {
		final int orig = duration;
		final SplitResult result = new SplitResult();

		if (orig < 50)
			result.duration = (int) (orig * 0.5);
		else if (orig < 500)
			result.duration = duration - 50;
		else
			result.duration = duration - 100;

		if (actualTime < result.duration)
			result.remainingTime = result.duration - actualTime;
		else
			result.remainingTime = 0;

		result.pause = orig - result.duration;
		return result;
	}

	/**
	 * Resets and starts the counter. Button must be active otherwise nothing
	 * happens. A valid duration in milliseconds for type == COUNT_BACKWARDS is
	 * [0..4000], for type == ONE_ROUND >=0 and for type == SECONDS, duration
	 * will be ignored. If the view is not inflated at calling time, only the
	 * setting will be stored an started later on (but only the last one).
	 * 
	 * @param totalDuration
	 *            int with milliseconds, but must be 0 for type == seconds
	 * @param type
	 * @param actualTime
	 *            int with startingPoint in milliseconds
	 */
	public void startAnimation(final int totalDuration, final Type type,
			final int actualTime) {
		Log.v(TAG, "startAnimation( ) " + totalDuration + " " + type.name()
				+ " " + actualTime);

		// If view not inflated, put the start info to memory
		if (needle == null) {
			Log.v(TAG, "Only write animation settings to memory.");
			animationSettingMemory = new AnimationSetting(totalDuration, type,
					actualTime);
			return;
		} else if (animationSettingMemory != null) {
			Log.v(TAG, "Dismiss animationSettingMemory");
			// Dismiss obsolet animation memory.
			animationSettingMemory = null;
		}

		if (type.equals(Type.SECONDS)) {
			if (totalDuration != 0)
				throw new RuntimeException("TotalDuration<>0: " + totalDuration);
		} else {
			if (totalDuration <= 0)
				throw new RuntimeException("TotalDuration<=0: " + totalDuration);
			if (actualTime > totalDuration)
				throw new RuntimeException("ActualTime > TotalDuration: "
						+ actualTime + " " + totalDuration);
		}

		if (actualTime < 0)
			throw new RuntimeException("ActualTime<0: " + actualTime);

		if (type.equals(Type.SECONDS))
			buildAnimation(RECORD_DURATION, type, actualTime);
		else
			buildAnimation(totalDuration, type, actualTime);

	}
}
