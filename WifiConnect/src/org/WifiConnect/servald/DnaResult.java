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
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;

import org.WifiConnect.account.AccountService;
import org.WifiConnect.servaldna.AbstractId.InvalidHexException;
import org.WifiConnect.servaldna.ServalDCommand;
import org.WifiConnect.servaldna.SubscriberId;

public class DnaResult implements IPeer {
	public final Peer peer;
	public boolean local = true;
	public String ext;
	public String did;
	public String name;
	public final Uri uri;

	public DnaResult(ServalDCommand.LookupResult result) throws InvalidHexException {
		this.name = result.name;
		this.did = result.did;
		this.uri = Uri.parse(result.uri);
		if ("sid".equals(this.uri.getScheme())) {
			SubscriberId sid = new SubscriberId(this.uri.getHost());
			this.peer = PeerListService.getPeer(sid);

			for (String s : this.uri.getPathSegments()) {
				if (s.equals("local")) {
					this.local = true;
				} else if (s.equals("external")) {
					this.local = false;
				} else {
					this.ext = s;
				}
			}
		} else
			throw new IllegalArgumentException();

	}

	@Override
	public String toString() {
		if (name != null && !name.equals(""))
			return name;
		if (did != null && !did.equals(""))
			return did;
		return peer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof DnaResult))
			return false;
		DnaResult other = (DnaResult) o;
		return this.uri.equals(other.uri);
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}

	@Override
	public SubscriberId getSubscriberId() {
		return peer.getSubscriberId();
	}

	@Override
	public long getContactId() {
		// prevent adding an android contact for gateway services
		if (local)
			return peer.getContactId();
		else
			return Long.MAX_VALUE;
	}

	@Override
	public void addContact(Context context) throws RemoteException,
			OperationApplicationException {
		if (local && peer.contactId == -1) {
			peer.contactId = AccountService.addContact(
					context, name, getSubscriberId(),
					did);
		}
	}

	@Override
	public boolean hasName() {
		return name != null && !name.equals("");
	}

	@Override
	public String getSortString() {
		return hasName() ? name : peer.name +
				did +
				peer.sid;
	}

	@Override
	public String getDid() {
		return did;
	}

	@Override
	public boolean isReachable() {
		return peer.isReachable();
	}
}
