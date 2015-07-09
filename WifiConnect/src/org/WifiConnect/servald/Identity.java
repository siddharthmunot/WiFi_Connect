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

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import org.WifiConnect.ServalBatPhoneApplication;
import org.WifiConnect.servaldna.AbstractId.InvalidHexException;
import org.WifiConnect.servaldna.AsyncResult;
import org.WifiConnect.servaldna.ServalDCommand;
import org.WifiConnect.servaldna.ServalDFailureException;
import org.WifiConnect.servaldna.SubscriberId;

import java.util.ArrayList;
import java.util.List;

public class Identity {
	public final SubscriberId subscriberId;
	private String name;
	private String did;
	private boolean main;
	private static List<Identity> identities;

	public static Identity createIdentity() throws InvalidHexException, ServalDFailureException {
		ServalDCommand.IdentityResult result = ServalDCommand.keyringAdd();
		Identity id = new Identity(result.subscriberId);
		id.did = result.did;
		id.name = result.name;
		id.main = Identity.identities.size() == 0;
		Identity.identities.add(id);
		return id;
	}

	public static List<Identity> getIdentities() {
		if (identities == null) {
			identities = new ArrayList<Identity>();
			try {
				// TODO provide list of unlock PINs
				ServalDCommand.keyringList(new AsyncResult<ServalDCommand.IdentityResult>() {
					@Override
					public void result(ServalDCommand.IdentityResult nextResult) {
						Identity id = new Identity(nextResult.subscriberId);
						id.updateDetails(nextResult);
						id.main = identities.size() == 0;
						identities.add(id);
					}
				});
			}
			catch (ServalDFailureException e) {
				Log.e("Identities", e.toString(), e);
			}
		}
		return identities;
	}

	public static Identity getIdentity(SubscriberId sid){
		for(Identity i:getIdentities())
			if (i.subscriberId.equals(sid))
				return i;
		return null;
	}

	public static Identity getMainIdentity() {
		getIdentities();
		if (identities.size() < 1)
			return null;
		return identities.get(0);
	}

	private Identity(SubscriberId sid) {
		this.subscriberId = sid;
	}

	public String getName() {
		return name;
	}

	public String getDid() {
		return did;
	}

	private void updateDetails(ServalDCommand.IdentityResult result){
		this.did = result.did;
		this.name = result.name;
	}

	public void setDetails(Context context, String did, String name)
			throws ServalDFailureException {

		if (did == null || !did.matches("[0-9+*#]{5,31}"))
			throw new IllegalArgumentException(
					"The phone number must contain only 0-9+*# and be at least 5 characters in length");

		if (PhoneNumberUtils.isEmergencyNumber(did) || did.startsWith("11"))
			throw new IllegalArgumentException(
					"That number cannot be dialed as it will be redirected to a cellular emergency service.");
		updateDetails(ServalDCommand.keyringSetDidName(this.subscriberId, did == null ? "" : did, name == null ? "" : name));

		ServalBatPhoneApplication.context.server.restart();

		if (main) {
			Intent intent = new Intent("org.WifiConnect.SET_PRIMARY");
			intent.putExtra("did", this.did);
			intent.putExtra("sid", this.subscriberId.toHex());
			context.sendStickyBroadcast(intent);
		}
	}

	@Override
	public int hashCode() {
		return this.subscriberId.hashCode();
	}

	@Override
	public String toString() {
		return this.subscriberId.toString() + " " + did + " " + name;
	}

}
