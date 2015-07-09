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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.WifiConnect.ServalBatPhoneApplication.State;
import org.WifiConnect.account.AccountService;
import org.WifiConnect.batphone.CallHandler;
import org.WifiConnect.servald.IPeerListListener;
import org.WifiConnect.servald.Identity;
import org.WifiConnect.servald.Peer;
import org.WifiConnect.servald.PeerComparator;
import org.WifiConnect.servald.PeerListService;
import org.WifiConnect.share.activities.BaseActivity;
import org.WifiConnect.share.activities.MainActivity;
import org.WifiConnect.ui.ShareUsActivity;
import org.WifiConnect.ui.help.HtmlHelp;
import org.WifiConnect.wizard.Wizard;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 *
 *
 *         Peer List fetches a list of known peers from the PeerListService.
 *         When a peer is received from the service this activity will attempt
 *         to resolve the peer by calling ServalD in an async task.
 */
public class PeerList extends ListActivity {
	public ServalBatPhoneApplication app;

	private PeerListAdapter<Peer> listAdapter;


	private static final String TAG = "PeerList";

	public static final String PICK_PEER_INTENT = "org.WifiConnect.PICK_FROM_PEER_LIST";

	public static final String CONTACT_NAME = "org.WifiConnect.PeerList.contactName";
	public static final String CONTACT_ID = "org.WifiConnect.PeerList.contactId";
	public static final String DID = "org.WifiConnect.PeerList.did";
	public static final String SID = "org.WifiConnect.PeerList.sid";
	public static final String NAME = "org.WifiConnect.PeerList.name";
	public static final String RESOLVED = "org.WifiConnect.PeerList.resolved";

	protected static String PASTETEXT = null;
	public static PeerList context1;

	private boolean returnResult = false;

	private List<Peer> peers = new ArrayList<Peer>();
	Intent serviceIntent;
	int sdk = android.os.Build.VERSION.SDK_INT;
	CharSequence pasteText = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = (ServalBatPhoneApplication) this.getApplication();
		context1 = this;

		Intent intent = getIntent();
		if (intent != null) {
			if (PICK_PEER_INTENT.equals(intent.getAction())) {
				returnResult = true;
			}
		}
	       
		Log.i(TAG, "calling clipboardPaste");
		clipboardPaste();
		listAdapter = new PeerListAdapter<Peer>(this, peers);
		listAdapter.setNotifyOnChange(false);
		this.setListAdapter(listAdapter);
		ListView lv = getListView();

