package com.tartakynov.robotnoise;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * @author Artem Tartakynov
 * A VolumeCircleView is an extension of ImageView and is a circle analog of SeekBar. The user can set the angle of arc by touch. 
 */
public class VolumeCircleView extends ImageView {

    /**
     * @author Artem Tartakynov
     * Used for receiving notifications from the VolumeCircleView when angle have changed
     */
    public interface ICircleAngleChanged {
	/**
	 * Called when angle have changed
	 */
	void onAngleChanged(int angle);
    }

    private final RectF mOval = new RectF();	
    private final Paint mPaint = new Paint();
    private final ArrayList<ICircleAngleChanged> mListeners = new ArrayList<ICircleAngleChanged>();
    private int mAngle = 0;

    public VolumeCircleView(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	init();
    }

    public VolumeCircleView(Context context, AttributeSet attrs) {
	super(context, attrs);
	init();
    }

    public VolumeCircleView(Context context) {
	super(context);
	init();
    }

    /********************* Public methods ******************************/

    public void setAngle(int angle) {
	mAngle = angle;
	invalidate();
    }

    public int getAngle() { 
	return mAngle; 
    }

    public void registerListener(ICircleAngleChanged listener) {
	mListeners.add(listener);
    }

    /********************* ImageView methods ***************************/

    @Override
    protected void onDraw(Canvas canvas) {
	super.onDraw(canvas);
	mOval.left = mOval.top = 0;
	mOval.right = mOval.bottom = this.getWidth();	
	canvas.drawArc(mOval, 90, mAngle, true, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	float centerX = getWidth() / 2.0f;
	float centerY = getHeight() / 2.0f;
	float touchedX = event.getX();
	float touchedY = event.getY();
	int angle = (int) (Math.toDegrees(Math.atan2(centerY - touchedY, centerX - touchedX))) + 90;	
	if (angle < 0) {
	    angle += 360;
	}
	if ((angle < 30) && (mAngle > 350)) {
	    return true;
	}	
	if ((angle > 330) && (mAngle < 10)) {
	    return true;
	}
	mAngle = angle;
	if (event.getAction() == MotionEvent.ACTION_UP) {
	    notifyListeners();
	}
	invalidate();
	return true;
    }

    /********************* Private methods *****************************/

    private void init() {
	BitmapShader bitmapShader = new BitmapShader(BitmapFactory.decodeResource(getResources()
		, R.drawable.volume_background)
		, TileMode.CLAMP
		, TileMode.CLAMP);
	mPaint.setColor(0xFF000000);
	mPaint.setShader(bitmapShader);
	mPaint.setAntiAlias(true);
	mPaint.setDither(true);
    }

    private void notifyListeners() {
	for (ICircleAngleChanged listener : mListeners) {
	    listener.onAngleChanged(mAngle);
	}
    }
}
