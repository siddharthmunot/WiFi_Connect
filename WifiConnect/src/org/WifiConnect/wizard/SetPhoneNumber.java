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


/**
 * 
 *  Wizard: Set Phone Number And Name.
 *  Used for initial run and is called again to reset phone number.
 **/
package org.WifiConnect.wizard;

import java.util.List;

import org.WifiConnect.PeerList;
import org.WifiConnect.R;
import org.WifiConnect.ServalBatPhoneApplication;
import org.WifiConnect.ServalBatPhoneApplication.State;
import org.WifiConnect.account.AccountService;
import org.WifiConnect.servald.Identity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SetPhoneNumber extends Activity {
	ServalBatPhoneApplication app;

	EditText number;
	// EditText name;
	String name = null;
	TextView sid;
	Button button;
	ProgressBar progress;
	Identity identity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app=(ServalBatPhoneApplication)this.getApplication();

		setContentView(R.layout.set_phone_no);
		number = (EditText)this.findViewById(R.id.batphoneNumberText);
		number.setSelectAllOnFocus(true);

		button = (Button) this.findViewById(R.id.btnPhOk);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				button.setEnabled(false);

				new AsyncTask<Void, Void, Boolean>() {

					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							identity.setDetails(app,
									number.getText().toString(),
									name);

							// create the serval android acount if it doesn't
							// already exist
							Account account = AccountService
									.getAccount(SetPhoneNumber.this);
							if (account == null) {
								account = new Account(getString(R.string.app_name),
										AccountService.TYPE);
								AccountManager am = AccountManager
										.get(SetPhoneNumber.this);

								if (!am.addAccountExplicitly(account, "", null))
									throw new IllegalStateException(
											"Failed to create account");

								Intent ourIntent = SetPhoneNumber.this
										.getIntent();
								if (ourIntent != null
										&& ourIntent.getExtras() != null) {
									AccountAuthenticatorResponse response = ourIntent
											.getExtras()
											.getParcelable(
													AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
									if (response != null) {
										Bundle result = new Bundle();
										result.putString(
												AccountManager.KEY_ACCOUNT_NAME,
												account.name);
										result.putString(
												AccountManager.KEY_ACCOUNT_TYPE,
												AccountService.TYPE);
										response.onResult(result);
									}
								}
							}
							return true;
						} catch (IllegalArgumentException e) {
							app.displayToastMessage(e.getMessage());
						} catch (Exception e) {
							Log.e("org.WifiConnect", e.getMessage(), e);
							app.displayToastMessage(e.getMessage());
						}
						return false;
					}

					@Override
					protected void onPostExecute(Boolean result) {
						if (result) {
							app.mainIdentityUpdated(identity);
							Intent intent = new Intent(SetPhoneNumber.this,
									PeerList.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							SetPhoneNumber.this.startActivity(intent);
							SetPhoneNumber.this.setResult(RESULT_OK);
							SetPhoneNumber.this.finish();
							return;
						}
						button.setEnabled(true);
					}
				}.execute((Void[]) null);
			}
		});

	}

	BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int stateOrd = intent.getIntExtra(
					ServalBatPhoneApplication.EXTRA_STATE, 0);
			State state = State.values()[stateOrd];
			stateChanged(state);
		}
	};
	boolean registered = false;

	private void stateChanged(State state) {
		// TODO update display of On/Off button
		switch (state) {
		case Installing:
		case Upgrading:
			button.setVisibility(View.VISIBLE);
			break;
		default:
			button.setVisibility(View.VISIBLE);
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter();
		filter.addAction(ServalBatPhoneApplication.ACTION_STATE);
		this.registerReceiver(receiver, filter);
		registered = true;
		stateChanged(app.getState());

		String existingName = null;
		String existingNumber = null;
		String sidAbbrev = null;

		List<Identity> identities = Identity.getIdentities();

		if (identities.size() > 0) {
			identity = identities.get(0);

			existingNumber = identity.getDid();
			sidAbbrev = identity.subscriberId.abbreviation();
			System.out.println("Existing Number" + existingNumber);
		} else {
			// try to get number from phone, probably wont work though...
			TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			existingNumber = mTelephonyMgr.getLine1Number();
			System.out.println("Existing Number telephony" + existingNumber);

			try {
				identity = Identity.createIdentity();
				sidAbbrev = identity.subscriberId.abbreviation();
			} catch (Exception e) {
				Log.e("SetPhoneNumber", e.getMessage(), e);
				app.displayToastMessage(e.getMessage());
			}
		}

		number.setText(existingNumber);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (registered)
			this.unregisterReceiver(receiver);
	}
}
