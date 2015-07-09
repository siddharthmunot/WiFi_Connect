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

package org.WifiConnect.share.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.WifiConnect.share.UriInterpretation;
//import com.github.mrengineer13.snackbar.SnackBar;

import java.util.ArrayList;

import org.WifiConnect.share.MyHttpServer;
import org.WifiConnect.R;
import org.WifiConnect.share.Util;

public class BaseActivity extends ActionBarActivity {

    protected static MyHttpServer httpServer = null;
    protected String preferredServerUrl;
    protected CharSequence[] listOfServerUris;
    protected final Activity thisActivity = this;

    // LinkMessageView
    protected TextView link_msg;

    protected TextView uriPath;

    // NavigationViews
    protected View stopServer;
    protected View sharePeers;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.context = this;
    }

    protected void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setTitleTextColor(getResources().getColor(R.color.light_blue));
        setSupportActionBar(toolbar);
    }

    protected void setupTextViews() {
        link_msg = (TextView) findViewById(R.id.link_msg);
        uriPath = (TextView) findViewById(R.id.uriPath);
    }

    protected void setupNavigationViews() {
        stopServer = findViewById(R.id.stop_server);
        sharePeers = findViewById(R.id.button_share_url);
      }

    protected void createViewClickListener() {
          stopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	//stopServer();
                MyHttpServer p = httpServer;
                httpServer = null;
                if (p != null) {
                    p.stopServer();
                }
                Intent intent1 = new Intent(getApplicationContext(),
    					MainActivity.class);
                //intent1.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                finish();
                startActivity(intent1);
                ClipboardManager clipboard = (ClipboardManager)
    					getSystemService(Context.CLIPBOARD_SERVICE);
    			clipboard.setPrimaryClip(ClipData.newPlainText(null, null));
               }
        });

        sharePeers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	onBackPressed();
                //Intent i = new Intent(Intent.ACTION_SEND);
                //i.setType("text/plain");
                //i.putExtra(Intent.EXTRA_TEXT, preferredServerUrl);
                //startActivity(Intent.createChooser(i, BaseActivity.this.getString(R.string.share_url)));
            }
        });
    }
    
    public static void stopServer() {
    	 MyHttpServer p = httpServer;
          httpServer = null;
          if (p != null) {
                p.stopServer();
            }
    }

   
    protected void populateUriPath(ArrayList<UriInterpretation> uriList) {
        StringBuilder output = new StringBuilder();
        String sep = "\n";
        boolean first = true;
        for( UriInterpretation thisUriInterpretation : uriList){
            if(first){
                first = false;
            }else{
                output.append(sep);
            }
            output.append(thisUriInterpretation.getPath());
        }
        uriPath.setText(output.toString());
    }

    protected void initHttpServer(ArrayList<UriInterpretation> myUris) {
        Util.context = this.getApplicationContext();
        if (myUris == null || myUris.size() == 0) {
            finish();
            return;
        }

        httpServer = new MyHttpServer(9999);
        listOfServerUris = httpServer.ListOfIpAddresses();
        preferredServerUrl = listOfServerUris[0].toString();

        httpServer.SetFiles(myUris);
    }

    protected void saveServerUrlToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(preferredServerUrl, preferredServerUrl));
       
    }

    protected void setLinkMessageToView() {
        link_msg.setPaintFlags(link_msg.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        link_msg.setText(preferredServerUrl);
    }

    
 }
