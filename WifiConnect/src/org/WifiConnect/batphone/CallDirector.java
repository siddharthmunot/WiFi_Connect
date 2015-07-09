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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.WifiConnect.PeerListAdapter;
import org.WifiConnect.R;
import org.WifiConnect.ServalBatPhoneApplication;
import org.WifiConnect.servald.DnaResult;
import org.WifiConnect.servald.IPeerListListener;
import org.WifiConnect.servald.Peer;
import org.WifiConnect.servald.PeerListService;
import org.WifiConnect.servaldna.AbstractId;
import org.WifiConnect.servaldna.AsyncResult;
import org.WifiConnect.servaldna.MdpDnaLookup;
import org.WifiConnect.servaldna.ServalDCommand;
import org.WifiConnect.servaldna.ServalDInterfaceException;
import org.WifiConnect.servaldna.SubscriberId;

import java.io.IOException;
import java.util.ArrayList;

public class CallDirector extends ListActivity implements OnClickListener, IPeerListListener {

	private ServalBatPhoneApplication app;
	private String last_number;
	private PeerListAdapter<DnaResult> adapter;
	private Button call;
	private Button cancel;
	private Button search;
	private TextView phone_number;
	private static final String TAG = "CallDirector";
	private MdpDnaLookup searchSocket = null;
	private Handler handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = (ServalBatPhoneApplication)getApplication();
		handler = new Handler();
		this.setContentView(R.layout.call_director);
		call = (Button) this.findViewById(R.id.call);
		call.setOnClickListener(this);

		cancel = (Button) this.findViewById(R.id.cancel);
		cancel.setOnClickListener(this);

		search = (Button) this.findViewById(R.id.search);
		search.setOnClickListener(this);

		phone_number = (TextView) this.findViewById(R.id.phone_number);

		Intent intent = this.getIntent();

		String dialed_number = intent.getStringExtra("phone_number");
		phone_number.setText(dialed_number);
		if (dialed_number==null || dialed_number.equals("")){
			call.setVisibility(View.GONE);
			phone_number.setVisibility(View.VISIBLE);
		}else{
			call.setVisibility(View.VISIBLE);
			phone_number.setVisibility(View.GONE);
		}
		adapter = new PeerListAdapter<DnaResult>(this, new ArrayList<DnaResult>());
		setListAdapter(adapter);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		   case R.id.call:
				BatPhone.call(phone_number.getText().toString());
				closeNow();
				break;
		   case R.id.cancel:
				closeNow();
				break;
		   case R.id.search:
				searchMesh(false);
				break;
		}
	}

	private void closeNow(){
		if (phone_number.getVisibility() == View.VISIBLE) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(phone_number.getWindowToken(), 0);
		}
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		searchMesh(true);
		PeerListService.addListener(this);
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (!isFinishing())
			finish();
	}

	@Override
	protected void onPause() {
		super.onPause();
		PeerListService.removeListener(this);
		handler.removeCallbacks(searcher);
		search_count=0;
		closeSocket();
	}

	private void closeSocket(){
		final MdpDnaLookup sock = searchSocket;
		searchSocket = null;
		
		if (sock != null){
			app.runOnBackgroundThread(new Runnable(){
				@Override
				public void run() {
					sock.close();
				}
			});
		}
	}

	private int search_count=0;
	private Runnable searcher=new Runnable() {
		@Override
		public void run() {
			if (app.isMainThread())
				app.runOnBackgroundThread(this);
			else
				search();
		}
	};

	private void search(){
		try {
			handler.removeCallbacks(searcher);

			if (searchSocket == null){
				searchSocket = app.server.getMdpDnaLookup(new AsyncResult<ServalDCommand.LookupResult>() {
					@Override
					public void result(ServalDCommand.LookupResult nextResult) {
						try {
							final DnaResult result = new DnaResult(nextResult);
							handler.post(new Runnable() {
								@Override
								public void run() {
									if (adapter.getPosition(result) < 0) {
										adapter.add(result);
										adapter.notifyDataSetChanged();
									}
								}
							});
						} catch (AbstractId.InvalidHexException e) {
							Log.e(TAG, e.getMessage(), e);
						}
					}
				});
			}
			searchSocket.sendRequest(SubscriberId.broadcastSid, last_number);
			if (--search_count<=0)
				return;
			handler.postDelayed(searcher, 1000);
		}catch (IOException e){
			Log.e(TAG, e.getMessage(), e);
			app.displayToastMessage(e.getMessage());
		} catch (ServalDInterfaceException e) {
			Log.e(TAG, e.getMessage(), e);
			app.displayToastMessage(e.getMessage());
		}
	}

	private void searchMesh(boolean onResume) {
		final String phone = phone_number.getText().toString();
		if (onResume && (phone==null || phone.equals("")))
			return;

		if (!phone.equals(last_number)){
			last_number = phone;
			adapter.clear();
			adapter.notifyDataSetChanged();
			closeSocket();
		}
		search_count=5;
		app.runOnBackgroundThread(searcher);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		try {
			CallHandler.dial(adapter.getItem(position));
			closeNow();
		} catch (Exception e) {
			app.displayToastMessage(e
					.getMessage());
			Log.e(TAG, e.getMessage(), e);
		}
	}

	@Override
	public void peerChanged(final Peer p) {
		if (!app.isMainThread()){
			handler.post(new Runnable() {
				@Override
				public void run() {
					peerChanged(p);
				}
			});
			return;
		}
		adapter.notifyDataSetChanged();
	}
}
