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

package org.WifiConnect.share.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.WifiConnect.R;

public class RoundedImageButtonWithTextView extends RelativeLayout {

    public RoundedImageButtonWithTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RoundedImageButtonWithTextView, 0, 0);
        Drawable drawable = typedArray.getDrawable(R.styleable.RoundedImageButtonWithTextView_imageSrc);
        String text = typedArray.getString(R.styleable.RoundedImageButtonWithTextView_text);
        typedArray.recycle();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.rounded_image_button_with_text_view, this, true);

        setupImageButton(drawable);
        setupTextView(text);
    }

    private void setupTextView(String text) {
        TextView textView = (TextView) getChildAt(1);
        textView.setText(text);
    }

    private void setupImageButton(Drawable drawable) {
        ImageButton imageButton = (ImageButton) getChildAt(0);
        imageButton.setImageDrawable(drawable);
    }
}
