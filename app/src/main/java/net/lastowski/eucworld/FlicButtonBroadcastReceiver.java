package net.lastowski.eucworld;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;

import net.lastowski.common.Commons;
import net.lastowski.eucworld.utils.Constants;
import net.lastowski.eucworld.utils.SettingsUtil;

import io.flic.lib.FlicBroadcastReceiver;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicManager;

public class FlicButtonBroadcastReceiver extends FlicBroadcastReceiver {

    @Override
    protected void onRequestAppCredentials(Context context) {
        FlicManager.setAppCredentials(Constants.flicKey(context), Constants.flicSecret(context), Constants.APP_NAME);
    }

    @Override
    public void onButtonSingleOrDoubleClickOrHold(Context context, FlicButton button, boolean wasQueued, int timeDiff, boolean isSingleClick, boolean isDoubleClick, boolean isHold) {
        int action = 0;
        Bundle options = new Bundle();
        if (isSingleClick)
            action = SettingsUtil.getFlicActionSingle(context);
        else
        if (isDoubleClick)
            action = SettingsUtil.getFlicActionDouble(context);
        else
        if (isHold) {
            action = SettingsUtil.getFlicActionHold(context);
        }
        if (action == Commons.Action.HORN) {
            int mode = SettingsUtil.getFlicHornMode(context);
            options.putInt("mode", mode);
            if (mode == Commons.Action.HornMode.BLUETOOTH_AUDIO) {
                if (SettingsUtil.getFlicUseCustomHornSound(context))
                    options.putString("path", SettingsUtil.getFlicCustomHornSoundPath(context));
            }
        }
        Utilities.performAction(action, context, options);
    }

}