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

package org.WifiConnect.audio;

import java.util.Stack;

public class BufferList {
	private Stack<AudioBuffer> reuseList = new Stack<AudioBuffer>();
	public final int mtu;
	// enough space for 16bit, 120ms @ 8KHz
	static final int DEFAULT_MTU = 2 * 120 * 8;

	public BufferList() {
		this(DEFAULT_MTU);
	}

	public BufferList(int mtu) {
		this.mtu = mtu;
	}

	public AudioBuffer getBuffer() {
		AudioBuffer buff = null;
		synchronized (reuseList) {
			if (reuseList.size() > 0)
				buff = reuseList.pop();
		}
		if (buff == null)
			buff = new AudioBuffer(this, mtu);
		else
			buff.clear();

		if (buff.inUse)
			throw new IllegalStateException();
		buff.inUse = true;
		return buff;
	}

	public void releaseBuffer(AudioBuffer buff) {
		if (!buff.inUse)
			throw new IllegalStateException();
		buff.inUse = false;
		synchronized (reuseList) {
			reuseList.push(buff);
		}
	}
}
