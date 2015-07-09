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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import org.WifiConnect.R;
import org.WifiConnect.share.UriInterpretation;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {

    public static final int REQUEST_CODE = 1024;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupTextViews();
        setupNavigationViews();
        createViewClickListener();
        setupPickItemView();
       }

    private void setupPickItemView() {
        findViewById(R.id.pick_items).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isKitKatOrHigher = getResources().getBoolean(R.bool.isKitKatOrHigher);
                if (isKitKatOrHigher) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setType("*/*");
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, REQUEST_CODE);
                }
            }
        });
    }

    private void setViewsVisible() {
        findViewById(R.id.link_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.navigation_layout).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ArrayList<UriInterpretation> uriList = getFileUris(data);
            populateUriPath(uriList);
            initHttpServer(uriList);
            saveServerUrlToClipboard();
            setLinkMessageToView();
            setViewsVisible();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Read values from the "savedInstanceState"-object and put them in your textview
        super.onRestoreInstanceState(savedInstanceState);
        uriPath.setText(savedInstanceState.getCharSequence("uriPath"));
        link_msg.setText(savedInstanceState.getCharSequence("link_msg"));

        if (!savedInstanceState.getCharSequence("uriPath").equals("")) {
            setViewsVisible();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("link_msg", link_msg.getText());
        outState.putCharSequence("uriPath", uriPath.getText());
        // Save the values you need from your textview into "outState"-object
        super.onSaveInstanceState(outState);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private ArrayList<UriInterpretation> getFileUris(Intent data) {
        ArrayList<UriInterpretation> theUris = new ArrayList<UriInterpretation>();
        Uri dataUri = data.getData();
        if (dataUri != null) {
            theUris.add(new UriInterpretation(dataUri));
        } else {
            ClipData clipData = data.getClipData();
            for (int i = 0; i < clipData.getItemCount(); ++i) {
                ClipData.Item item = clipData.getItemAt(i);
                Uri uri = item.getUri();
                theUris.add(new UriInterpretation(uri));
            }
        }
        return theUris;
    }
}
