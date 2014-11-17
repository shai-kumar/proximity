package com.zebra.proximity.slideshow;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.zebra.proximity.DeviceManager;
import com.zebra.proximity.DeviceManager.OnDeviceChangeListener;
import com.zebra.proximity.MonitorService;
import com.zebra.proximity.command_manager.CommandManager;
import com.zebra.proximity.rf.ProximityEstimator.Distance;
import com.zebra.proximityapp.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class Slideshow extends Activity implements
		AdapterView.OnItemSelectedListener, ViewSwitcher.ViewFactory {

	private String TAG = "Slideshow";

	private Handler mHandler = new Handler();

	private ImageAdapter mImageAdapter;
	private ImageSwitcher mSwitcher;
	private Gallery mGallery;

	private List<CacheItem> mPlaylist = new ArrayList<CacheItem>();

	private Timer mTimer;
	private MediaPlayer mMediaPlayer;
	private int marker;
	private boolean commandSent = false;

	DeviceManager mDeviceManager;
	CommandManager mCommandManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.slideshow);
		initPlaylist();

		mSwitcher = (ImageSwitcher) findViewById(R.id.switcher);
		mSwitcher.setFactory(this);
		mSwitcher.setInAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in));
		mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_out));

		mImageAdapter = new ImageAdapter(this);
		mGallery = (Gallery) findViewById(R.id.gallery);
		mGallery.setAdapter(mImageAdapter);
		mGallery.setOnItemSelectedListener(this);

		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						marker %= mPlaylist.size();
						mGallery.setSelection(marker++);
						Log.v(TAG, "file = "
								+ mPlaylist.get(marker - 1).getPath());
					}

				});
			}

		}, 0, 2000);

		mCommandManager = CommandManager.getInstance(this);

		mDeviceManager = MonitorService.getDeviceManager();
		if (mDeviceManager != null) {
			mDeviceManager.addOnDeviceListener(new OnDeviceChangeListener() {

				@Override
				public void onDistanceMeasure(String mac, Distance distance) {
					Log.d(TAG, "Distance measured for " + mac + " at "
							+ distance.name());

					Message msg = mHandler.obtainMessage();
					msg.obj = distance.name();

					mHandler.sendMessage(msg);
					if (distance == Distance.CLOSE) {
						if (!commandSent) {
							if (mCommandManager.sendCommand(mPlaylist.get(
									marker - 1).getPath())) {
								commandSent = true;
								// pause slideshow
								pauseSlideshow();
							}
						}
					}
				}

				@Override
				public void onLoss(String mac) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onDiscover(String mac) {
					// TODO Auto-generated method stub

				}
			});
		}

		String currentFile = getIntent().getStringExtra("file_name");
		if (currentFile != null) {
			marker = mPlaylist.indexOf(new CacheItem(currentFile));
		}
	}

	private void pauseSlideshow() {
		if (mTimer != null) {
			Log.d(TAG, "Timer stopped.");
			mTimer.cancel();
			mTimer = null;
		}
		// stop audio
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// start audio
		mMediaPlayer = MediaPlayer.create(this, R.raw.test_cbr);
		mMediaPlayer.setLooping(true);
		mMediaPlayer.start();

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (mTimer != null) {
			Log.d(TAG, "Timer stopped.");
			mTimer.cancel();
			mTimer = null;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mTimer != null) {
			Log.d(TAG, "Timer stopped.");
			mTimer.cancel();
			mTimer = null;
		}
		// stop audio
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mCommandManager.stopChord();
		if (mTimer != null) {
			Log.d(TAG, "Timer stopped.");
			mTimer.cancel();
			mTimer = null;
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position,
			long id) {
		marker = position;

		Bitmap bm = mPlaylist.get(position).getBitmap();
		if (bm == null)
			return;

		// mSwitcher.setImageURI(Uri.fromFile(new File ( files[position] )));
		Drawable drawable = new BitmapDrawable(getResources(), bm);
		mSwitcher.setImageDrawable(drawable);

	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	@Override
	public View makeView() {
		ImageView i = new ImageView(this);
		i.setBackgroundColor(0xFF000000);
		i.setScaleType(ImageView.ScaleType.FIT_CENTER);
		i.setLayoutParams(new ImageSwitcher.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		return i;
	}

	public class ImageAdapter extends BaseAdapter {

		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			// return bitmaps.length;
			return mPlaylist.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView i = new ImageView(mContext);

			if (mPlaylist.get(position).getThumbnailBitmap() != null) {
				// i.setImageBitmap(bitmaps[position]);
				i.setImageBitmap(mPlaylist.get(position).getThumbnailBitmap());
			} else {
				Log.v(TAG, "thumbnail not ready yet");
				i.setImageResource(R.drawable.blank_shape);
			}
			i.setAdjustViewBounds(true);
			i.setLayoutParams(new Gallery.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			return i;
		}

	}

	public void initPlaylist() {

		File dir = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/lifesense/pictures/");
		String files[] = dir.list();
		if (files == null) {
			Log.v(TAG, dir + "is empty");
			return;
		}
		mPlaylist = new ArrayList<CacheItem>();
		Arrays.sort(files);
		for (int i = 0; i < files.length; i++) {
			if (files[i].endsWith(".jpg")) {
				String path = dir + "/" + files[i];
				mPlaylist.add(new CacheItem(path));
			}
		}

	}
}
