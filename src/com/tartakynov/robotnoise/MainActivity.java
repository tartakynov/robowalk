package com.tartakynov.robotnoise;

import com.tartakynov.robotnoise.leg.ILegMovementListener;
import com.tartakynov.robotnoise.leg.LegMovementDetector;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	public static final String LOG_TAG = "MainActivity";
	
	private TextView mStatusText;
	private RobotService mService;
	private boolean mIsServiceBound = false;
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
        setContentView(R.layout.activity_main);        
        this.mStatusText = (TextView)findViewById(R.id.statusText);
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

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	    if (this.mService != null) {
		    Preferences pref = Preferences.Open(getApplicationContext());
		    int volume = pref.getVolume();
		    int action = event.getAction();
		    int keyCode = event.getKeyCode();
		    switch (keyCode) {
	        case KeyEvent.KEYCODE_VOLUME_UP:
	            if (action == KeyEvent.ACTION_UP) {
	            	pref.setVolume(volume + 1);	            	
	            	mStatusText.setText("" + (volume + 1));
	            }
	            return true;
	        case KeyEvent.KEYCODE_VOLUME_DOWN:
	            if ((action == KeyEvent.ACTION_DOWN) && (volume > 0)) {
	            	pref.setVolume(volume - 1);
	            	mStatusText.setText("" + (volume - 1));
	            }
	            return true;
	        }	    	
	    }
        return super.dispatchKeyEvent(event);
	}
	
	/******************* Working with RobotService ********************/
	
	private ServiceConnection mConnection = new ServiceConnection() {
	    @Override
		public void onServiceConnected(ComponentName className, IBinder service) {
	    	Log.i(LOG_TAG, "Service connected");
	        mService = ((RobotService.RobotBinder)service).getService();
	        mService.setListener(mListener);	        
	        mService.start();
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

	/******************************************************************/
	
	ILegMovementListener mListener = new ILegMovementListener(){

		@Override
		public void onLegActivity(int activity) {
			switch (activity) {
			case LegMovementDetector.LEG_MOVEMENT_NONE:
				MainActivity.this.mStatusText.setText("Stopped");
				break;
			case LegMovementDetector.LEG_MOVEMENT_BACKWARD:
				MainActivity.this.mStatusText.setText("Backward");
				break;
			case LegMovementDetector.LEG_MOVEMENT_FORWARD:
				MainActivity.this.mStatusText.setText("Forward");
				break;
			}
		}
		
	};
}
