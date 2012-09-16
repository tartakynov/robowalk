/**
 * 
 */
package com.tartakynov.robotnoise.leg;

import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class LegMovementDetector implements SensorEventListener {
	protected static final String 	LOG_TAG 				= "LegMovementDetector";
	protected static final float 	LEG_THRSHOLD_AMPLITUDE	= 1.0f;
	protected static final int 		LEG_THRSHOLD_INACTIVITY	= 10;
	protected static final int 		LEG_SENSOR_RATE			= SensorManager.SENSOR_DELAY_UI;	
	public static final int			LEG_MOVEMENT_NONE 		= 0;
	public static final int 		LEG_MOVEMENT_FORWARD	= 1;
	public static final int 		LEG_MOVEMENT_BACKWARD 	= 2;	
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private ArrayList<ILegMovementListener> mListeners = new ArrayList<ILegMovementListener>();	
	private float mLastZ;
	private int mLastActivity = LEG_MOVEMENT_NONE;
	private int mInactivityCount = 0;
	
	private ScalarKalmanFilter mFiltersCascade[] = new ScalarKalmanFilter[3];
	
	public LegMovementDetector(SensorManager sensorManager){
		this.mSensorManager = sensorManager;
		this.mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.mFiltersCascade[0] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);
		this.mFiltersCascade[1] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);
		this.mFiltersCascade[2] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int value) {
		// TODO Auto-generated method stub				
	}

	/**
	 * Starts detecting single leg movement
	 */
	public void startDetector(){
		mSensorManager.registerListener(this, this.mAccelerometer, LEG_SENSOR_RATE);
	}

	/**
	 * Stops detecting leg movement
	 */
	public void stopDetector(){
		mSensorManager.unregisterListener(this);
	}

	/**
	 * Adds listener
	 * @param listener
	 */
	public void addListener(ILegMovementListener listener){
		this.mListeners.add(listener);
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER){
			return;
		}
		final float z = filter(event.values[2]);				
//		final float dzdt = 2.5f * (z - this.mLastZ);// amplified derivative (LEG_SENSOR_RATE * (z - this.mLastZ)) / 1000000;
//		Log.i("LegMovementTest", String.valueOf(z) + "," + String.valueOf(dzdt));
		if (Math.abs(z - this.mLastZ) > LEG_THRSHOLD_AMPLITUDE)
		{
			this.mInactivityCount = 0;
			int currentActivity = (z > this.mLastZ) ? LEG_MOVEMENT_FORWARD : LEG_MOVEMENT_BACKWARD;			
			if (currentActivity != this.mLastActivity){
				this.mLastActivity = currentActivity;
				notifyListeners(currentActivity);
			}			
		} else {
			if (this.mInactivityCount > LEG_THRSHOLD_INACTIVITY) {
				if (this.mLastActivity != LEG_MOVEMENT_NONE){
					this.mLastActivity = LEG_MOVEMENT_NONE;
					notifyListeners(LEG_MOVEMENT_NONE);					
				}
			} else {
				this.mInactivityCount++;
			}
		}
		this.mLastZ = z;
	}
	
	private float filter(float measurement){
		float f1 = this.mFiltersCascade[0].correct(measurement);
		float f2 = this.mFiltersCascade[1].correct(f1);
		float f3 = this.mFiltersCascade[2].correct(f2);
		return f3;
	}
	
	private void notifyListeners(int activity){
		if (activity == 0) return;
		for (ILegMovementListener listener : mListeners){
				listener.onLegActivity(activity);
				Log.i(LOG_TAG, String.valueOf(activity));
		}									
	}
	
}