		// TODO Long click listener for more options, eg text message

		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				try {
					Peer p = listAdapter.getItem(position);
					if (returnResult) {
						Log.i(TAG, "returning selected peer " + p);
						Intent returnIntent = new Intent();
						returnIntent.putExtra(
								CONTACT_NAME,
								p.getContactName());
						returnIntent.putExtra(SID, p.sid.toHex());
						returnIntent.putExtra(CONTACT_ID, p.contactId);
						returnIntent.putExtra(DID, p.did);
						returnIntent.putExtra(NAME, p.name);
						returnIntent.putExtra(PASTETEXT, pasteText);
						returnIntent.putExtra(RESOLVED,
								p.cacheUntil > SystemClock.elapsedRealtime());
						setResult(Activity.RESULT_OK, returnIntent);
						finish();
					} else {
						Log.i(TAG, "calling selected peer " + p);
						CallHandler.dial(p);
					}
				} catch (Exception e) {
					ServalBatPhoneApplication.context.displayToastMessage(e
							.getMessage());
					Log.e("org.WifiConnect", e.getMessage(), e);
				}
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch (id) {
		
		case R.id.Settings:
		   	 startActivity(new Intent(getApplicationContext(),
					org.WifiConnect.wizard.SetPhoneNumber.class));
			break;
		
		case R.id.Share:
		   	 startActivity(new Intent(getApplicationContext(),
					ShareUsActivity.class));
			break;
		
		case R.id.Stop_Server:
			BaseActivity.stopServer();
			PASTETEXT = null;
			onResume();
			break;
		
		case R.id.helpLabel:
		     Intent intent = new Intent(getApplicationContext(),
					HtmlHelp.class);
			intent.putExtra("page", "helpWiCo.html");
			startActivity(intent);
			break;
		
		case R.id.Attachment:
			Log.i(TAG, "Called Attachement");
			Intent intent2 = new Intent(getApplicationContext(),
					MainActivity.class);
			startActivity(intent2);
			break;		
				
		case R.id.Exit:
			appClose();
		}
		return super.onOptionsItemSelected(item);
	}

	boolean registered = false;
	BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int stateOrd = intent.getIntExtra(
					ServalBatPhoneApplication.EXTRA_STATE, 0);
			State state = State.values()[stateOrd];
			}
	};
	
	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		if (intent != null) {
			if (PICK_PEER_INTENT.equals(intent.getAction())) {
				returnResult = true;
			}
		}
	}

	private void peerUpdated(Peer p) {
		if (!peers.contains(p)){
			if (!p.isReachable())
				return;
			peers.add(p);
		}
		if (!p.hasName()) {
			peers.remove(p);
		}
		Collections.sort(peers, new PeerComparator());
		listAdapter.notifyDataSetChanged();
	}

	private IPeerListListener listener = new IPeerListListener() {
		@Override
		public void peerChanged(final Peer p) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					peerUpdated(p);
				};

			});
		}
	};

	@Override
	protected void onPause() {
		super.onPause();
		if (registered) {
			this.unregisterReceiver(receiver);
			registered = false;
		}
		PeerListService.removeListener(listener);
		peers.clear();
		listAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkAppSetup();
		clipboardPaste();

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				if (!PeerListService.havePeers()) {
					Log.i(TAG, "No Peers Found");
					app.displayToastMessage("No Peers Found, will continue Searching..");
				}
				PeerListService.addListener(listener);
				return null;
			}

		}.execute();
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	// Before 2.0
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void clipboardPaste() {
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			pasteText = clipboard.getText().toString();
		} else {
			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard.hasPrimaryClip() == true) {
				ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
				pasteText = item.getText();
				}

			else {
				Log.i(TAG, "Nothing to Paste");
				return;

			}
		}
		if (pasteText != null) {
			pasteText = pasteText.toString();
		}
		else {
			return;
		}
		Log.i(TAG, "pasteText: " + pasteText);
		if (!((String) pasteText).contains("9999")) {
			pasteText = null;
		}
		else {
			ClipboardManager clipboard = (ClipboardManager)
					getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setPrimaryClip(ClipData.newPlainText(null, null));
		}
		PASTETEXT = (String) pasteText;
		pasteText = null;
	}
	public void appClose() {
		BaseActivity.stopServer();
		PASTETEXT = null;
		NotificationManager mNotifyMgr = 
		        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);;
		mNotifyMgr.cancel(001);
		serviceIntent = new Intent(this, Control.class);
		stopService(serviceIntent);
		super.moveTaskToBack(true);
		return;
	}

	/**
	 * Run initialisation procedures to setup everything after install. Called
	 * from onResume() and after agreeing Warning dialog
	 */
	private void checkAppSetup() {
		State state = app.getState();
		// stateChanged(state);

		if (state == State.Installing || state == State.Upgrading) {
			app.installFiles();

			if (state == State.Installing) {
				this.startActivity(new Intent(this, Wizard.class));
				finish();
				return;
			}
		}

		// Start by showing the preparation wizard
		
		Identity main = Identity.getMainIdentity();
		if (main == null || AccountService.getAccount(this) == null
				|| main.getDid() == null) {
			Log.v("MAIN",
					"Keyring doesn't seem to be initialised, starting wizard");

			this.startActivity(new Intent(this, Wizard.class));
			finish();
			return;
		}

		if (!registered) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(ServalBatPhoneApplication.ACTION_STATE);
			this.registerReceiver(receiver, filter);
			registered = true;
		}

		serviceIntent = new Intent(this, Control.class);
		startService(serviceIntent);

	}
	
	
	

}

