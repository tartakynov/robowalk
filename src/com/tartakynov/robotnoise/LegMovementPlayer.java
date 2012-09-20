package com.tartakynov.robotnoise;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;

/**
 * @author Artem Tartakynov
 * Plays the sounds of leg movement
 */
public class LegMovementPlayer implements OnAudioFocusChangeListener {	
    private final static int PLAYER_FORWARD 	= 0;
    private final static int PLAYER_BACKWARD 	= 1;

    private final AudioManager mAudioManager;	
    private final Context mContext;
    private MediaPlayer[] mPlayers = new MediaPlayer[2];
    private Object mSync = new Object();
    private boolean mCanPlay = false;

    public LegMovementPlayer(Context context) {
	this.mContext = context;
	this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
	if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
	    init();
	}
    }

    /********************* Public methods ******************************/

    /**
     * Plays forward movement sound
     */
    public void playForward() {
	play(PLAYER_FORWARD);
    }

    /**
     * Plays backward movement sound
     */
    public void playBackward() {
	play(PLAYER_BACKWARD);		
    }

    /**
     * Sets volume
     */
    public void setVolume(int volume) {
	mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    /**
     * Call this method when you are done with this instance
     */ 
    public void release() {
	synchronized (mSync) {
	    for (int i = 0; i < mPlayers.length; i++) {
		if (mCanPlay) {
		    if (mPlayers[i].isPlaying()) mPlayers[i].stop();
		    mPlayers[i].release();
		    mPlayers[i] = null;					
		    mCanPlay = false;				
		}
	    }			
	}
    }

    /**
     * Initializes current instance
     */ 
    public void init() {
	synchronized (mSync) {
	    mPlayers[PLAYER_FORWARD] = MediaPlayer.create(mContext, R.raw.forward);
	    mPlayers[PLAYER_BACKWARD] = MediaPlayer.create(mContext, R.raw.backward);
	    mPlayers[PLAYER_FORWARD].setVolume(1.0f, 1.0f);
	    mPlayers[PLAYER_BACKWARD].setVolume(1.0f, 1.0f);
	    mCanPlay = true;							
	}
    }

    /********************* OnAudioFocusChangeListener ******************/
    
    /**
     * Handles audio focus change for this listener
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
	synchronized (mSync) {
	    switch (focusChange) {
	    case AudioManager.AUDIOFOCUS_GAIN:
		init();
		break;
	    case AudioManager.AUDIOFOCUS_LOSS:
		release();
		break;
	    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
		mCanPlay = false;				
		break;		
	    }
	}
    }

    /********************* Private methods *****************************/

    /**
     * Plays specified player if it's allowed
     */
    private void play(int player) {
	if (mCanPlay) {
	    mPlayers[player].start();			
	}
    }
}
