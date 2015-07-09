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

import java.util.ArrayList;

import org.WifiConnect.PeerList;
import org.WifiConnect.servaldna.AbstractId;
import org.WifiConnect.servaldna.SubscriberId;
import org.WifiConnect.wizard.Wizard;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class AccountService extends Service {
	private static AccountAuthenticator authenticator=null;
	public static final String ACTION_ADD = "org.WifiConnect.account.add";
	public static final String TYPE = "org.WifiConnect.account";

	public static final String SID_FIELD_MIMETYPE = "vnd.android.cursor.item/org.WifiConnect.unsecuredSid";

	public static long getContactId(ContentResolver resolver,
			SubscriberId sid) {
		Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
				new String[] {
					ContactsContract.Data.CONTACT_ID
				},
				"UPPER(" + ContactsContract.Data.DATA1 + ") = ? AND "
						+ ContactsContract.Data.MIMETYPE + " = ?",
				new String[] {
						sid.toHex(), SID_FIELD_MIMETYPE
				}, null);
		try {
			if (!cursor.moveToNext()) {
				return -1;
			}

			return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	public static long getContactId(ContentResolver resolver,
			String did) {
		Cursor cursor = resolver
				.query(ContactsContract.Data.CONTENT_URI,
						new String[] {
							ContactsContract.Data.CONTACT_ID
						},
						ContactsContract.CommonDataKinds.Phone.NUMBER
								+ " = ? AND " + ContactsContract.Data.MIMETYPE
								+ " = ?",
						new String[] {
								did,
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
						},
						null);

		try {
			if (!cursor.moveToNext())
				return -1;
			return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	public static String getContactName(ContentResolver resolver,
			long contactId) {
		Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
				new String[] { ContactsContract.Contacts.DISPLAY_NAME },
				"_ID = ?", new String[] { Long.toString(contactId) }, null);

		try {
			if (!cursor.moveToNext()) {
				Log.w("org.WifiConnect", "Could not find contact name for "
						+ contactId);
				return null;
			}

			return cursor.getString(0);
		} finally {
			cursor.close();
		}
	}

	public static SubscriberId getContactSid(ContentResolver resolver, Uri uri) throws AbstractId.InvalidHexException {
		if (uri==null)
			return null;
		// TODO patern match Uri?
		long contactId=-1;
		Cursor cursor = resolver.query(
				uri,
				new String[]{
						ContactsContract.Data.RAW_CONTACT_ID
				},
				null,
				null,
				null);
		try {
			if (cursor.moveToNext())
				contactId = cursor.getLong(0);
		} finally {
			cursor.close();
		}
		if (contactId==-1)
			return null;
		return getContactSid(resolver, contactId);
	}

	public static SubscriberId getContactSid(ContentResolver resolver,
			long contactId) {
		Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
				new String[] { ContactsContract.Data.DATA1 },
				ContactsContract.Data.CONTACT_ID + " = ? AND "
						+ ContactsContract.Data.MIMETYPE + " = ?",
				new String[] { Long.toString(contactId), SID_FIELD_MIMETYPE },
				null);
		try {
			if (cursor.moveToNext())
				return new SubscriberId(cursor.getString(0));
		}
		catch (SubscriberId.InvalidHexException e) {
			Log.e("org.WifiConnect", "Invalid SID", e);
		}
		finally {
			cursor.close();
		}
		return null;
	}

	public static Account getAccount(Context context) {
		AccountManager manager = AccountManager.get(context);
		Account[] accounts = manager.getAccountsByType(AccountService.TYPE);
		if (accounts == null || accounts.length == 0)
			return null;
		return accounts[0];
	}

	public static long addContact(Context context, String name,
			SubscriberId sid, String did) throws RemoteException,
			OperationApplicationException {
		ContentResolver resolver = context.getContentResolver();
		Account account = getAccount(context);
		if (account == null)
			throw new IllegalStateException();
		return addContact(resolver, account, name, sid, did);
	}

	public static long addContact(ContentResolver resolver, Account account,
			String name, SubscriberId sid, String did) throws RemoteException,
			OperationApplicationException {
		Log.i("org.WifiConnect", "Adding contact: " + name);
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

		// Create our RawContact
		ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(RawContacts.CONTENT_URI);
		builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
		builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
		builder.withValue(RawContacts.VERSION, 1);
		operationList.add(builder.build());

		// Create a Data record of common type 'StructuredName' for our
		// RawContact
		builder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(
				ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID,
				0);
		builder.withValue(
				ContactsContract.Data.MIMETYPE,
				ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
		if (name != null && !name.equals(""))
			builder.withValue(
					ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
					name);
		operationList.add(builder.build());

		// Create a Data record for the subscriber id
		builder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE, SID_FIELD_MIMETYPE);
		builder.withValue(ContactsContract.Data.DATA1, sid.toHex());
		builder.withValue(ContactsContract.Data.DATA2, "Call Mesh");
		builder.withValue(ContactsContract.Data.DATA3, sid.abbreviation());
		operationList.add(builder.build());

		// Create a Data record for their phone number
		builder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE,
				ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
		builder.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, did);
		builder.withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
				ContactsContract.CommonDataKinds.Phone.TYPE_MAIN);
		operationList.add(builder.build());

		resolver.applyBatch(
				ContactsContract.AUTHORITY,
				operationList);

		return getContactId(resolver, sid);
	}

	private class AccountAuthenticator extends AbstractAccountAuthenticator {
		Context context;

		public AccountAuthenticator(Context context) {
			super(context);
			this.context = context;
		}

		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,
				String accountType, String authTokenType,
				String[] requiredFeatures, Bundle options)
				throws NetworkErrorException {

			Intent intent = new Intent(context, Wizard.class);
			intent.setAction(ACTION_ADD);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
					response);
			Bundle reply = new Bundle();
			reply.putParcelable(AccountManager.KEY_INTENT, intent);
			return reply;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response,
				Account account, Bundle options) throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response,
				String accountType) {
			Intent intent = new Intent(context, PeerList.class);
			Bundle reply = new Bundle();
			reply.putParcelable(AccountManager.KEY_INTENT, intent);
			return reply;
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response,
				Account account, String[] features)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT)){
			if (authenticator==null)
				authenticator = new AccountAuthenticator(this);
			return authenticator.getIBinder();
		}
		return null;
	}

}
