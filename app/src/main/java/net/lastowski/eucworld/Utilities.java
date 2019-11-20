package net.lastowski.eucworld;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;

import net.lastowski.common.Commons;
import net.lastowski.eucworld.utils.Constants;

public class Utilities {

    public static void performAction(int action, Context context, @NonNull Bundle options) {
        switch (action) {
            case Commons.Action.HORN:
                int mode = options.getInt("mode");
                switch (mode) {
                    case Commons.Action.HornMode.BUILT_IN:
                        context.sendBroadcast(new Intent(Constants.ACTION_REQUEST_HORN));
                        break;
                    case Commons.Action.HornMode.BLUETOOTH_AUDIO:
                        String path = options.getString("path");
                        MediaPlayer player = (path != null && !path.isEmpty()) ? new MediaPlayer() : MediaPlayer.create(context, R.raw.bicycle_bell);
                        try {
                            if (path != null && !path.isEmpty()) player.setDataSource(path);
                            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    player.start();
                                }
                            });
                            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    player.release();
                                }
                            });
                            if (path != null && !path.isEmpty()) player.prepareAsync();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
            case Commons.Action.LIGHT:
                context.sendBroadcast(new Intent(Constants.ACTION_REQUEST_LIGHT_TOGGLE));
                break;
            case Commons.Action.REQUEST_VOICE_MESSAGE:
                context.sendBroadcast(new Intent(Constants.ACTION_REQUEST_VOICE_REPORT));
                break;
            case Commons.Action.DISMISS_VOICE_MESSAGE:
                context.sendBroadcast(new Intent(Constants.ACTION_REQUEST_VOICE_DISMISS));
                break;
            default:
                break;
        }
    }

    public static int dpToPx(Context context, float dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }

}
