/*
Copyright (c) 2011, Marcos Diez --  marcos AT unitron.com.br
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Neither the name of  Marcos Diez nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.WifiConnect.share;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.net.Uri;
import android.util.Log;

public class FileZipper implements Runnable {
	private void s(String s2) { // an alias to avoid typing so much!
		Log.d(Util.myLogName, s2);
	}

	OutputStream dest;
	ArrayList<UriInterpretation> inputUriInterpretations;
	Boolean atLeastOneDirectory = false;

	public FileZipper(OutputStream dest, ArrayList<UriInterpretation> inputUriInterpretations) {
		/*
		 * // get a list of files from current directory File f = new File(".");
		 * String inputFiles[] = f.list();
		 */
		this.dest = dest;
		this.inputUriInterpretations = inputUriInterpretations;

	}

	@Override
	public void run() {
		int BUFFER = 4096;

		try {
			// FileOutputStream dest = new
			// FileOutputStream("c:\\zip\\myfigs.zip");
			s("Initializing ZIP");

			CheckedOutputStream checksum = new CheckedOutputStream(dest,
					new Adler32());

			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
					checksum));

			// out.setMethod(method);
			// out.setLevel(1) ;
			byte data[] = new byte[BUFFER];
			for (UriInterpretation thisUriInterpretation : inputUriInterpretations) {
				addFileOrDirectory(BUFFER, out, data, thisUriInterpretation);
			}
			out.close();
			s("Zip Done. Checksum: " + checksum.getChecksum().getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void addFileOrDirectory(int BUFFER, ZipOutputStream out, byte[] data,
			UriInterpretation uriFile) throws FileNotFoundException,
			IOException {
		if (uriFile.isDirectory()) {
			addDirectory(BUFFER, out, data, uriFile);
		} else {
			addFile(BUFFER, out, data, uriFile);
		}
	}

	void addDirectory(int BUFFER, ZipOutputStream out, byte[] data,
			UriInterpretation uriFile) throws FileNotFoundException,
			IOException {
		atLeastOneDirectory = true;
		String directoryPath = uriFile.getUri().getPath();
		if (directoryPath.charAt(directoryPath.length() - 1) != File.separatorChar) {
			directoryPath += File.separatorChar;
		}
		ZipEntry entry = new ZipEntry(directoryPath.substring(1));
		out.putNextEntry(entry);

		s("Adding Directory: " + directoryPath);
		File f = new File(directoryPath);
		String[] theFiles = f.list();
		if (theFiles != null) {
			for (String aFilePath : theFiles) {
				if (!aFilePath.equals(".") && !aFilePath.equals("..")) {
					String fixedFileName = "file://" + directoryPath
							+ aFilePath;
					Uri aFileUri = Uri.parse(fixedFileName);
					UriInterpretation uriFile2 = new UriInterpretation(aFileUri);
					addFileOrDirectory(BUFFER, out, data, uriFile2);
				}
			}
		}

	}

	void addFile(int BUFFER, ZipOutputStream out, byte[] data,
			UriInterpretation uriFile) throws FileNotFoundException,
			IOException {
		BufferedInputStream origin;
		s("Adding File: " + uriFile.getUri().getPath() + " -- " + uriFile.getName());
		origin = new BufferedInputStream(uriFile.getInputStream(), BUFFER);

		ZipEntry entry = new ZipEntry(getFileName(uriFile));

		out.putNextEntry(entry);
		int count;
		while ((count = origin.read(data, 0, BUFFER)) != -1) {
			out.write(data, 0, count);
		}
		origin.close();
	}

	String getFileName(UriInterpretation uriFile) {
		/*	Galery Sends uri.getPath() with values like /external/images/media/16458
		 *  while urlFile.name returns IMG_20120427_120038.jpg
		 *
		 *  since such name has no directory info, that would break real directories
		 */
		if (atLeastOneDirectory) {
			return uriFile.getUri().getPath().substring(1);
		}
		return uriFile.getName();
	}

}
