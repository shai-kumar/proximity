package com.zebra.proximity.videoplayer;

import java.util.Timer;
import java.util.TimerTask;

import com.zebra.proximityapp.R;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.MediaController;
import android.widget.Toast;

public class VideoPlayer extends Activity implements OnBufferingUpdateListener,
		OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener,
		SurfaceHolder.Callback, MediaController.MediaPlayerControl {

	private static final String TAG = "VideoPlayer";
	private int mVideoWidth;
	private int mVideoHeight;
	private MediaPlayer mMediaPlayer;
	private SurfaceView mPreview;
	private SurfaceHolder holder;
	private String path;
	private int PLAY_DURATION = 2000; // in ms
	private static int markerArray[] = { 0, 1, 2, 3, 4 }; // in minutes.
	private static int MARKER_LENGTH = markerArray.length;
	private static int startPosArray[]; // in ms
	{
		startPosArray = new int[markerArray.length];
		for (int i = 0; i < markerArray.length; i++) {
			startPosArray[i] = 60 * markerArray[i] * 1000 - PLAY_DURATION / 2;
			startPosArray[i] = startPosArray[i] < 0 ? 0: startPosArray[i];
		}
	}
	private int index = 0;
	private int startPos = startPosArray[index]; // in ms
	private Bundle extras;
	private static final String MEDIA = "media";
	private boolean mIsVideoSizeKnown = false;
	private boolean mIsVideoReadyToBePlayed = false;
	MediaController mc;
	private Timer t;

	/**
	 * 
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		path = getIntent().getExtras().getString("path");
		setContentView(R.layout.mediaplayer_2);
		mPreview = (SurfaceView) findViewById(R.id.surface);
		mPreview.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				mc.show();
				return false;
			}

		});
		holder = mPreview.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		extras = getIntent().getExtras();
		mc = new MediaController(this, true);
		mc.setAnchorView(findViewById(R.id.surface));
		t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				Log.v(TAG, "timer call");
				startPos = startPosArray[(++index % MARKER_LENGTH)]; // in ms
				mMediaPlayer.seekTo(startPos);
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(
								getApplicationContext(),
								"seek pos = " + String.valueOf(startPos
										+ " ms"), Toast.LENGTH_SHORT)
								.show();
					}
				});

			}
		}, PLAY_DURATION, PLAY_DURATION);
	}

	private void playVideo(Integer Media) {
		doCleanUp();
		try {
			// Create a new media player and set the listeners
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource(path);
			mMediaPlayer.setDisplay(holder);
			mMediaPlayer.prepare();
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnVideoSizeChangedListener(this);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mc.setMediaPlayer(this);
		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
	}

	public void onBufferingUpdate(MediaPlayer arg0, int percent) {
		Log.d(TAG, "onBufferingUpdate percent:" + percent);
	}

	public void onCompletion(MediaPlayer arg0) {
		Log.d(TAG, "onCompletion called");
	}

	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		Log.v(TAG, "onVideoSizeChanged called");
		if (width == 0 || height == 0) {
			Log.e(TAG, "invalid video width(" + width + ") or height(" + height
					+ ")");
			return;
		}
		mIsVideoSizeKnown = true;
		mVideoWidth = width;
		mVideoHeight = height;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	public void onPrepared(MediaPlayer mediaplayer) {
		Log.d(TAG, "onPrepared called");
		mIsVideoReadyToBePlayed = true;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
		Log.d(TAG, "surfaceChanged called");

	}

	public void surfaceDestroyed(SurfaceHolder surfaceholder) {
		Log.d(TAG, "surfaceDestroyed called");
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated called");
		playVideo(extras.getInt(MEDIA));

	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaPlayer();
		doCleanUp();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		releaseMediaPlayer();
		doCleanUp();
		t.cancel();
	}

	private void releaseMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	private void doCleanUp() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		mIsVideoReadyToBePlayed = false;
		mIsVideoSizeKnown = false;

	}

	private void startVideoPlayback() {
		Log.v(TAG, "startVideoPlayback");
		// holder.setFixedSize(mVideoWidth, mVideoHeight);
		mMediaPlayer.seekTo(startPos);
		mMediaPlayer.start();
	}

	@Override
	public boolean canPause() {

		return true;
	}

	@Override
	public boolean canSeekBackward() {

		return true;
	}

	@Override
	public boolean canSeekForward() {

		return true;
	}

	@Override
	public int getBufferPercentage() {

		return 0;
	}

	@Override
	public int getCurrentPosition() {

		return mMediaPlayer.getCurrentPosition();
	}

	@Override
	public int getDuration() {

		return mMediaPlayer.getDuration();
	}

	@Override
	public boolean isPlaying() {
		return mMediaPlayer.isPlaying();
	}

	@Override
	public void seekTo(int arg0) {
		mMediaPlayer.seekTo(arg0);

	}

	@Override
	public void start() {
		mMediaPlayer.start();
		t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				Log.v(TAG, "timer call");
				startPos = startPosArray[(++index % 5)]; // in ms
				mMediaPlayer.seekTo(startPos);
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(
								getApplicationContext(),
								String.valueOf(startPos
										+ " ms"), Toast.LENGTH_SHORT)
								.show();
					}
				});

			}
		}, PLAY_DURATION, PLAY_DURATION);
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void pause() {
		mMediaPlayer.pause();
		t.cancel();
	}
}
