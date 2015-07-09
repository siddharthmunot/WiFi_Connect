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

import android.os.Bundle;
import android.webkit.MimeTypeMap;

/**
 * Represents a Rhizome File manifest, with methods to serialise to/from a byte stream for storage
 * on disk.
 *
 * @author Andrew Bettison <andrew@org.WifiConnect.com>
 */
public class RhizomeManifest_File extends RhizomeManifest {

	public final static String SERVICE = "file";

	private String mName;

	@Override
	public RhizomeManifest_File clone() throws CloneNotSupportedException {
		return (RhizomeManifest_File) super.clone();
	}

	/** Construct an empty Rhizome manifest.
	 *
	 * @author Andrew Bettison <andrew@org.WifiConnect.com>
	 */
	public RhizomeManifest_File() throws RhizomeManifestParseException {
		super(SERVICE);
		mName = null;
	}

	/** Construct a Rhizome manifest from an Android Bundle containing various manifest fields.
	 *
	 * @author Andrew Bettison <andrew@org.WifiConnect.com>
	 */
	protected RhizomeManifest_File(Bundle b, byte[] signatureBlock) throws RhizomeManifestParseException {
		super(b, signatureBlock);
		mName = b.getString("name");
	}

	@Override
	protected void makeBundle() {
		super.makeBundle();
		if (mName != null) mBundle.putString("name", mName);
	}

	/** Return the 'name' field.
	 * @author Andrew Bettison <andrew@org.WifiConnect.com>
	 */
	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	@Override
	public String getDisplayName() {
		if (mName != null && !"".equals(mName))
			return mName;
		return super.getDisplayName();
	}

	public void setField(String name, String value) throws RhizomeManifestParseException {
		if ("name".equalsIgnoreCase(name))
			setName(value);
		else
			super.setField(name, value);
	}

	@Override
	public String getMimeType(){
		String ext = mName.substring(mName.lastIndexOf(".") + 1);
		String contentType = MimeTypeMap.getSingleton()
				.getMimeTypeFromExtension(ext);
		if (contentType==null || "".equals(contentType))
			return super.getMimeType();
		return contentType;
	}
}
