/**
 * 
 */
package com.tartakynov.robotnoise;

import com.tartakynov.robotnoise.Preferences.OnPreferenceChangeListener;
import com.tartakynov.robotnoise.leg.ILegMovementListener;
import com.tartakynov.robotnoise.leg.LegMovementDetector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * @author Артем
 *
 */
public class RobotService extends Service implements OnPreferenceChangeListener {	
	
	private static final int NOTIFICATION 	= R.string.robot_service_label;
	private static final String WAKELOCK 	= "WL_TAG";    
    private static final String LOG_TAG		= "RobotService";
    
	private NotificationManager mNotificationManager;
	private SensorManager mSensorManager;    
	private PowerManager mPowerManager;
	private PowerManager.WakeLock mWakeLock;

    private LegMovementDetector mLegMovementDetector;    
    private ILegMovementListener mLegMovementActivityCallback;	
	private LegMovementPlayer mPlayer;
	private final IBinder mBinder = new RobotBinder();
	
	private static boolean sIsRunning = false;

    public class RobotBinder extends Binder {
    	RobotService getService() {
            return RobotService.this;
        }
    }
	
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
	
    @Override
    public void onCreate() {
    	sIsRunning = true;

    	// initialize class fields
        this.mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);		
    	this.mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    	this.mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mPlayer = new LegMovementPlayer(getApplicationContext());
    	
		// initialize wakelock
        this.mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK);
        this.mWakeLock.acquire();    	
        
		// initialize movement detector
        this.mLegMovementDetector = new LegMovementDetector(mSensorManager);
        this.mLegMovementDetector.addListener(mLegMovementListener);

		showNotification(NOTIFICATION);
		
		final Preferences pref = Preferences.Open(getApplicationContext());
		pref.registerPreferenceChangeListener(this);
		onPreferenceChanged(pref);
    }
        
    @Override
    public void onDestroy() {
    	sIsRunning = false;
    	this.mLegMovementDetector.stopDetector();
    	this.mNotificationManager.cancel(NOTIFICATION);    		
    	this.mPlayer.release(); 
    	this.mWakeLock.release();
    }        
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	return START_STICKY;
    }

	@Override
	public void onPreferenceChanged(Preferences pref) {
		Log.i(LOG_TAG, "onPreferenceChanged");
		this.mPlayer.setVolume(pref.getVolume());
	}
	
	public boolean start() {
		if (this.mLegMovementDetector == null) { // just to be on safe side
			return false;
		}
		this.mLegMovementDetector.startDetector();
		return true;
	}	

	/**
     * Show a notification while this service is running.
     */
    @SuppressWarnings("deprecation")
	private void showNotification(int id) {
        CharSequence text = getText(R.string.robot_service_text);
        Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.robot_service_label), text, contentIntent);
        mNotificationManager.notify(id, notification);
    }
    
    private ILegMovementListener mLegMovementListener = new ILegMovementListener() {
    	@Override
    	public void onLegActivity(int activity) {
    		if (RobotService.this.mLegMovementActivityCallback != null) {
    			RobotService.this.mLegMovementActivityCallback.onLegActivity(activity);
    		}
    		switch (activity) {
    		case LegMovementDetector.LEG_MOVEMENT_BACKWARD:
    			mPlayer.playBackward();
    			break;
    		case LegMovementDetector.LEG_MOVEMENT_FORWARD:
    			mPlayer.playForward();
    			break;
    		}									
    	}   	
    };
    
    public void setListener(ILegMovementListener callback) {
    	this.mLegMovementActivityCallback = callback;
    }
        
    public static boolean isRunning() {
    	return sIsRunning;
    }
}
