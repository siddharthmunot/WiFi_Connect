/**
 * The source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses/>.
 * Use this application at your own risk.
 */

package org.WifiConnect;

import java.io.IOException;


//import org.WifiConnect.PeerList.switchButtonListener;
import org.WifiConnect.ServalBatPhoneApplication.State;
import org.WifiConnect.servaldna.ServalDCommand;
import org.WifiConnect.servaldna.ServalDFailureException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 *
 * Control service responsible for turning Serval on and off and changing the
 * Wifi radio mode.
 *
 *
 */
public class Control extends Service {
	private ServalBatPhoneApplication app;
	private boolean servicesRunning = false;
	private boolean serviceRunning = false;
	private SimpleWebServer webServer;
	private int peerCount = -1;
	private PowerManager.WakeLock cpuLock;
	private WifiManager.MulticastLock multicastLock = null;
	private static final String TAG = "Control";
	
	public void onNetworkStateChanged() {
		if (serviceRunning) {
			new AsyncTask<Object, Object, Object>() {
				@Override
				protected Object doInBackground(Object... params) {
					modeChanged();
					return null;
				}
			}.execute();
		}
	}

	private Handler handler = new Handler();

	private synchronized void startServices() {
		if (servicesRunning)
			return;
		Log.d(TAG, "Starting services");
		servicesRunning = true;
		cpuLock.acquire();
		multicastLock.acquire();
		try {
			app.server.isRunning();
		} catch (ServalDFailureException e) {
			app.displayToastMessage(e.getMessage());
			Log.e(TAG, e.getMessage(), e);
		}
		peerCount=0;
		updateNotification();
		try {
			ServalDCommand.configActions(
					ServalDCommand.ConfigAction.del, "interfaces.0.exclude",
					ServalDCommand.ConfigAction.sync
			);
		} catch (ServalDFailureException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		try {
			if (webServer == null)
				webServer = new SimpleWebServer(8080);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private synchronized void stopServices() {
		if (!servicesRunning)
			return;

		Log.d(TAG, "Stopping services");
		servicesRunning = false;
		multicastLock.release();
		try {
			ServalDCommand.configActions(
					ServalDCommand.ConfigAction.set, "interfaces.0.exclude", "on",
					ServalDCommand.ConfigAction.sync
			);
		} catch (ServalDFailureException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		peerCount=-1;
		if (webServer != null) {
			webServer.interrupt();
			webServer = null;
		}

		this.stopForeground(true);
		cpuLock.release();
	}

	private synchronized void modeChanged() {
		boolean wifiOn = app.nm.isUsableNetworkConnected();

		Log.d(TAG, "modeChanged("+wifiOn+")");

		// if the software is disabled, or the radio has cycled to sleeping,
		// make sure everything is turned off.
		if (!serviceRunning)
			wifiOn = false;

		if (multicastLock == null)
		{
			WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			multicastLock = wm.createMulticastLock("org.WifiConnect");
		}

		if (wifiOn) {
			startServices();
		} else {
			stopServices();
		}
	}

	private void updateNotification() {
		if (!servicesRunning)
			return;
/*
		Notification notification = new Notification(
				R.drawable.ic_launcher, getString(R.string.app_name),
				System.currentTimeMillis());

		// Intent intent = new Intent(app, Main.class);
		Intent intent = new Intent(app, PeerList.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		notification.setLatestEventInfo(Control.this, getString(R.string.app_name),
				app.getResources().getQuantityString(R.plurals.peers_label, peerCount, peerCount),
				PendingIntent.getActivity(app, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT));

		notification.flags = Notification.FLAG_ONGOING_EVENT;
		this.startForeground(-1, notification);
		*/
		int mNotificationId = 001;
		Intent intent = new Intent(app, PeerList.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Intent intent1 = new Intent(app, NotificationClose.class);
		
		intent1.setAction("close");		
		PendingIntent pIntentClose = PendingIntent.getBroadcast(this, 0, intent1, 0);
		

	    // Build notification
	    // Actions are just fake
		NotificationCompat.Builder noti = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.ic_launcher)
	        .setContentTitle(getString(R.string.app_name))
	        .setContentText(app.getResources().getQuantityString(R.plurals.peers_label, peerCount, peerCount))
	        .setContentIntent(pIntent)
	        .addAction(R.drawable.audiocancel1, "Close", pIntentClose);
		
	       
		
		NotificationManager mNotifyMgr = 
		        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		noti.setOngoing(true);
		mNotifyMgr.notify(mNotificationId, noti.build());
		
	}

	private synchronized void startService() {
		app.controlService = this;
		app.setState(State.Starting);
		try {
			this.modeChanged();
			app.setState(State.On);
		} catch (Exception e) {
			app.setState(State.Off);
			Log.e("org.WifiConnect", e.getMessage(), e);
			app.displayToastMessage(e.getMessage());
		}
	}

	private synchronized void stopService() {
		app.setState(State.Stopping);
		app.nm.onStopService();
		stopServices();
		app.setState(State.Off);
		app.controlService = null;
	}

	public void updatePeerCount(int peerCount) {
		if (this.peerCount == peerCount)
			return;
		this.peerCount = peerCount;
		updateNotification();
	}

	class Task extends AsyncTask<State, Object, Object> {
		@Override
		protected Object doInBackground(State... params) {
			if (app.getState() == params[0])
				return null;

			if (params[0] == State.Off) {
				stopService();
			} else {
				startService();
			}
			return null;
		}
	}

	@Override
	public void onCreate() {
		this.app = (ServalBatPhoneApplication) this.getApplication();
		PowerManager pm = (PowerManager) app
				.getSystemService(Context.POWER_SERVICE);
		cpuLock = pm
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Services");

		super.onCreate();
	}

	@Override
	public void onDestroy() {
		new Task().execute(State.Off);
		app.controlService = null;
		serviceRunning = false;
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		State existing = app.getState();
		// Don't attempt to start the service if the current state is invalid
		// (ie Installing...)
		if (existing != State.Off && existing != State.On) {
			Log.v("Control", "Unable to process request as app state is "
					+ existing);
			return START_NOT_STICKY;
		}

		new Task().execute(State.On);

		serviceRunning = true;
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
