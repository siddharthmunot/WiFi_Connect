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

import org.WifiConnect.servald.IPeerListListener;

/**
 * Provides an interface for adding and removing listeners to the
 * PeerListService. The onBind() method in PeerListService returns an object
 * that implements this interface which allows interested classes to add and
 * remove their listener.
 *
 * @author brendon
 *
 */
public interface IPeerListMonitor {

	public void registerListener(IPeerListListener listener);

	public void removeListener(IPeerListListener listener);

}
