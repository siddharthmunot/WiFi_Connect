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

package org.WifiConnect.servald;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.WifiConnect.R;
import org.WifiConnect.ServalBatPhoneApplication;
import org.WifiConnect.account.AccountService;
import org.WifiConnect.servaldna.AsyncResult;
import org.WifiConnect.servaldna.MdpDnaLookup;
import org.WifiConnect.servaldna.ServalDCommand;
import org.WifiConnect.servaldna.ServalDFailureException;
import org.WifiConnect.servaldna.ServalDInterfaceException;
import org.WifiConnect.servaldna.SubscriberId;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;

/**
 *
 * @author brendon
 *
 *         Service responsible for periodically fetching peer list from ServalD.
 *         Activities and other components can register listeners to receive new
 *         peer updates.
 *
 */
public class PeerListService {
	private PeerListService() {

	}

	public static ConcurrentMap<SubscriberId, Peer> peers = new ConcurrentHashMap<SubscriberId, Peer>();
	private static final String TAG="PeerListService";
	static String contact_name;

	public static Peer getPeer(SubscriberId sid) {
		boolean changed = false;

		Peer p = peers.get(sid);
		if (p == null) {
			p = new Peer(sid);
			peers.put(sid, p);
			changed = true;
		}

		if (checkContacts(p))
			changed = true;

		if (p.cacheUntil < SystemClock.elapsedRealtime())
			resolve(p);

		if (changed)
			notifyListeners(p);

		return p;
	}

	private static boolean checkContacts(Peer p) {
		ContentResolver resolver = ServalBatPhoneApplication.context.getContentResolver();

		long contactId = AccountService.getContactId(
				resolver, p.sid);

		boolean changed = false;
		String contactName = null;
		// String contactName;

		if (contactId >= 0) {
			contactName = AccountService
					.getContactName(
							resolver,
							contactId);
		}

		if (p.contactId != contactId) {
			changed = true;
			p.contactId = contactId;
		}

		if (!(p.contactName == null ? "" : p.contactName)
				.equals(contactName == null ? "" : contactName)) {
			//System.out.println("contactName1: " + p.contactName);
			Log.i(TAG, "contactName:" + p.contactName);
			changed = true;
			// p.setContactName(contactName);
		}
		p.cacheContactUntil = SystemClock.elapsedRealtime()+60000;
		return changed;
	}

	static final int CACHE_TIME = 60000;
	private static List<IPeerListListener> listeners = new ArrayList<IPeerListListener>();

