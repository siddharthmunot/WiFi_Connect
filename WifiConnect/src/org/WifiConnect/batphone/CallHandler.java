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

package org.WifiConnect.batphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;

import org.WifiConnect.R;
import org.WifiConnect.ServalBatPhoneApplication;
import org.WifiConnect.audio.AudioBuffer;
import org.WifiConnect.audio.AudioPlaybackStream;
import org.WifiConnect.audio.AudioRecordStream;
import org.WifiConnect.audio.AudioStream;
import org.WifiConnect.audio.BufferList;
import org.WifiConnect.audio.JitterStream;
import org.WifiConnect.audio.TranscodeStream;
import org.WifiConnect.servald.DnaResult;
import org.WifiConnect.servald.Identity;
import org.WifiConnect.servald.Peer;
import org.WifiConnect.servald.PeerListService;
import org.WifiConnect.servald.ServalDMonitor;
import org.WifiConnect.servaldna.SubscriberId;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

// This class maintains the state of a call
// handles the lifecycle of recording and playback
// and the triggers the display of any activities.
public class CallHandler {
	final Peer remotePeer;
	String did;
	String name;
	private int local_id = 0;
	private String localIdString = null;

	public enum CallState{
		Prep(R.string.outgoing_call),
		Ringing(R.string.incoming_call),
		RemoteRinging(R.string.outgoing_call),
		InCall(R.string.in_call),
		End(R.string.call_ended);

		public final int displayResource;
		private CallState(int resource){
			this.displayResource = resource;
		}
	};

	public CallState state = null;
	public VoMP.Codec codec = VoMP.Codec.Signed16;
	private long lastKeepAliveTime;
	private long callStarted = SystemClock.elapsedRealtime();
	private long callEnded;
	private boolean uiStarted = false;
	private boolean initiated = false;
	private final ServalBatPhoneApplication app;
	private final ServalDMonitor monitor;
	private UnsecuredCall ui;
	private MediaPlayer mediaPlayer;
	private BufferList bufferList;
	private final Timer timer = new Timer();

	private Thread audioRecordThread;
	private AudioRecordStream recorder;
	public JitterStream player;
	private boolean ringing = false;
	private boolean audioRunning = false;

	private static final String TAG = "CallHandler";
	private AudioStream monitorOutput = new AudioStream() {
		@Override
		public int write(AudioBuffer buff) throws IOException {
			try {
				if (monitor.hasStopped())
					throw new EOFException();
				monitor.sendMessageAndData(buff.buff, buff.dataLen, "audio ",
						localIdString, " ",
						buff.codec.codeString, " ",
						Integer.toString(buff.sampleStart), " ",
						Integer.toString(buff.sequence));
			} finally {
				buff.release();
			}
			return 0;
		}
	};

	public static void dial(DnaResult result) throws IOException {
		CallHandler call = createCall(result.peer);
		call.did = result.ext == null ? result.did : result.ext;
		call.name = result.name;
		call.dial();
	}

	public static void dial(Peer peer) throws IOException {
		dial(null, peer);
	}

	public static CallHandler dial(UnsecuredCall ui, Peer peer) throws IOException {
		CallHandler call = createCall(peer);
		call.ui = ui;
		call.dial();
		return call;
	}

	private static synchronized CallHandler createCall(Peer peer)
			throws IOException {
		ServalBatPhoneApplication app = ServalBatPhoneApplication.context;
		if (app.callHandler != null)
			throw new IOException(
					"Only one call is allowed at a time");
		ServalDMonitor monitor = app.server.getMonitor();
		if (monitor == null)
			throw new IOException(
					"Not currently connected to serval daemon");
		app.callHandler = new CallHandler(app, monitor, peer);
		return app.callHandler;
	}

	private static class EventMonitor implements ServalDMonitor.Messages {
		private final ServalDMonitor monitor;
		ServalBatPhoneApplication app = ServalBatPhoneApplication.context;

		private EventMonitor(ServalDMonitor monitor){
			this.monitor = monitor;
			monitor.addHandler("CALLFROM", this);
			monitor.addHandler("CALLTO", this);
			monitor.addHandler("CODECS", this);
			monitor.addHandler("RINGING", this);
			monitor.addHandler("ANSWERED", this);
			monitor.addHandler("AUDIO", this);
			monitor.addHandler("HANGUP", this);
			monitor.addHandler("KEEPALIVE", this);
		}

