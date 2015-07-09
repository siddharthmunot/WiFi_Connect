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
import android.os.RemoteException;

import org.WifiConnect.servaldna.SubscriberId;

public interface IPeer {

	public SubscriberId getSubscriberId();

	public long getContactId();

	public void addContact(Context context) throws RemoteException,
			OperationApplicationException;

	public boolean hasName();

	public String getSortString();

	public String getDid();

	public boolean isReachable();
}
