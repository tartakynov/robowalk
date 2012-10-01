package com.tartakynov.robotnoise;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.flurry.android.FlurryAgent;
import com.tartakynov.robotnoise.Preferences.OnPreferenceChangeListener;
import com.tartakynov.robotnoise.VolumeCircleView.ICircleAngleChanged;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
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

	mPreferences = Preferences.Open(getApplicationContext());
	mPreferences.registerPreferenceChangeListener(mPreferenceChangeListener);
	mCircleView = (VolumeCircleView)findViewById(R.id.imageView1);
	mCircleView.registerListener(mAngleChangeListener);
	mCircleView.setAngle(mPreferences.getAngle());

	startAnalyticsSession();
	doStartService();
	doBindService();
    }

    @Override
    protected void onDestroy() {		
	super.onDestroy();
	doUnbindService();
	stopAnalyticsSession();
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
	    if (mPreferences.isFeedbackRequested() && !isDebuggable()) {
		finish();        
	    } else {
		showFeedbackDialog();
		mPreferences.setFeedbackRequested();
	    }
	    break;
	case R.id.menu_about: // MENU ABOUT
	    showAboutDialog();
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
		if (mGATracker != null) {
		    mGATracker.dispatch();
		}
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
    private void doBindService() {
	if (!mIsServiceBound) {
	    Log.i(LOG_TAG, "doBindService");
	    bindService(new Intent(MainActivity.this, RobotService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	    mIsServiceBound = true;
	}
    }

    /***
     * Unbinds service if it's bound
     */
    private void doUnbindService() {
	if (mIsServiceBound) {
	    Log.i(LOG_TAG, "doUnbindService");
	    unbindService(mConnection);
	    mIsServiceBound = false;
	}
    }

    /***
     * Starts service if it's not running already
     */
    private void doStartService() {
	if (!RobotService.isRunning()) {
	    Log.i(LOG_TAG, "startService");
	    startService(new Intent(MainActivity.this, RobotService.class));		
	}
    }

    /***
     * Stops service if it's running
     */
    private void doStopService() {
	if (RobotService.isRunning())
	{
	    Log.i(LOG_TAG, "stopService");
	    stopService(new Intent(this, RobotService.class));
	}
    }

    /********************* Working with Analytics **********************/

    private void startAnalyticsSession() {
	if (!isDebuggable()) {	    
	    mGATracker = GoogleAnalyticsTracker.getInstance(); 
	    mGATracker.startNewSession(getResources().getString(R.string.ganalytics), this);
	    mGATracker.trackPageView("/MainActivity");
	    FlurryAgent.onStartSession(this, getResources().getString(R.string.flurry));	    
	    Log.i(LOG_TAG, "Started analytics sessions");
	}
    }

    private void stopAnalyticsSession() {
	if (mGATracker != null) {
	    mGATracker.dispatch();
	    mGATracker.stopSession();
	}
	FlurryAgent.onEndSession(this);
    }

    /********************* Private methods *****************************/

    private void showAboutDialog() {
	final Dialog dialog = new Dialog(this);
	dialog.setContentView(R.layout.about_dialog);
	dialog.setTitle(R.string.app_name);
	TextView txtVersion = (TextView)dialog.findViewById(R.id.txtVersion);
	String appName = getResources().getString(R.string.app_name);
	String appVersion = getResources().getString(R.string.app_version);
	txtVersion.setText(String.format("%s v.%s", appName, appVersion));
	Button btnClose = (Button) dialog.findViewById(R.id.btnClose);	
	btnClose.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View btn) {
		dialog.dismiss();
	    }});
	dialog.setCancelable(true);
	dialog.show();
    }

    private void showFeedbackDialog() {
	final Dialog dialog = new Dialog(this);
	dialog.setContentView(R.layout.feedback_dialog);
	dialog.setTitle(R.string.feedback_title);
	Button btnClose = (Button) dialog.findViewById(R.id.btnFeedback);	
	btnClose.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View btn) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.tartakynov.robotnoise"));
		startActivity(browserIntent);
		dialog.dismiss();
	    }
	});
	dialog.setOnDismissListener(new OnDismissListener() {
	    @Override
	    public void onDismiss(DialogInterface arg0) {
		finish();
	    }	    
	});
	dialog.setCancelable(true);
	dialog.show();
    }

    private boolean isDebuggable() {
	return ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    }

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
