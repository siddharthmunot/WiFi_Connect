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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.WifiConnect.PeerList;

public class  NotificationClose extends BroadcastReceiver {
	private static final String TAG = "PeerList";
				@Override
	    public void onReceive(Context context, Intent intent) {
					PeerList app = PeerList.context1;
					if(app == null) {
						return;
					}
			Log.i(TAG, "Called Close App");
	    	if(intent.getAction().equals("close")) {
	    		
				Log.i(TAG, "Executing Close App");
				app.appClose();
	    	}
	    }

	}