	public static void resolve(final Peer p){
		if (!p.isReachable())
			return;
		if (p.nextRequest > SystemClock.elapsedRealtime())
			return;

		if (ServalBatPhoneApplication.context.isMainThread()){
			ServalBatPhoneApplication.context.runOnBackgroundThread(new Runnable() {
				@Override
				public void run() {
					resolve(p);
				}
			});
			return;
		}

		Log.v(TAG, "Attempting to fetch details for " + p.getSubscriberId().abbreviation());
		try {
			if (lookupSocket==null){
				lookupSocket = ServalBatPhoneApplication.context.server.getMdpDnaLookup(
						new AsyncResult<ServalDCommand.LookupResult>() {
							@Override
							public void result(ServalDCommand.LookupResult nextResult) {
								Log.v(TAG, "Resolved; "+nextResult.toString());
								boolean changed = false;

								Peer p = peers.get(nextResult.subscriberId);
								if (p==null){
									p = new Peer(nextResult.subscriberId);
									peers.put(nextResult.subscriberId, p);
									changed = true;
								}

								if (!nextResult.did.equals(p.did)) {
									p.did = nextResult.did;
									changed = true;
								}
								Log.i(TAG, "DID: " + p.did);
								if (!p.hasName()) {
									contact_name = displayName(p.getDid());
									Log.i(TAG, "contactName : " + contact_name);
									p.setContactName(contact_name);
								}

								if (!nextResult.name.equals(p.name)) {
									p.name = contact_name;
									changed = true;
								}

								if (p.cacheContactUntil < SystemClock.elapsedRealtime()){
									if (checkContacts(p))
										changed = true;
								}

								p.cacheUntil = SystemClock.elapsedRealtime() + CACHE_TIME;
								if (changed)
									notifyListeners(p);
							}
						});
			}

			lookupSocket.sendRequest(p.getSubscriberId(), "");
			// only allow one request per second
			p.nextRequest = SystemClock.elapsedRealtime()+1000;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (ServalDInterfaceException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private static void closeSocket(){
		if (ServalBatPhoneApplication.context.isMainThread()){
			ServalBatPhoneApplication.context.runOnBackgroundThread(new Runnable() {
				@Override
				public void run() {
					closeSocket();
				}
			});
			return;
		}
		if (lookupSocket!=null){
			lookupSocket.close();
			lookupSocket = null;
		}
	}

	public static void addListener(IPeerListListener callback) {
		listeners.add(callback);
		// send the peers that may already have been found. This may result
		// in the listener receiving a peer multiple times
		for (Peer p : peers.values()) {
			boolean changed = false;

			if (p.cacheContactUntil < SystemClock.elapsedRealtime()){
				if (checkContacts(p))
					changed = true;
			}

			if (changed)
				notifyListeners(p);
			else
				callback.peerChanged(p);

			if (p.cacheUntil < SystemClock.elapsedRealtime())
				resolve(p);
		}
	}

	public static void removeListener(IPeerListListener callback) {
		listeners.remove(callback);
	}

	public static void notifyListeners(Peer p) {
		for (IPeerListListener l : listeners) {
			l.peerChanged(p);
		}
	}

	private static void clear(){
		for (Peer p:peers.values()){
			if (p.isReachable()){
				p.linkChanged(null, -1);
				notifyListeners(p);
			}
		}
		peers.clear();
	}

	private static boolean interfaceUp = false;
	private static int lastPeerCount=-1;

	private static void updatePeerCount(){
		try {
			ServalBatPhoneApplication app = ServalBatPhoneApplication.context;
			int peerCount = 0;
			if (interfaceUp) {
				peerCount = ServalDCommand.peerCount();
				if (peerCount == 0) {
					app.server.updateStatus(R.string.server_nopeers);
				} else {
					app.server.updateStatus(app.getResources().getQuantityString(R.plurals.peers_label, peerCount, peerCount));
				}
			}else{
				app.server.updateStatus(R.string.server_idle);
			}
			if (app.controlService != null)
				app.controlService.updatePeerCount(peerCount);
			lastPeerCount = peerCount;
		} catch (ServalDFailureException e) {
			e.printStackTrace();
		}

	}

	private static MdpDnaLookup lookupSocket = null;
	public static void registerMessageHandlers(ServalDMonitor monitor){
		ServalDMonitor.Messages handler = new ServalDMonitor.Messages(){
			@Override
			public void onConnect(ServalDMonitor monitor) {
				try {
					interfaceUp = false;
					updatePeerCount();
					// TODO move to ServalD??
					// re-init mdp binding
					if (lookupSocket!=null){
						try{
							lookupSocket.rebind();
						}catch(IOException e){
							lookupSocket = null;
							Log.e(TAG, e.getMessage(), e);
						}
					}
					monitor.sendMessage("monitor interface");
					monitor.sendMessage("monitor links");
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}

			@Override
			public void onDisconnect(ServalDMonitor monitor) {
				closeSocket();
				clear();
			}

			@Override
			public int message(String cmd, Iterator<String> iArgs, InputStream in, int dataLength) throws IOException {
				ServalBatPhoneApplication app = ServalBatPhoneApplication.context;

				if(cmd.equalsIgnoreCase("LINK")) {
					try{
						int hop_count = ServalDMonitor.parseInt(iArgs.next());
						String sid = iArgs.next();
						SubscriberId transmitter = sid.equals("") ? null : new SubscriberId(sid);
						SubscriberId receiver = new SubscriberId(iArgs.next());

						Log.v(TAG, "Link; " + receiver.abbreviation() + " " + (transmitter == null ? "" : transmitter.abbreviation()) + " " + hop_count);
						boolean changed = false;

						Peer p = peers.get(receiver);
						if (p == null) {
							p = new Peer(receiver);
							peers.put(receiver, p);
							changed = true;
						}

						if (p.cacheContactUntil < SystemClock.elapsedRealtime()) {
							if (checkContacts(p))
								changed = true;
						}

						boolean wasReachable = p.getTransmitter()!=null;
						boolean isReachable = transmitter!=null;

						if (p.linkChanged(transmitter, hop_count))
							changed = true;

						if (wasReachable!=isReachable)
							updatePeerCount();

						if (changed)
							notifyListeners(p);

						if (transmitter!=null && p.cacheUntil < SystemClock.elapsedRealtime())
							resolve(p);
					} catch (SubscriberId.InvalidHexException e) {
						IOException t = new IOException(e.getMessage());
						t.initCause(e);
						throw t;
					}
				}else if(cmd.equalsIgnoreCase("INTERFACE")){
					iArgs.next(); // name
					String state = iArgs.next();
					// TODO track all interfaces by name?
					interfaceUp = state.equals("UP");
					updatePeerCount();
				}
				return 0;
			}
		};
		monitor.addHandler("LINK", handler);
		monitor.addHandler("INTERFACE", handler);
	}

	public static boolean havePeers() {
		return lastPeerCount>0;
	}

	/*
	 * Display the Name against the phone Number; if not fond print not in
	 * Contact
	 */
	protected static String displayName(String phonenumber) {
		String name = null;
		String contact_Id = null;
		// InputStream input = null;

		// define the columns I want the query to return
		String[] projection = new String[] {
				ContactsContract.PhoneLookup.DISPLAY_NAME,
				ContactsContract.PhoneLookup._ID
		};
		Log.i(TAG, "phoneNumber: " + phonenumber);

		// encode the phone number and build the filter URI
		Uri contactUri = Uri.withAppendedPath(
				ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(phonenumber));
		try {

			// query time
			Cursor cursor = ServalBatPhoneApplication.context
					.getContentResolver().query(
					contactUri, projection,
					null, null, null);
			if (cursor == null) {
				return null;
			}

			if (cursor.moveToFirst()) {

				// Get values from contacts database:
				contact_Id = cursor.getString(cursor
						.getColumnIndex(ContactsContract.PhoneLookup._ID));
				name = cursor
						.getString(cursor
								.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));

				Log.v("ffnet", "Started uploadcontactphoto: Contact Found @ "
						+ phonenumber);
				Log.v("ffnet", "Started uploadcontactphoto: Contact name = "
						+ name);
				Log.v("ffnet", "Started uploadcontactphoto: Contact id = "
						+ contact_Id);

			} else {

				Log.v("ffnet",
						"Started uploadcontactphoto: Contact Not Found @ "
								+ phonenumber);
				name = "Contact Not Found";
				cursor.close();
				return null;

			}
			cursor.close();
		} catch (Exception e) {
			return null;
		}
		return name;

	}
}
