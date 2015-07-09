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

import java.util.Comparator;


/**
 * A comparator for a Peer. Sort on peer.sortString. If the peer has a name,
 * sort it above peers that don't so that numbers appear below names.
 *
 * @author brendon
 * @author jeremy
 */
public class PeerComparator implements Comparator<IPeer> {

	@Override
	public int compare(IPeer p1, IPeer p2) {
		boolean hasName1 = p1.hasName();
		boolean hasName2 = p2.hasName();

		if (hasName1 && !hasName2)
			return -1;
		if (!hasName1 && hasName2)
			return 1;

		String s1 = p1.getSortString();
		String s2 = p2.getSortString();

		return s1.compareTo(s2);
	}
}
