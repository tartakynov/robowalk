package com.tartakynov.robotnoise;

import com.tartakynov.robotnoise.PocketDetector.IInPocketListener;
import com.tartakynov.robotnoise.Preferences.OnPreferenceChangeListener;
import com.tartakynov.robotnoise.VolumeCircleView.ICircleAngleChanged;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Shader.TileMode;
import android.hardware.SensorManager;

public class MainActivity extends Activity {

    public static final String LOG_TAG = "MainActivity";

    private RobotService mService;
    private PocketDetector mPocket;
    private Vibrator mVibrator;
    private boolean mIsServiceBound = false;
    private Button mPowerButton;
    private VolumeCircleView mCircleView;
    private Preferences mPreferences;
    
    /********************* Activity ************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);		
	setContentView(R.layout.activity_main);
	
	mPreferences = Preferences.Open(getApplicationContext());
	mPreferences.registerPreferenceChangeListener(mPreferenceChangeListener);
	mPowerButton = (Button)findViewById(R.id.button_power);
	mCircleView = (VolumeCircleView)findViewById(R.id.imageView1);
	mCircleView.registerListener(mAngleChangeListener);
	mCircleView.setAngle(mPreferences.getAngle());
	
	
	mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);	

	mPocket = new PocketDetector((SensorManager) getSystemService(SENSOR_SERVICE));
	mPocket.registerListener(mPocketDetectorListener);
	mPocket.start();	

	doStartService();
	doBindService();
    }
    
    @Override
    protected void onDestroy() {		
	super.onDestroy();
	mPocket.release();
	doUnbindService();
    }	

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.activity_main, menu);
	return true;       
    }

    /**
     * Menu callback handler 
     */    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId())
	{
	case R.id.menu_quit: // MENU QUIT
	    doStopService();
	    finish();        
	    break;
	}
	return true;
    }

//    /**
//     * Handles volume buttons click 
//     */
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//	if (mService != null) {
//	    Preferences pref = Preferences.Open(getApplicationContext());
//	    int volume = pref.getVolume();
//	    int action = event.getAction();
//	    int keyCode = event.getKeyCode();
//	    switch (keyCode) {
//	    case KeyEvent.KEYCODE_VOLUME_UP:
//		if ((action == KeyEvent.ACTION_UP) && (volume < 20)) {
//		    pref.setVolume(volume + 1);	            	
//		}
//		return true;
//	    case KeyEvent.KEYCODE_VOLUME_DOWN:
//		if ((action == KeyEvent.ACTION_DOWN) && (volume > 0)) {
//		    pref.setVolume(volume - 1);
//		}
//		return true;
//	    }	    	
//	}
//	return super.dispatchKeyEvent(event);
//    }

    /******************* Elements callback handlers *******************/

    /**
     * Power button click handler
     */
    public void onPowerButtonClick(View view) {
	Button btn = (Button)view;	
	if (mService != null) {
	    if (mService.isStarted()) {
		mService.stop();
	    } else {
		mService.start();		
		Toast.makeText(this, R.string.robot_power_on, Toast.LENGTH_SHORT).show();
	    }
	    btn.setSelected(mService.isStarted());
	    if (mVibrator != null) {
		mVibrator.vibrate(75);
	    }
	}
    }

    /******************* Working with VolumeCircleView ****************/
    
    private ICircleAngleChanged mAngleChangeListener = new ICircleAngleChanged() {

	@Override
	public void onAngleChanged(int angle) {	    
	    if (mPreferences != null) {
		mPreferences.setAngle(angle);
	    }
	}
	
    };

    /******************* Working with Preferences *********************/

    private OnPreferenceChangeListener mPreferenceChangeListener = new OnPreferenceChangeListener() {

	@Override
	public void onPreferenceChanged(Preferences pref) {
	    if (mService != null) {
		int volume = map(360, 20, pref.getAngle());
		mService.setVolume(volume);
	    }
	}

    };
    
    /******************* Working with RobotService ********************/

    private ServiceConnection mConnection = new ServiceConnection() {
	@Override
	public void onServiceConnected(ComponentName className, IBinder service) {
	    Log.i(LOG_TAG, "Service connected");
	    mService = ((RobotService.RobotBinder)service).getService();
	    int volume = map(360, 20, mPreferences.getAngle());
	    mService.setVolume(volume);
	    mPowerButton.setSelected(mService.isStarted());
	}

	@Override
	public void onServiceDisconnected(ComponentName className) {
	    mService = null;	    	
	    Log.i(LOG_TAG, "Service disconnected");
	}
    };

    /***
     * Binds service if it's not bound already
     */
    void doBindService() {
	if (!mIsServiceBound) {
	    Log.i(LOG_TAG, "doBindService");
	    bindService(new Intent(MainActivity.this, RobotService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	    mIsServiceBound = true;
	}
    }

    /***
     * Unbinds service if it's bound
     */
    void doUnbindService() {
	if (mIsServiceBound) {
	    Log.i(LOG_TAG, "doUnbindService");
	    unbindService(mConnection);
	    mIsServiceBound = false;
	}
    }

    /***
     * Starts service if it's not running already
     */
    void doStartService() {
	if (!RobotService.isRunning()) {
	    Log.i(LOG_TAG, "startService");
	    startService(new Intent(MainActivity.this, RobotService.class));		
	}
    }

    /***
     * Stops service if it's running
     */
    void doStopService() {
	if (RobotService.isRunning())
	{
	    Log.i(LOG_TAG, "stopService");
	    stopService(new Intent(this, RobotService.class));
	}
    }

    /******************* Working with Pocket detector *****************/

    private IInPocketListener mPocketDetectorListener = new IInPocketListener() {

	/**
	 * Called when you put the phone in pocket
	 */
	public void phoneInPocket() {
	    if (mService != null) {
		mService.startDetector();
	    }
	}

	/**
	 * Called when you take the phone out of pocket
	 */
	public void phoneOutOfPocket() {
	    if (mService != null) {
		mService.stopDetector();
	    }
	}
    };
    
    /********************* Private methods *****************************/

    /**
     * Maps integer value from one range to another
    */
    private int map(int maxFrom, int maxTo, int value) {
	return (int)((value) * ((float)maxTo / (float)maxFrom));
    }    
}
