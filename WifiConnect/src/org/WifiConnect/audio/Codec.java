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

public abstract class Codec {
	public abstract void close();

	public abstract AudioBuffer encode(AudioBuffer source);

	public abstract AudioBuffer decode(AudioBuffer source);

	// how many samples are in this compressed buffer?
	public int sampleLength(AudioBuffer buff) {
		AudioBuffer out = decode(buff);
		try {
			return out.dataLen / 2;
		} finally {
			out.release();
		}
	}

	// if this codec can mask missing audio, generate some
	public AudioBuffer decode_missing(int duration) {
		return null;
	}
}
