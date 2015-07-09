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

package org.WifiConnect.audio;

import java.io.IOException;

import org.WifiConnect.batphone.VoMP;

import android.util.Log;

public class TranscodeStream extends AudioStream {
	private final AudioStream out;
	private final boolean encode;
	VoMP.Codec codec;
	Codec encoder;

	public static TranscodeStream getEncoder(AudioStream out, VoMP.Codec codec) {
		return new TranscodeStream(out, codec);
	}

	public static TranscodeStream getDecoder(AudioStream out) {
		return new TranscodeStream(out, null);
	}

	private TranscodeStream(AudioStream out, VoMP.Codec codec) {
		this.out = out;
		encode = (codec != null);
		if (encode)
			createCodec(codec);
	}

	@Override
	public void close() throws IOException {
		if (out != null)
			out.close();

		if (encoder != null)
			encoder.close();
	}

	private void createCodec(VoMP.Codec codec) {
		switch (codec) {
		case Signed16:
			this.encoder = null;
			break;
		case Ulaw8:
		case Alaw8:
			this.encoder = new ULawCodec(codec == VoMP.Codec.Alaw8);
			break;
		case Codec2_1200:
		case Codec2_3200:
			this.encoder = new Codec2(codec);
			break;
		case Opus:
			this.encoder = new Opus();
			break;
		default:
			throw new IllegalStateException("Unsupported codec " + codec);
		}
		this.codec = codec;
	}

	@Override
	public void missed(int duration, boolean missing) throws IOException {
		if (this.encoder != null && duration >= 20) {
			AudioBuffer decoded = this.encoder.decode_missing(duration);
			if (decoded != null) {
				out.write(decoded);
				return;
			}
		}
		out.missed(duration, missing);
	}

	@Override
	public int write(AudioBuffer buff) throws IOException {
		AudioBuffer output;
		if (encode) {
			if (encoder == null)
				output = buff;
			else {
				output = encoder.encode(buff);
				buff.release();
			}
		} else {
			if (buff.codec != this.codec) {
				if (encoder != null)
					encoder.close();
				createCodec(buff.codec);
				Log.v("Transcoder", "Codec changed to " + buff.codec);
			}
			if (encoder == null)
				output = buff;
			else {
				output = encoder.decode(buff);
				buff.release();
			}
		}
		if (output == null)
			return -1;
		return out.write(output);
	}

	@Override
	public int sampleDurationMs(AudioBuffer buff) {
		if (buff.codec != this.codec) {
			if (encoder != null)
				encoder.close();
			createCodec(buff.codec);
			Log.v("Transcoder", "Codec changed to " + buff.codec);
		}

		return encoder.sampleLength(buff) / (codec.sampleRate / 1000);
	}

	@Override
	public int getBufferDuration() {
		return out.getBufferDuration();
	}

}
