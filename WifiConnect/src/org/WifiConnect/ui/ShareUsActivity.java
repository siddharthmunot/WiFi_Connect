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


package org.WifiConnect.ui;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.WifiConnect.R;
import org.WifiConnect.ServalBatPhoneApplication;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ShareUsActivity extends Activity {
	private static final String TAG = "ShareUsActivity";
	TextView shareWifi;

	private void updateHelpText() {
		ServalBatPhoneApplication app = (ServalBatPhoneApplication) this
				.getApplication();

		String ssid = null;
		InetAddress addr = null;

		try {
			if (app.nm.control.wifiManager.isWifiEnabled()){
				NetworkInfo networkInfo = app.nm.control.connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				WifiInfo connection = app.nm.control.wifiManager.getConnectionInfo();
				if (networkInfo!=null && networkInfo.isConnected() && connection!=null) {
					int iAddr = connection.getIpAddress();
					addr = Inet4Address.getByAddress(new byte[]{
							(byte) iAddr,
							(byte) (iAddr >> 8),
							(byte) (iAddr >> 16),
							(byte) (iAddr >> 24),
					});
					ssid = connection.getSSID();
			}
			}else if(app.nm.control.wifiApManager.isWifiApEnabled()){
				WifiConfiguration conf = app.nm.control.wifiApManager.getWifiApConfiguration();
				if (conf!=null && conf.SSID!=null)
					ssid = conf.SSID;

				// TODO FIXME get the real AP network address
				addr = Inet4Address.getByAddress(new byte[] {
						(byte) 192, (byte) 168, 43, 1,
				});
			}
		} catch (UnknownHostException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		String helpText = null;
		if (addr != null && ssid != null)
			helpText = getString(R.string.share_wifi, ssid,"http://" + addr.getHostAddress()
							+ ":8080/");
		else
			helpText = getString(R.string.share_wifi_off);
		shareWifi.setText(helpText);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.shareus);

		shareWifi = (TextView) findViewById(R.id.share_wifi);
		updateHelpText();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

}
