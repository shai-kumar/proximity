package com.zebra.proximity.command_manager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.chord.Schord;
import com.samsung.android.sdk.chord.SchordChannel;
import com.samsung.android.sdk.chord.SchordManager;
import com.samsung.android.sdk.chord.SchordManager.NetworkListener;
import com.zebra.proximity.videoplayer.VideoPlayer;

public class CommandManager {

	private static final String TAG = "CommandManager";

	private static final String COMMAND_CHANNEL = "com.zebra.proximityapp.command_manager.COMMAND_CHANNEL";
	private static final String COMMAND_MESSAGE = "com.zebra.proximityapp.command_manager.COMMAND_MESSAGE";

	private static CommandManager mInstance;
	private Context mContext;
	private SchordManager mSchordManager = null;
	private boolean isTV = false;
	private boolean mJoined = false;
	private String mFromNode = null;
	private String mFromChannel = null;

	private CommandManager(Context cntx) {
		mContext = cntx;
		isTV = isTV();
		init();
	}

	public static CommandManager getInstance(Context cntx) {
		if (mInstance == null) {
			mInstance = new CommandManager(cntx);
		}
		return mInstance;
	}

	private void init() {

		// initailize cord
		/****************************************************
		 * 1. GetInstance
		 ****************************************************/
		Schord chord = new Schord();
		try {
			chord.initialize(mContext);
		} catch (SsdkUnsupportedException e) {
			if (e.getType() == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED) {
				return;
			}
		}

		mSchordManager = new SchordManager(mContext);

		/****************************************************
		 * 2. Set some values before start If you want to use secured channel,
		 * you should enable SecureMode. Please refer
		 * UseSecureChannelFragment.java mChordManager.enableSecureMode(true);
		 * Once you will use sendFile or sendMultiFiles, you have to call
		 * setTempDirectory mChordManager.setTempDirectory(Environment.
		 * getExternalStorageDirectory().getAbsolutePath() + "/Chord");
		 ****************************************************/
		mSchordManager.setLooper(mContext.getMainLooper());

		/**
		 * Optional. If you need listening network changed, you can set callback
		 * before starting chord.
		 */
		mSchordManager.setNetworkListener(new NetworkListener() {

			@Override
			public void onDisconnected(int interfaceType) {
				Toast.makeText(mContext,
						getInterfaceName(interfaceType) + " is disconnected",
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onConnected(int interfaceType) {
				Toast.makeText(mContext,
						getInterfaceName(interfaceType) + " is connected",
						Toast.LENGTH_SHORT).show();
			}
		});

		startChord(SchordManager.INTERFACE_TYPE_WIFI);
	}

	private String getInterfaceName(int interfaceType) {
		if (SchordManager.INTERFACE_TYPE_WIFI == interfaceType)
			return "Wi-Fi";
		else if (SchordManager.INTERFACE_TYPE_WIFI_AP == interfaceType)
			return "Mobile AP";
		else if (SchordManager.INTERFACE_TYPE_WIFI_P2P == interfaceType)
			return "Wi-Fi Direct";

		return "UNKNOWN";
	}

	public void startChord(int interfaceType) {
		try {
			Log.d(TAG, "start cord");

			switch (interfaceType) {
			case SchordManager.INTERFACE_TYPE_WIFI:
				mSchordManager.start(interfaceType, mWifi_ManagerListener);
				break;
			}
		} catch (Exception e) {

		}
	}

	// ***************************************************
	// ChordManagerListener
	// ***************************************************
	private SchordManager.StatusListener mWifi_ManagerListener = new SchordManager.StatusListener() {

		@Override
		public void onStarted(String nodeName, int reason) {
			/**
			 * 4. Chord has started successfully
			 */

			if (reason == STARTED_BY_USER) {
				// Success to start by calling start() method
				joinTestChannel(SchordManager.INTERFACE_TYPE_WIFI);
			} else if (reason == STARTED_BY_RECONNECTION) {
				// Re-start by network re-connection.
			}

		}

		@Override
		public void onStopped(int reason) {
			/**
			 * 8. Chord has stopped successfully
			 */

			if (STOPPED_BY_USER == reason) {
				// Success to stop by calling stop() method

			} else if (NETWORK_DISCONNECTED == reason) {
				// Stopped by network disconnected
			}
		}
	};

	private void joinTestChannel(int interfaceType) {
		/**
		 * 5. Join my channel
		 */

		SchordChannel channel = null;
		SchordManager currentManager = null;

		currentManager = mSchordManager;

		switch (interfaceType) {
		case SchordManager.INTERFACE_TYPE_WIFI:
			channel = currentManager.joinChannel(COMMAND_CHANNEL,
					mWifi_ChannelListener);
			break;
		case SchordManager.INTERFACE_TYPE_WIFI_P2P:

			break;
		case SchordManager.INTERFACE_TYPE_WIFI_AP:

			break;
		}
	}

	public void stopChord() {
		/**
		 * 7. Stop Chord. You can call leaveChannel explicitly.
		 * mSchordManager_1.leaveChannel(CHORD_HELLO_TEST_CHANNEL);
		 */

		if (mSchordManager == null)
			return;

		mSchordManager.stop();

	}

	// ***************************************************
	// ChordChannelListener
	// ***************************************************
	private SchordChannel.StatusListener mWifi_ChannelListener = new SchordChannel.StatusListener() {

		int interfaceType = SchordManager.INTERFACE_TYPE_WIFI;

		/**
		 * Called when a node leave event is raised on the channel.
		 */
		@Override
		public void onNodeLeft(String fromNode, String fromChannel) {

		}

		/**
		 * Called when a node join event is raised on the channel
		 */
		@Override
		public void onNodeJoined(String fromNode, String fromChannel) {
			mFromNode = fromNode;
			mFromChannel = fromChannel;
			mJoined = true;

		}

		/**
		 * Called when the data message received from the node.
		 */
		@Override
		public void onDataReceived(String fromNode, String fromChannel,
				String payloadType, byte[][] payload) {
			/**
			 * Received data from other node
			 */

			if (payloadType.equals(COMMAND_MESSAGE)) {
				String cmd = new String(payload[0]);
				if (isTV) {
//					Intent in = new Intent(mContext, Slideshow.class);
//					in.putExtra("file_name", cmd);
					Intent in = new Intent(mContext, VideoPlayer.class);
					in.putExtra("path", "/sdcard/lifesense/videos/1.mp4");
					in.putExtra("startpos", Integer.parseInt(cmd));;
					mContext.startActivity(in);
				}
				Log.v(TAG, "onDataReceived: message");
			}
		}

		/**
		 * The following callBacks are not used in this Fragment. Please refer
		 * to the SendFilesFragment.java
		 */
		@Override
		public void onMultiFilesWillReceive(String fromNode,
				String fromChannel, String fileName, String taskId,
				int totalCount, String fileType, long fileSize) {

		}

		@Override
		public void onMultiFilesSent(String toNode, String toChannel,
				String fileName, String taskId, int index, String fileType) {

		}

		@Override
		public void onMultiFilesReceived(String fromNode, String fromChannel,
				String fileName, String taskId, int index, String fileType,
				long fileSize, String tmpFilePath) {

		}

		@Override
		public void onMultiFilesFinished(String node, String channel,
				String taskId, int reason) {

		}

		@Override
		public void onMultiFilesFailed(String node, String channel,
				String fileName, String taskId, int index, int reason) {

		}

		@Override
		public void onMultiFilesChunkSent(String toNode, String toChannel,
				String fileName, String taskId, int index, String fileType,
				long fileSize, long offset, long chunkSize) {

		}

		@Override
		public void onMultiFilesChunkReceived(String fromNode,
				String fromChannel, String fileName, String taskId, int index,
				String fileType, long fileSize, long offset) {

		}

		@Override
		public void onFileWillReceive(String fromNode, String fromChannel,
				String fileName, String hash, String fileType,
				String exchangeId, long fileSize) {

		}

		@Override
		public void onFileSent(String toNode, String toChannel,
				String fileName, String hash, String fileType, String exchangeId) {

		}

		@Override
		public void onFileReceived(String fromNode, String fromChannel,
				String fileName, String hash, String fileType,
				String exchangeId, long fileSize, String tmpFilePath) {

		}

		@Override
		public void onFileFailed(String node, String channel, String fileName,
				String hash, String exchangeId, int reason) {

		}

		@Override
		public void onFileChunkSent(String toNode, String toChannel,
				String fileName, String hash, String fileType,
				String exchangeId, long fileSize, long offset, long chunkSize) {

		}

		@Override
		public void onFileChunkReceived(String fromNode, String fromChannel,
				String fileName, String hash, String fileType,
				String exchangeId, long fileSize, long offset) {

		}

		/**
		 * The following callBacks are not used in this Fragment. Please refer
		 * to the UdpFrameworkFragment.java
		 */
		@Override
		public void onUdpDataReceived(String fromNode, String fromChannel,
				String payloadType, byte[][] payload, String serviceType) {

		}

		@Override
		public void onUdpDataDelivered(String fromNode, String channelName,
				String reqId) {

		}

	};

	public boolean sendCommand(String cmd) {
		if (!mJoined || cmd == null) {
			return false;
		}
		/**
		 * Send data to joined node
		 */
		byte[][] payload = new byte[1][];
		payload[0] = cmd.getBytes();

		SchordChannel channel = mSchordManager.getJoinedChannel(mFromChannel);

		if (channel == null) {

			return false;
		}

		try {
			channel.sendData(mFromNode, COMMAND_MESSAGE, payload);
		} catch (Exception e) {

			return false;
		}
		return true;
	}

	public static boolean isTV() {
		String configFilePath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/lifesense/config.txt";
		BufferedReader in = null;
		String line = null;
		try {
			in = new BufferedReader(new FileReader(configFilePath));
			line = in.readLine();
		} catch (FileNotFoundException e) {
			Log.v(TAG, "lifesense config.txt is missing");
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if ("tv=1".equals(line)) {
			return true;
		}
		return false;
	}

}
