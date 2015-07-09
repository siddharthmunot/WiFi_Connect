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

package org.WifiConnect.messages;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.InputStream;

/**
 * various utility methods for dealing with messages
 */
public class MessageUtils {
	/**
	 * retrieve the contact photo given a contact id
	 *
	 * @param context
	 *            a context object used to get a content resolver
	 * @param id
	 *            the id number of contact
	 *
	 * @return the bitmap of the photo or null
	 */
	public static Bitmap loadContactPhoto(Context context, long id) {
		try {
			Uri uri = ContentUris.withAppendedId(
					ContactsContract.Contacts.CONTENT_URI, id);

			InputStream input = ContactsContract.Contacts
					.openContactPhotoInputStream(context.getContentResolver(),
							uri);
			if (input == null) {
				return null;
			}
			return BitmapFactory.decodeStream(input);
		} catch (Exception e) {
			// catch any security exceptions in APIv14
			Log.e("MessageUtils", e.getMessage(), e);
			return null;
		}
	}

}
