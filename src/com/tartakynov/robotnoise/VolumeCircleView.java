package com.tartakynov.robotnoise;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class VolumeCircleView extends ImageView {

    private int mAngle = 0;
    private final RectF mOval = new RectF();	
    private final Paint mPaint = new Paint();
    private Canvas c2;
    private Bitmap overlay;
    
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

    @Override
    protected void onDraw(Canvas canvas) {
	super.onDraw(canvas);
	mOval.left = mOval.top = 45;
	mOval.right = mOval.bottom = this.getWidth() - 45;	
	canvas.drawArc(mOval, 90, mAngle, true, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	double centerX = getWidth() / 2.0f;
	double centerY = getHeight() / 2.0f;
	double touchedX = event.getX();
	double touchedY = event.getY();
	mAngle = (int) (Math.toDegrees(Math.atan2(centerY - touchedY, centerX - touchedX)));	
	if (mAngle < -90) {
	    mAngle = 360 + mAngle;
	}
	mAngle -= 270;			
	this.invalidate();
	return true;
    }
        
    private void init() {
	BitmapShader bitmapShader = new BitmapShader(BitmapFactory.decodeResource(getResources()
		, R.drawable.volume_background)
		, TileMode.CLAMP
		, TileMode.CLAMP);
	mPaint.setAntiAlias(true);
	mPaint.setColor(0xFF000000);
	mPaint.setShader(bitmapShader);
	mPaint.setDither(true);
	    
//	mPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_OVER));
//	mPaint.setColor(Color.TRANSPARENT);
//	mPaint.setAlpha(255);
//	mPaint.setStyle(Style.FILL);	
//	mPaint.setAntiAlias(true);
    }
}
