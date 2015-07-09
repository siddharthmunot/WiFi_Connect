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


package uk.co.mmscomputing.sound;

public abstract class Compressor {
	protected abstract int compress(short sample);

	public final void compress(byte in[], int offset, int len, byte out[],
			int outOffset) {
		int i = 0;
		while (i < len) {
			int sample = (in[offset + i++] & 0x00FF);
			sample |= (in[offset + i++] << 8);
			out[outOffset++] = (byte) compress((short) sample);
		}
	}
}