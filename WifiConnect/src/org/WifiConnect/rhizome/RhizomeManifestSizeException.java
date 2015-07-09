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

package org.WifiConnect.rhizome;

import java.io.File;

/**
 * Thrown when a Rhizome manifest is too long to fit in a limited-size byte stream.
 *
 * @author Andrew Bettison <andrew@org.WifiConnect.com>
 */
public class RhizomeManifestSizeException extends Exception {
	private static final long serialVersionUID = 1L;
	private long mSize;
	private long mMaxSize;

	public RhizomeManifestSizeException(String message, long size, long maxSize) {
		super(message + " (" + size + "bytes exceeds " + maxSize + ")");
		mSize = size;
		mMaxSize = maxSize;
	}

	public RhizomeManifestSizeException(File manifestFile, long maxSize) {
		this(manifestFile.toString(), manifestFile.length(), maxSize);
	}

}
