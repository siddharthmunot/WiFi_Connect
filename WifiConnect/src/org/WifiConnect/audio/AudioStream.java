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

import java.io.IOException;


public abstract class AudioStream {

	public void close() throws IOException {

	}

	public int getBufferDuration() {
		return 0;
	}

	public void missed(int duration, boolean missing) throws IOException {

	}

	public abstract int write(AudioBuffer buff)
			throws IOException;

	public int sampleDurationMs(AudioBuffer buff) {
		return -1;
	}
}