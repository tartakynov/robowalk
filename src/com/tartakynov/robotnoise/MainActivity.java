package com.tartakynov.robotnoise;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String LOG_TAG = "MainActivity";

    private RobotService mService;
    private Vibrator mVibrator;
    private boolean mIsServiceBound = false;
    private Button mPowerButton;
    private VolumeCircleView mCircleView;
    private Preferences mPreferences;
    private GoogleAnalyticsTracker mGATracker;
    
    /********************* Activity ************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);		
	setContentView(R.layout.activity_main);

	mPowerButton = (Button)findViewById(R.id.button_power);
	mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);	

	mGATracker = GoogleAnalyticsTracker.getInstance(); 
	
	mPreferences = Preferences.Open(getApplicationContext());
	mPreferences.registerPreferenceChangeListener(mPreferenceChangeListener);
	mCircleView = (VolumeCircleView)findViewById(R.id.imageView1);
	mCircleView.registerListener(mAngleChangeListener);
	mCircleView.setAngle(mPreferences.getAngle());

	doStartService();
	doBindService();
    }

    @Override
    protected void onDestroy() {		
	super.onDestroy();
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
	    setVolumeAngle(mService, pref.getAngle());
	}

    };

    /******************* Working with RobotService ********************/

    private ServiceConnection mConnection = new ServiceConnection() {
	@Override
	public void onServiceConnected(ComponentName className, IBinder service) {
	    Log.i(LOG_TAG, "Service connected");
	    mService = ((RobotService.RobotBinder)service).getService();
	    setVolumeAngle(mService, mPreferences.getAngle());
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

    /********************* Private methods *****************************/

    private static final void setVolumeAngle(RobotService service, int angle) {
	if (service != null) {
	    float volume = map(360, 1, angle);
	    service.setVolume(volume);	    
	}
    }

    /**
     * Maps integer value from one range to another
     */
    private static final float map(float maxFrom, float maxTo, float value) {
	return value * (maxTo / maxFrom);
    }    
}
