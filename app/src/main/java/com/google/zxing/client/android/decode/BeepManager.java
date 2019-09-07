/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.decode;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

import me.yoqi.qrcode.Config;
import me.yoqi.qrcode.R;

/**
 * Manages beeps and vibrations for {@link CaptureActivity}.
 */
public final class BeepManager
{

    private static final String TAG              = BeepManager.class.getSimpleName ();

    private static final float  BEEP_VOLUME      = 0.10f;
    private static final long   VIBRATE_DURATION = 200L;

    private final Activity      activity;
    private MediaPlayer         mediaPlayer;
    private boolean             playBeep;
    private boolean             vibrate;

    public BeepManager(Activity activity)
    {
        this.activity = activity;
        this.mediaPlayer = null;
        updatePrefs ();
    }

    public void updatePrefs(){
        playBeep = shouldBeep (activity);
        vibrate =Config.KEY_VIBRATE;
        if (playBeep && mediaPlayer == null)
        {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
            // so we now play on the music stream.
            activity.setVolumeControlStream (AudioManager.STREAM_MUSIC);
            mediaPlayer = buildMediaPlayer (activity);
        }
    }

    public void playBeepSoundAndVibrate(){
        if (playBeep && mediaPlayer != null)
        {
            mediaPlayer.start ();
        }
        if (vibrate)
        {
            Vibrator vibrator = (Vibrator) activity.getSystemService (Context.VIBRATOR_SERVICE);
            vibrator.vibrate (VIBRATE_DURATION);
        }
    }

    private static boolean shouldBeep(Context activity){
        boolean shouldPlayBeep = Config.KEY_PLAY_BEEP;
        if (shouldPlayBeep)
        {
            // See if sound settings overrides this
            AudioManager audioService = (AudioManager) activity.getSystemService (Context.AUDIO_SERVICE);
            if (audioService.getRingerMode () != AudioManager.RINGER_MODE_NORMAL)
            {
                shouldPlayBeep = false;
            }
        }
        return shouldPlayBeep;
    }

    private static MediaPlayer buildMediaPlayer(Context activity){
        MediaPlayer mediaPlayer = new MediaPlayer ();
        mediaPlayer.setAudioStreamType (AudioManager.STREAM_MUSIC);
        // When the beep has finished playing, rewind to queue up another one.
        mediaPlayer.setOnCompletionListener (new MediaPlayer.OnCompletionListener ()
        {

            @Override
            public void onCompletion(MediaPlayer player){
                player.seekTo (0);
            }
        });

        AssetFileDescriptor file = activity.getResources ().openRawResourceFd (R.raw.beep);
        try
        {
            mediaPlayer.setDataSource (file.getFileDescriptor (), file.getStartOffset (), file.getLength ());
            file.close ();
            mediaPlayer.setVolume (BEEP_VOLUME, BEEP_VOLUME);
            mediaPlayer.prepare ();
        } catch (IOException ioe)
        {
            Log.w (TAG, ioe);
            mediaPlayer = null;
        }
        return mediaPlayer;
    }

}