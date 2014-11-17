package com.zebra.proximity.slideshow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class CacheItem {

	private String mPath = null;
	private Bitmap mThumbnailBitmap = null;
	private Bitmap mBitmap = null;

	public CacheItem(String path) {
		mPath = path;
		createBitmap();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof CacheItem)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		CacheItem rhs = (CacheItem) obj;
		return mPath == null ? rhs.mPath == null : mPath.equals(rhs.mPath);
	}

	@Override
	public int hashCode() {
		return mPath.hashCode();
	}

	public String getPath() {
		return mPath;
	}

	public Bitmap getBitmap() {
		if (mBitmap == null) {
			mBitmap = getResizedBitmap(mPath, 760, 840);
		}
		return mBitmap;
	}

	public Bitmap getThumbnailBitmap() {
		if (mThumbnailBitmap == null) {
			mThumbnailBitmap = getResizedBitmap(mPath, 380, 420);
		}
		return mThumbnailBitmap;
	}

	/*
	 * Create bitmap from local file path
	 */
	public void createBitmap() {
		/*
		 * new Thread( new Runnable() {
		 * 
		 * @Override public void run() { mThumbnailBitmap =
		 * getResizedBitmap(mPath, 380, 420); mBitmap = getResizedBitmap(mPath,
		 * 760, 840); } }).start();
		 */
	}

	private Bitmap getResizedBitmap(String imagePath, int maxW, int maxH) {

		if (imagePath == null) {
			return null;
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imagePath, options);

		int scaleW = options.outWidth / maxW + 1;
		int scaleH = options.outHeight / maxH + 1;
		int scale = Math.max(scaleW, scaleH);

		options.inJustDecodeBounds = false;
		options.inSampleSize = scale;
		return BitmapFactory.decodeFile(imagePath, options);

	}
}
