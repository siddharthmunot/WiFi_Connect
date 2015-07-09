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

//package webs;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.net.Uri;
import android.util.Log;

/**
 *
 * Title: A simple Webserver Tutorial NO warranty, NO guarantee, MAY DO damage
 * to FILES, SOFTWARE, HARDWARE!! Description: This is a simple tutorial on
 * making a webserver posted on http://turtlemeat.com . Go there to read the
 * tutorial! This program and sourcecode is free for all, and you can copy and
 * modify it as you like, but you should give credit and maybe a link to
 * turtlemeat.com, you know R-E-S-P-E-C-T. You gotta respect the work that has
 * been put down.
 *
 * Copyright: Copyright (c) 2002 Company: TurtleMeat
 *
 * @author: Jon Berg <jon.berg[on_server]turtlemeat.com
 * @version 1.0
 */

// file: server.java
// the real (http) serverclass
// it extends thread so the server is run in a different
// thread than the gui, that is to make it responsive.
// it's really just a macho coding thing.
public class MyHttpServer extends Thread {

	// by design, we only serve one file at a time.

	public static void SetFiles(ArrayList<UriInterpretation> fileUris) {
		MyHttpServer.fileUris = fileUris;
	}

	public static ArrayList<UriInterpretation> GetFiles(){
		return MyHttpServer.fileUris;
	}

	// default port is 80
	public MyHttpServer(int listen_port) {
		port = listen_port;
		if (serversocket == null) {
			this.start();
		}
	}

	private static int port;
	private static ArrayList<UriInterpretation> fileUris;

	private static ServerSocket serversocket = null;
	private boolean webserverLoop = true;

	private String getServerUrl(String ipAddress) {
		if (port == 80) {
			return "http://" + ipAddress + "/";
		}
        if( ipAddress.indexOf(":") >= 0 ){
            // IPv6
            int pos = ipAddress.indexOf("%");
            // java insists in adding %wlan and %p2p0 to everything
            if( pos > 0 ){
                ipAddress = ipAddress.substring(0, pos);
            }
            return "http://[" + ipAddress + "]:" + port + "/";
        }
		return "http://" + ipAddress + ":" + port + "/";
	}

	public synchronized void stopServer() {
		s("Closing server...\n\n");
		webserverLoop = false;
		if (serversocket != null) {
			try {
				serversocket.close();
				serversocket = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}
	}

	public CharSequence[] ListOfIpAddresses() {
		ArrayList<String> arrayOfIps = new ArrayList<String>();


		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();

				Log.d(Util.myLogName , "Inteface: " + intf.getDisplayName());
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();

					String theIpTemp = inetAddress.getHostAddress();
					String theIp = getServerUrl(theIpTemp);

					if (inetAddress instanceof Inet6Address
							|| inetAddress.isLoopbackAddress()) {

                        arrayOfIps.add(theIp);
						continue;
					}

                    arrayOfIps.add(0, theIp); // we prefer non local IPv4
				}
			}

            if(arrayOfIps.size() == 0){
                String firstIp = getServerUrl("0.0.0.0");
                arrayOfIps.add(firstIp);
            }

		} catch (SocketException ex) {
			Log.e("httpServer", ex.toString());
		}

		CharSequence[] output = arrayOfIps.toArray(new CharSequence[arrayOfIps
				.size()]);
		return output;
	}

	public static String getLocalIpAddress() {
		try {
			InetAddress localAddress = null;
			InetAddress ipv6 = null;

			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (inetAddress instanceof Inet6Address) {
						ipv6 = inetAddress;
						continue;
					}
					if (inetAddress.isLoopbackAddress()) {
						localAddress = inetAddress;
						continue;
					}
					return inetAddress.getHostAddress().toString();
				}
			}
			if (ipv6 != null) {
				return ipv6.getHostAddress().toString();
			}
			if (localAddress != null) {
				return localAddress.getHostAddress().toString();
			}
			return "0.0.0.0";

		} catch (SocketException ex) {
			Log.e("httpServer", ex.toString());
		}
		return "0.0.0.0";
	}

	private boolean normalBind(int thePort) {
		s("Attempting to bind on port " + thePort);
		try {
			serversocket = new ServerSocket(thePort);
		} catch (Exception e) {
			s("Fatal Error:" + e.getMessage() + " " + e.getClass().toString());
			return false;
		}
		port = thePort;
		s("Binding was OK!");
		return true;
	}

	private static final ExecutorService threadPool = Executors.newCachedThreadPool();

	public void run() {
		s("Starting " + Util.myLogName + " server v" + Util.getAppVersion());
		if (!normalBind(port)) {
			return;
		}

		// go in a infinite loop, wait for connections, process request, send
		// response
		while (webserverLoop) {
			s("Ready, Waiting for requests...\n");
			try {
				if(serversocket == null) {
					return;
				}
				Socket connectionsocket = serversocket.accept();
				HttpServerConnection theHttpConnection = new HttpServerConnection(fileUris, connectionsocket);

				threadPool.submit(theHttpConnection);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void s(String s2) { // an alias to avoid typing so much!
		Log.d(Util.myLogName, s2);
	}

}