		@Override
		public void onConnect(ServalDMonitor monitor) {
			// tell servald that we can initiate and answer phone calls, and
			// the list of codecs we support
			StringBuilder sb = new StringBuilder("monitor vomp");
			for (VoMP.Codec codec : VoMP.Codec.values()) {
				if (codec.isSupported())
					sb.append(' ').append(codec.codeString);
			}
			try {
				monitor.sendMessage(sb.toString());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}

		@Override
		public void onDisconnect(ServalDMonitor monitor) {

		}

		private boolean checkSession(Iterator<String> args){
			int local_session = ServalDMonitor.parseIntHex(args.next());
			if (app.callHandler != null && app.callHandler.local_id == local_session){
				app.callHandler.lastKeepAliveTime = SystemClock.elapsedRealtime();
				return true;
			}

			// one call at a time
			monitor.sendMessageAndLog("hangup ", Integer.toHexString(local_session));
			return false;
		}

		@Override
		public int message(String cmd, Iterator<String> args, InputStream in,
				int dataLength) throws IOException {
			int ret = 0;

			if (cmd.equalsIgnoreCase("HANGUP") && app.callHandler==null)
				// NOOP
				return 0;

			int local_session = ServalDMonitor.parseIntHex(args.next());
			if (app.callHandler==null){
				if(cmd.equals("CALLFROM")){
					try {
						args.next(); // local_sid
						args.next(); // local_did
						SubscriberId remote_sid = new SubscriberId(args.next());
						String remote_did = args.next();
						Peer peer = PeerListService.getPeer(remote_sid);

						CallHandler call = createCall(peer);
						call.local_id = local_session;
						call.localIdString = Integer.toHexString(local_session);
						call.did = remote_did;
						call.lastKeepAliveTime = SystemClock.elapsedRealtime();
						monitor.sendMessageAndLog("ringing ",
								Integer.toHexString(local_session));
						call.setCallState(CallState.Ringing);
						return 0;
					} catch (SubscriberId.InvalidHexException e) {
						throw new IOException("invalid SubscriberId token: " + e);
					}
				}
			}else if (cmd.equalsIgnoreCase("CALLTO")) {
				try{
					SubscriberId my_sid = new SubscriberId(args.next());
					args.next(); // local_did
					SubscriberId remote_sid = new SubscriberId(args.next());
					args.next(); // remote_did

					if (   app.callHandler.state == null
							&& app.callHandler.remotePeer.getSubscriberId().equals(remote_sid)
							&& app.callHandler.initiated){
						app.callHandler.local_id = local_session;
						app.callHandler.localIdString = Integer.toHexString(local_session);
						app.callHandler.lastKeepAliveTime = SystemClock.elapsedRealtime();
						app.callHandler.setCallState(CallState.Prep);
						return 0;
					}
				} catch (SubscriberId.InvalidHexException e) {
					throw new IOException("invalid SubscriberId token: " + e);
				}
			}else if(app.callHandler.local_id==local_session){
				app.callHandler.lastKeepAliveTime = SystemClock.elapsedRealtime();
				if (cmd.equalsIgnoreCase("CODECS")) {
					app.callHandler.codecs(args);
				}else if(cmd.equalsIgnoreCase("RINGING")) {
					app.callHandler.setCallState(CallState.RemoteRinging);
				}else if(cmd.equalsIgnoreCase("ANSWERED")) {
					app.callHandler.setCallState(CallState.InCall);
				} else if (cmd.equalsIgnoreCase("AUDIO")) {
					ret += app.callHandler.receivedAudio(args, in, dataLength);
				} else if (cmd.equalsIgnoreCase("HANGUP")) {
					app.callHandler.setCallState(CallState.End);
				}
				return ret;
			}
			// one call at a time
			monitor.sendMessageAndLog("hangup ", Integer.toHexString(local_session));
			return ret;
		}
	}

	public static void registerMessageHandlers(ServalDMonitor monitor) {
		new EventMonitor(monitor);
	}

