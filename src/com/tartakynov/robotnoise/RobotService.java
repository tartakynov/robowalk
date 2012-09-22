package com.tartakynov.robotnoise;

import com.tartakynov.robotnoise.PocketDetector.IInPocketListener;
import com.tartakynov.robotnoise.leg.LegMovementDetector;
import com.tartakynov.robotnoise.leg.LegMovementDetector.ILegMovementListener;

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

/**
 * @author Артем
 *
 */
public class RobotService extends Service {	
    private static final int NOTIFICATION 	= R.string.robot_service_label;
    private static final String WAKELOCK 	= "WL_TAG";    

    private static boolean sIsRunning = false;

    private final IBinder mBinder = new RobotBinder();
    private NotificationManager mNotificationManager;
    private SensorManager mSensorManager;    
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private LegMovementDetector mLegMovementDetector;    
    private LegMovementPlayer mPlayer;
    private PocketDetector mPocket;
    private boolean mIsStarted = false;

    public class RobotBinder extends Binder {
	RobotService getService() {
	    return RobotService.this;
	}
    }

    /**
     * Used for receiving notifications from the LegMovementDetector when leg state have changed
     */
    private ILegMovementListener mLegMovementListener = new ILegMovementListener() {
	@Override
	public void onLegActivity(int activity) {
	    if (!mIsStarted) return;
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

    /********************* Service *************************************/

    @Override
    public IBinder onBind(Intent intent) {
	return mBinder;
    }

    @Override
    public void onCreate() {
	sIsRunning = true;

	// initialize class fields
	mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);		
	mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	mPlayer = new LegMovementPlayer(getApplicationContext());

	// initialize wakelock
	mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK);
	mWakeLock.acquire();    	

	// initialize movement detector
	mLegMovementDetector = new LegMovementDetector(mSensorManager);
	mLegMovementDetector.addListener(mLegMovementListener);

	// initialize pocket detector
	mPocket = new PocketDetector((SensorManager) getSystemService(SENSOR_SERVICE));
	mPocket.registerListener(mPocketDetectorListener);
	mPocket.start();	

	showNotification(NOTIFICATION);
    }

    @Override
    public void onDestroy() {
	sIsRunning = false;
	mLegMovementDetector.stopDetector();
	mNotificationManager.cancel(NOTIFICATION);    		
	mPocket.release();
	mPlayer.release(); 
	mWakeLock.release();
    }        

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	return START_STICKY;
    }

    /********************* Public methods*******************************/

    public void start() {
	mPocket.start();
	mIsStarted = true;	
    }	

    public void stop() {
	mPocket.stop();			
	mLegMovementDetector.stopDetector();
	mIsStarted = false;	
    }

    public boolean isStarted() {
	return mIsStarted;
    }

    public void setVolume(float volume) {
	if (mPlayer != null) {
	    mPlayer.setVolume(volume);
	}	
    }

    public static boolean isRunning() {
	return sIsRunning;
    }

    /******************* Working with Pocket detector *****************/

    private IInPocketListener mPocketDetectorListener = new IInPocketListener() {

	/**
	 * Called when you put the phone in pocket
	 */
	public void phoneInPocket() {
	    if (mLegMovementDetector != null) { // just to be on safe side
		mLegMovementDetector.startDetector();
	    }
	}

	/**
	 * Called when you take the phone out of pocket
	 */
	public void phoneOutOfPocket() {
	    if (mLegMovementDetector != null) {
		mLegMovementDetector.stopDetector();			
	    }
	}
    };

    /********************* Private methods *****************************/

    /**
     * Show a notification while this service is running.
     */
    @SuppressWarnings("deprecation")
    private void showNotification(int id) {
	CharSequence text = getText(R.string.robot_service_text);
	Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
	notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
	Intent intent = new Intent(getApplicationContext(), MainActivity.class);
	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
	notification.setLatestEventInfo(this, getText(R.string.robot_service_label), text, contentIntent);
	mNotificationManager.notify(id, notification);
    }
}
