package com.tartakynov.robotnoise;

import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * @author Artem Tartakynov
 * Detects whether is your phone in the pocket or not using proximity sensor
 */
public class PocketDetector implements SensorEventListener {
    /**
     * @author Artem Tartakynov
     * Used for receiving notifications from the PocketDetector when phone state have changed
     */
    public interface IInPocketListener {
	/**
	 * Called when you put the phone in pocket
	 */
	void phoneInPocket();

	/**
	 * Called when you take the phone out of pocket
	 */
	void phoneOutOfPocket();
    }

    protected final static String LOG_TAG = "PocketDetector"; 
    protected final static float DISTANCE_THRESHOLD = 2.0f;  // 2cm
    protected final static int STATE_NONE 			= 0; // undetermined state 
    protected final static int STATE_IN_POCKET 		= 1; // phone in the pocket
    protected final static int STATE_OUT_OF_POCKET 	= 2; // phone out of pocket

    private final ArrayList<IInPocketListener> mListeners = new ArrayList<IInPocketListener>();
    private final SensorManager mSensorManager;
    private final Sensor mProximity;
    private int mState = STATE_NONE;
    private boolean mIsListenerRegistered = false;

    public PocketDetector(SensorManager sensorManager) {
	mSensorManager = sensorManager;
	mProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    /********************* Public methods ******************************/

    /**
     * Starts detector
     */
    public void start() {
	if (!mIsListenerRegistered) {
	    mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
	    mIsListenerRegistered = true;
	}
    }

    /**
     * Stops detector
     */
    public void stop() {  
	if (mIsListenerRegistered) {
	    mSensorManager.unregisterListener(this);
	    mIsListenerRegistered = false;
	}
    }

    /**
     * Call this method when you are done with this instance
     */ 
    public void release() {
	stop();
	mListeners.clear();
    }

    /**
     * Registers event listener
     */
    public void registerListener(IInPocketListener listener) {
	mListeners.add(listener);
    }

    /********************* SensorEventListener *************************/

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
	// Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
	float distance = event.values[0];
	int state = (distance < DISTANCE_THRESHOLD) ? STATE_IN_POCKET : STATE_OUT_OF_POCKET;
	if (state != mState) {
	    mState = state;
	    notifyListeners();
	}
    }

    /********************* Private methods *****************************/

    /**
     * Calls registered event listeners
     */
    private void notifyListeners() {
	for (IInPocketListener listener : mListeners) {
	    switch (mState) {
	    case STATE_IN_POCKET:
		listener.phoneInPocket();
		Log.i(LOG_TAG, "in pocket");
		break;
	    case STATE_OUT_OF_POCKET:
		listener.phoneOutOfPocket();
		Log.i(LOG_TAG, "out of pocket");
		break;
	    }
	}
    }
}