	private CallHandler(ServalBatPhoneApplication app, ServalDMonitor monitor,
			Peer peer) {
		this.app = app;
		this.monitor = monitor;
		this.remotePeer = peer;
		this.did = peer.did;
		this.name = peer.name;
		lastKeepAliveTime = SystemClock.elapsedRealtime();

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				long now = SystemClock.elapsedRealtime();
				if (now > (lastKeepAliveTime + 5000)) {
					// End call if no keep alive received
					Log.d(TAG,
							"Keepalive expired for call: "
									+ lastKeepAliveTime + " vs "
									+ now);
					hangup();
				}
			}
		}, 0, 3000);
	}

	public void hangup() {
		Log.d(TAG, "Hanging up");

		if (!monitor.hasStopped())
			monitor.sendMessageAndLog("hangup ", Integer.toHexString(local_id));

		setCallState(CallState.End);
	}

	private void stopRinging(){
		if (!ringing)
			return;
		Log.v(TAG, "Stopping ring tone");
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		Vibrator v = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
		if (v != null)
			v.cancel();
		ringing = false;
	}

	public void pickup() {
		if (state == CallState.Ringing){
			Log.d(TAG, "Picking up");
			monitor.sendMessageAndLog("pickup ", Integer.toHexString(local_id));
			app.callHandler.setCallState(CallState.InCall);
		}
	}

	private void startRinging() {
		if (ringing)
			return;

		Log.v(TAG, "Starting ring tone");
		final AudioManager audioManager = (AudioManager) app
				.getSystemService(Context.AUDIO_SERVICE);
		if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
			Uri alert = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			if (mediaPlayer == null)
				mediaPlayer = new MediaPlayer();
			try {
				mediaPlayer.setDataSource(app, alert);
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
				mediaPlayer.start();
			} catch (Exception e) {
				Log.e(TAG,
						"Could not get ring tone: " + e.toString(), e);
			}
		} else {
			// volume off, so vibrate instead
			Vibrator v = (Vibrator) app
					.getSystemService(Context.VIBRATOR_SERVICE);
			if (v != null) {
				// bzzt-bzzt ...... bzzt,bzzt ......
				long[] pattern = {
						0, 300, 200, 300, 2000
				};
				v.vibrate(pattern, 0);
			}
		}

		ringing = true;
	}

	private void startAudio() {
		try {
			if (this.recorder == null)
				throw new IllegalStateException(
						"Audio recorder has not been initialised");
			Log.v(TAG, "Starting audio");

			this.recorder.setStream(TranscodeStream.getEncoder(monitorOutput,
					codec));

			AudioManager am = (AudioManager) app
					.getSystemService(Context.AUDIO_SERVICE);

			AudioPlaybackStream playback = new AudioPlaybackStream(
					am,
					AudioManager.STREAM_VOICE_CALL,
					SAMPLE_RATE,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					8 * 60 * 2);

			AudioStream output = TranscodeStream.getDecoder(playback);

			this.player = new JitterStream(output);
			this.player.startPlaying();

			audioRunning = true;
		} catch (Exception e) {
			Log.v(TAG, e.getMessage(), e);
		}
	}

	private void stopAudio() {
		if (this.recorder == null)
			throw new IllegalStateException(
					"Audio recorder has not been initialised");
		Log.v(TAG, "Stopping audio");
		this.recorder.close();
		try {
			this.player.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		audioRunning = false;
	}

	static final int SAMPLE_RATE = 8000;

	private void setCallState(CallState state) {
		if (this.state == state)
			return;
		this.state = state;
		Log.v(TAG, "Call state changed to " + state);

		// TODO play audio indicator for Prep / RemoteRinging / End

		if (ringing != (state == CallState.Ringing)) {
			if (ringing)
				stopRinging();
			else
				startRinging();
		}
		if (audioRunning != (state == CallState.InCall)) {
			if (audioRunning) {
				callEnded = SystemClock.elapsedRealtime();
				stopAudio();
			} else {
				callStarted = SystemClock.elapsedRealtime();
				startAudio();
			}
		}

		Intent myIntent = new Intent(
				app,
				UnsecuredCall.class);

		myIntent.putExtra(UnsecuredCall.EXTRA_SID, remotePeer.getSubscriberId().toHex());
		myIntent.putExtra(UnsecuredCall.EXTRA_EXISTING, true);

		// Create call as a standalone activity
		// stack
		myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
				Intent.FLAG_ACTIVITY_CLEAR_TOP |
				Intent.FLAG_ACTIVITY_SINGLE_TOP);

		// open the UI if we initiated the call, or we reached ringing
		// state.
		if (ui != null)
			ui.runOnUiThread(ui.updateCallStatus);
		else if(state != CallState.End && !uiStarted) {
			Log.v(TAG, "Starting in call ui");
			uiStarted = true;
			ServalBatPhoneApplication.context.startActivity(myIntent);
		}

		// make sure invalid states don't open the UI
		NotificationManager nm = (NotificationManager) app
				.getSystemService(Context.NOTIFICATION_SERVICE);

		if (state == CallState.End){
			if (this.recorder != null) {
				this.recorder.close();
				recorder = null;
			}
			if (this.player != null)
				try {
					this.player.close();
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			timer.cancel();
			nm.cancel("Call", ServalBatPhoneApplication.NOTIFY_CALL);
			app.callHandler = null;
		}else{
			// Update the in call notification so the user can re-open the UI
			Notification inCall = new Notification(
					android.R.drawable.stat_sys_phone_call,
					remotePeer.getDisplayName(),
					System.currentTimeMillis());

			inCall.setLatestEventInfo(app, "WiCo Phone Call",
					remotePeer.getDisplayName(),
					PendingIntent.getActivity(app, 0,
							myIntent,
							PendingIntent.FLAG_UPDATE_CURRENT));
			nm.notify("Call", ServalBatPhoneApplication.NOTIFY_CALL, inCall);
		}
	}

	public void setCallUI(UnsecuredCall ui) {
		this.ui = ui;
		uiStarted = ui != null;
	}

	public void dial() {

		Identity main = Identity.getMainIdentity();
		if (main == null) {
			app.displayToastMessage("Unable to place call as I don't know who I am");
			return;
		}
		Log.v(TAG, "Calling " + remotePeer.sid.abbreviation() + "/"
				+ did);
		initiated = true;
		monitor.sendMessageAndLog("call ",
				remotePeer.sid.toHex(), " ",
				main.getDid(), " ", did);
	}

	public int receivedAudio(Iterator<String> args, InputStream in,
			int dataBytes) throws IOException {
		// ignore audio if not in call
		if (state != CallState.InCall)
			return 0;

		if (bufferList == null)
			bufferList = new BufferList(VoMP.Codec.Signed16.maxBufferSize() / 2);

		if (dataBytes > bufferList.mtu) {
			Log.v(TAG, "Audio size " + dataBytes
					+ " is larger than buffer MTU " + bufferList.mtu);
			return 0;
		}
		AudioBuffer buff = bufferList.getBuffer();

		buff.received = lastKeepAliveTime;

		buff.codec = VoMP.Codec.getCodec(ServalDMonitor
				.parseInt(args.next()));
		buff.sampleStart = ServalDMonitor.parseInt(args.next());
		buff.sequence = ServalDMonitor.parseInt(args.next()); // sequence
		player.setJitterDelay(ServalDMonitor.parseInt(args.next()));
		buff.thisDelay = ServalDMonitor.parseInt(args.next());
		buff.dataLen = dataBytes;

		int read = 0;
		while (read < dataBytes) {
			int actualRead = in.read(buff.buff, read, dataBytes - read);
			if (actualRead < 0)
				throw new EOFException();
			read += actualRead;
		}
		player.write(buff);
		return read;
	}

	public void codecs(Iterator<String> args) {
		try {
			VoMP.Codec best = null;

			while (args.hasNext()) {
				int c = ServalDMonitor.parseInt(args.next());
				VoMP.Codec codec = VoMP.Codec.getCodec(c);
				if (!codec.isSupported())
					continue;

				if (best == null || codec.preference > best.preference) {
					best = codec;
				}
			}

			if (best == null)
				throw new IOException("Unable to find a common codec");

			this.codec = best;
			int audioSource = MediaRecorder.AudioSource.MIC;
			if (Build.VERSION.SDK_INT >= 11)
				audioSource = 7; //MediaRecorder.AudioSource.VOICE_COMMUNICATION;
			recorder = new AudioRecordStream(
					null,
					audioSource,
					codec.sampleRate,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					8 * 100 * 2,
					codec.audioBufferSize(),
					codec.maxBufferSize());

			audioRecordThread = new Thread(recorder, "Recording");
			audioRecordThread.start();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			this.hangup();
		}
	}

	public long getCallStarted() {
		return callStarted;
	}

}
