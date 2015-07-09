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

package org.WifiConnect.rhizome;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.WifiConnect.R;
import org.WifiConnect.ServalBatPhoneApplication;
//import org.WifiConnect.messages.MessagesListActivity;
import org.WifiConnect.messages.ShowConversationActivity;
import org.WifiConnect.servald.Identity;
import org.WifiConnect.servald.ServalD;
import org.WifiConnect.servaldna.SubscriberId;
import org.WifiConnect.servaldna.meshms.MeshMSConversation;
import org.WifiConnect.servaldna.meshms.MeshMSConversationList;

public class MeshMS {
	private final ServalBatPhoneApplication app;
	private final Identity identity;
	private static final String TAG="MeshMS";
	public static final String NEW_MESSAGES="org.WifiConnect.meshms.NEW";

	public MeshMS(ServalBatPhoneApplication app, Identity identity){
		this.app=app;
		this.identity=identity;
	}

	public void bundleArrived(RhizomeManifest_MeshMS meshms){
		initialiseNotification();
		app.sendBroadcast(new Intent(NEW_MESSAGES));
	}

	public void markRead(SubscriberId recipient){
		try {
			app.server.getRestfulClient().meshmsMarkAllMessagesRead(identity.subscriberId, recipient);
		} catch (Exception e) {
			app.displayToastMessage(e.getMessage());
			Log.e(TAG, e.getMessage(), e);
		}
		cancelNotification();
		initialiseNotification();
	}

	private int lastMessageHash =0;

	// build an initial notification on startup
	public void initialiseNotification() {
		if (app.isMainThread()){
			 app.runOnBackgroundThread(new Runnable() {
			 @Override
			 public void run() {
			 initialiseNotification();
			 }
			 });
			 return;
			 }
		if (!ServalD.isRhizomeEnabled())
			return;

		SubscriberId recipient=null;
		boolean unread=false;
		int messageHash=0;
		try {
			MeshMSConversationList conversations = app.server.getRestfulClient().meshmsListConversations(identity.subscriberId);
			MeshMSConversation conv;
			while ((conv = conversations.nextConversation()) != null) {
				// detect when the number of incoming messages has changed
				if (conv.isRead)
					continue;

				messageHash =
						conv.theirSid.hashCode() ^
								(int) conv.lastMessageOffset ^
								(int) (conv.lastMessageOffset >> 32);

				Log.v(TAG, conv.theirSid.abbreviation()+", lastOffset = "+conv.lastMessageOffset+", hash = "+messageHash+", read = "+conv.isRead);

				// remember the recipient, if it is the only recipient with unread messages
				if (unread) {
					recipient = null;
				} else {
					recipient = conv.theirSid;
				}
				unread = true;
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}

		Log.v(TAG, "unread = "+unread+", hash = "+messageHash+", lastHash = "+lastMessageHash);
		if (!unread){
			cancelNotification();
			return;
		}
		if (messageHash == lastMessageHash)
			return;
		lastMessageHash = messageHash;
		// For now, just indicate that there are some unread messages
		String content = "Unread message(s)";
		Intent intent = new Intent(Intent.ACTION_MAIN);

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setClass(app, ShowConversationActivity.class);
		intent.putExtra("recipient", recipient.toHex().toUpperCase());
		PendingIntent pendingIntent = PendingIntent.getActivity(app, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification n = new Notification(R.drawable.ic_launcher,
				content,
				System.currentTimeMillis());

		n.setLatestEventInfo(app,
				"", content, pendingIntent);
		n.defaults |= Notification.DEFAULT_VIBRATE
				| Notification.DEFAULT_LIGHTS;

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(app);
		String sound = settings.getString("meshms_notification_sound",
				null);
		if (sound==null)
		if (sound == null) {
			n.defaults |= Notification.DEFAULT_SOUND;
		} else {
			n.sound = Uri.parse(sound);
		}

		n.flags |= Notification.FLAG_SHOW_LIGHTS
				| Notification.FLAG_AUTO_CANCEL;

		NotificationManager nm = (NotificationManager) app
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify("meshms", ServalBatPhoneApplication.NOTIFY_MESSAGES, n);
	}

	public void cancelNotification() {
		NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel("meshms", ServalBatPhoneApplication.NOTIFY_MESSAGES);
	}
}
