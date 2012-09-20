package com.tartakynov.robotnoise.leg;

import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * @author Artem Tartakynov
 * Detects your leg's movement when the phone is in pocket
 */
public class LegMovementDetector implements SensorEventListener {

    /**
     * @author Artem Tartakynov
     * Used for receiving notifications from the LegMovementDetector when leg state have changed
     */
    public interface ILegMovementListener {
	/**
	 * Called when leg state have changed
	 */
	void onLegActivity(int activity);
    }

    protected static final String 	LOG_TAG 		= "LegMovementDetector";
    protected static final float 	LEG_THRSHOLD_AMPLITUDE	= 1.0f;
    protected static final int 		LEG_THRSHOLD_INACTIVITY	= 10;
    protected static final int 		LEG_SENSOR_RATE		= 60000; //SensorManager.SENSOR_DELAY_UI;	
    public static final int		LEG_MOVEMENT_NONE 	= 0;
    public static final int 		LEG_MOVEMENT_FORWARD	= 1;
    public static final int 		LEG_MOVEMENT_BACKWARD 	= 2;	

    private final ArrayList<ILegMovementListener> mListeners = new ArrayList<ILegMovementListener>();	
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float mLastZ;
    private int mLastActivity = LEG_MOVEMENT_NONE;
    private int mInactivityCount = 0;

    private ScalarKalmanFilter mFiltersCascade[] = new ScalarKalmanFilter[3];

    public LegMovementDetector(SensorManager sensorManager){
	mSensorManager = sensorManager;
	mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	mFiltersCascade[0] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);
	mFiltersCascade[1] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);
	mFiltersCascade[2] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);
    }

    /********************* Public methods ******************************/

    /**
     * Starts detecting single leg movement
     */
    public void startDetector(){
	mSensorManager.registerListener(this, mAccelerometer, LEG_SENSOR_RATE);
    }

    /**
     * Stops detecting leg movement
     */
    public void stopDetector(){
	mSensorManager.unregisterListener(this);
    }

    /**
     * Adds listener
     */
    public void addListener(ILegMovementListener listener){
	mListeners.add(listener);
    }

    /********************* SensorEventListener *************************/

    @Override
    public void onAccuracyChanged(Sensor sensor, int value) {
	// TODO Auto-generated method stub				
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
	if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER){
	    return;
	}
	final float z = filter(event.values[2]);				
	if (Math.abs(z - mLastZ) > LEG_THRSHOLD_AMPLITUDE)
	{
	    mInactivityCount = 0;
	    int currentActivity = (z > mLastZ) ? LEG_MOVEMENT_FORWARD : LEG_MOVEMENT_BACKWARD;			
	    if (currentActivity != mLastActivity){
		mLastActivity = currentActivity;
		notifyListeners(currentActivity);
	    }			
	} else {
	    if (mInactivityCount > LEG_THRSHOLD_INACTIVITY) {
		if (mLastActivity != LEG_MOVEMENT_NONE){
		    mLastActivity = LEG_MOVEMENT_NONE;
		    notifyListeners(LEG_MOVEMENT_NONE);					
		}
	    } else {
		mInactivityCount++;
	    }
	}
	mLastZ = z;
    }
    
    /********************* Private methods *****************************/

    /**
     * Smoothes the signal from accelerometer
     */
    private float filter(float measurement){
	float f1 = mFiltersCascade[0].correct(measurement);
	float f2 = mFiltersCascade[1].correct(f1);
	float f3 = mFiltersCascade[2].correct(f2);
	return f3;
    }
    
    /**
     * Calls registered event listeners
     */
    private void notifyListeners(int activity){
	if (activity == 0) return;
	for (ILegMovementListener listener : mListeners){
	    listener.onLegActivity(activity);
	    Log.i(LOG_TAG, String.valueOf(activity));
	}									
    }
}
