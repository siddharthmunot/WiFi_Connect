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

import org.WifiConnect.batphone.VoMP;

public class AudioBuffer implements Comparable<AudioBuffer> {
	public VoMP.Codec codec;
	public final byte buff[];
	public int dataLen;
	public int sampleStart;
	public int sequence;
	public long received;
	public int thisDelay;
	public final BufferList bufferList;
	public boolean inUse = false;

	AudioBuffer(BufferList list, int mtu) {
		this.bufferList = list;
		this.buff = new byte[mtu];
	}

	public void copyFrom(AudioBuffer other) {
		this.sampleStart = other.sampleStart;
		this.sequence = other.sequence;
		this.received = other.received;
		this.thisDelay = other.thisDelay;
	}

	public void release() {
		this.bufferList.releaseBuffer(this);
	}

	@Override
	public int compareTo(AudioBuffer arg0) {
		if (0 < arg0.sampleStart - this.sampleStart)
			return -1;
		else if (this.sampleStart == arg0.sampleStart)
			return 0;
		return 1;
	}

	public void clear() {
		this.dataLen = 0;
		this.codec = null;
	}
}