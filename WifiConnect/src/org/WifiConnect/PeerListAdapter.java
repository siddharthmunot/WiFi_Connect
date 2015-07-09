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

/**
 *
 *         Peer List fetches a list of known peers from the PeerListService.
 *         When a peer is received from the service this activity will attempt
 *         to resolve the peer by calling ServalD in an async task.
 */

import java.util.ArrayList;
import java.util.List;

import org.WifiConnect.messages.ShowConversationActivity;
import org.WifiConnect.servald.IPeer;
import org.WifiConnect.servald.ServalD;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;



public class PeerListAdapter<T extends IPeer> extends ArrayAdapter<T> {
	public PeerListAdapter(Context context, List<T> peers) {
		super(context, R.layout.peer, R.id.Name, peers);
	}

	private static final String TAG = "PeerList";
	String contact_name;
	boolean a = true;
	List<String> textArray = new ArrayList<String>();
	int sdk = android.os.Build.VERSION.SDK_INT;

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View ret = super.getView(position, convertView, parent);
		T p = this.getItem(position);

		TextView displayName = (TextView) ret.findViewById(R.id.Name);
		TextView displayNumber = (TextView) ret.findViewById(R.id.Number);
		View chat = ret.findViewById(R.id.chat);
		View call = ret.findViewById(R.id.call);
		View share = ret.findViewById(R.id.share_msg);
		displayNumber.setText(p.getDid());


		if (p.getSubscriberId().isBroadcast()) {
			call.setVisibility(View.INVISIBLE);
		} else {
			call.setVisibility(View.VISIBLE);
		}

		 if (PeerList.PASTETEXT == null) {
		 share.setVisibility(View.INVISIBLE);
		 }
		 else
		 {
		 Log.i(TAG, "PASTE:" + PeerList.PASTETEXT);
		 share.setVisibility(View.VISIBLE);
		 }


		if (p.isReachable()){
			displayName.setTextColor(Color.parseColor("#859554"));
			displayNumber.setTextColor(Color.parseColor("#ffcc7948"));
			
		}else{
			displayName.setTextColor(Color.GRAY);
			displayNumber.setTextColor(Color.GRAY);
			
		}

		chat.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ServalBatPhoneApplication app = ServalBatPhoneApplication.context;

				T p = getItem(position);

				if (!ServalD.isRhizomeEnabled()) {
					app.displayToastMessage("Messaging cannot function without an sdcard");
					return;
				}

				// Send MeshMS by SID
				Intent intent = new Intent(
						app, ShowConversationActivity.class);
				intent.putExtra("recipient", p.getSubscriberId().toHex());
				getContext().startActivity(intent);
			}
		});


		
		  share.setOnClickListener(new OnClickListener() {
		  
		  @Override 
		  public void onClick(View v) { 
			  ServalBatPhoneApplication app = ServalBatPhoneApplication.context; 
			  T p = getItem(position); 
			  Log.i(TAG, "Clicked: " + p);
		 				 
	     String link = "Click <a href =\""+PeerList.PASTETEXT+"\">here</a> to access Files" ;
	     Log.i(TAG, "Link:" + link);
		 Intent intent1 = new Intent( app, ShowConversationActivity.class);
		 intent1.putExtra("recipient", p.getSubscriberId().toHex());
		 intent1.putExtra("msg", link);
		 
		 getContext().startActivity(intent1);
		 app.displayToastMessage("Link Shared with the Peer." +
		 		" Recommend to Click Stop Sharing from Menu post data download.");
		  
		 }
	 });
		return ret;
	}
}
