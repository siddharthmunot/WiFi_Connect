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

/**
 * Represents a Rhizome manifest, with methods to serialise to/from a byte stream for storage on
 * disk.
 *
 * @author Andrew Bettison <andrew@org.WifiConnect.com>
 */
public class RhizomeManifestParseException extends Exception {
	private static final long serialVersionUID = 1L;
	private int mOffset;

	/**
	 * Construct an exception which does not correspond to a particular place in the parsed stream.
	 */
	public RhizomeManifestParseException(String message) {
		super(message);
		mOffset = -1;
	}

	/**
	 * Construct an exception which does not correspond to a particular place in the parsed stream,
	 * specifying the cause.
	 */
	public RhizomeManifestParseException(String message, Throwable cause) {
		super(message, cause);
		mOffset = -1;
	}

	/**
	 * Construct an exception that identifies the position in the parsed stream that provoked it.
	 */
	public RhizomeManifestParseException(String message, int offset) {
		super(message);
		mOffset = offset;
	}

	/**
	 * Construct an exception that identifies the position in the parsed stream that provoked it
	 * and the specified cause.
	 */
	public RhizomeManifestParseException(String message, int offset, Throwable cause) {
		super(message, cause);
		mOffset = offset;
	}

	/**
	 * Return the position in the parsed stream where the error occurred.  If the exception was not
	 * related to any part of the stream, then this will return -1.
	 */
	public int getOffset() {
		return mOffset;
	}

}
