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

package org.WifiConnect.batphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;

import org.WifiConnect.ServalBatPhoneApplication;
import org.WifiConnect.ServalBatPhoneApplication.State;
import org.WifiConnect.rhizome.Rhizome;
import org.WifiConnect.servald.PeerListService;
import org.WifiConnect.system.CommotionAdhoc;
import org.WifiConnect.system.NetworkManager;
import org.WifiConnect.system.WifiAdhocControl;
import org.WifiConnect.system.WifiApControl;


public class BatPhone extends BroadcastReceiver {

	static BatPhone instance = null;

	public BatPhone() {
		instance = this;
	}

	public static BatPhone getEngine() {
		// TODO Auto-generated method stub
		if (instance == null)
			instance = new BatPhone();
		return instance;
	}

	public static void call(String phoneNumber) {
		// make call by cellular/normal means
		// we need to ignore this number when it is dialed in the next 3 seconds

		dial_time = SystemClock.elapsedRealtime();
		dialed_number = phoneNumber;

		String url = "tel:" + phoneNumber;
		Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ServalBatPhoneApplication.context.startActivity(intent);
	}

	static String dialed_number = null;
	static long dial_time = 0;

	public static String getDialedNumber()
	{
		return dialed_number;
	}

	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		ServalBatPhoneApplication app = ServalBatPhoneApplication.context;

		try {
			
		    if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
				if (app.nm != null)
					app.nm.onFlightModeChanged(intent);

			} else if (action.equals(Intent.ACTION_MEDIA_EJECT)
					|| action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
				Rhizome.setRhizomeEnabled(false);

			} else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
				Rhizome.setRhizomeEnabled();

			} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				if (app.nm != null)
					app.nm.control.onWifiStateChanged(intent);
				if (app.controlService != null)
					app.controlService.onNetworkStateChanged();

			} else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				// TODO force network connections in the background?

			} else if (action
					.equals(WifiApControl.WIFI_AP_STATE_CHANGED_ACTION)) {
				if (app.nm != null)
					app.nm.control.onApStateChanged(intent);
				if (app.controlService != null)
					app.controlService.onNetworkStateChanged();

			} else if (action
					.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
				if (app.nm != null)
					app.nm.control.onSupplicantStateChanged(intent);

			} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				if (app.controlService != null)
					app.controlService.onNetworkStateChanged();

			} else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				// TODO?

			} else if (action
					.equals(WifiAdhocControl.ADHOC_STATE_CHANGED_ACTION)) {
				if (app.controlService != null)
					app.controlService.onNetworkStateChanged();

			} else if (action.equals(CommotionAdhoc.ACTION_STATE_CHANGED)) {
				int state = intent.getIntExtra(CommotionAdhoc.STATE_EXTRA, -1);
				if (app.nm != null)
					app.nm.control.commotionAdhoc.onStateChanged(state);
				if (app.controlService != null)
					app.controlService.onNetworkStateChanged();

			} else {
				Log.v("org.WifiConnect", "Unexpected intent: " + intent.getAction());
			}
		} catch (Exception e) {
			Log.e("org.WifiConnect", e.toString(), e);
		}
	}
}
