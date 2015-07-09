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

package org.WifiConnect.account;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;

public class SyncService extends Service {
	private static SyncAdapter syncAdapter = null;

	private class SyncAdapter extends AbstractThreadedSyncAdapter {
		public SyncAdapter(Context context, boolean autoInitialize) {
			super(context, autoInitialize);
		}

		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if ("android.content.SyncAdapter".equals(intent.getAction())) {
			if (syncAdapter == null)
				syncAdapter = new SyncAdapter(this, false);

			return syncAdapter.getSyncAdapterBinder();
		}
		return null;
	}

}
