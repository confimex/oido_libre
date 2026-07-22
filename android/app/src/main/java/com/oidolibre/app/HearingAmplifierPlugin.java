package com.oidolibre.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "HearingAmplifier")
public class HearingAmplifierPlugin extends Plugin {
    @PluginMethod
    public void start(PluginCall call) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            call.reject("Falta el permiso del micrófono");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1502);
        }

        Intent intent = new Intent(getContext(), HearingAmplifierService.class);
        intent.setAction(HearingAmplifierService.ACTION_START);
        intent.putExtra(HearingAmplifierService.EXTRA_LEFT_LEVEL, safeLevel(call.getInt("leftLevel", 0)));
        intent.putExtra(HearingAmplifierService.EXTRA_RIGHT_LEVEL, safeLevel(call.getInt("rightLevel", 0)));
        ContextCompat.startForegroundService(getContext(), intent);
        call.resolve();
    }

    @PluginMethod
    public void updateLevels(PluginCall call) {
        Intent intent = new Intent(getContext(), HearingAmplifierService.class);
        intent.setAction(HearingAmplifierService.ACTION_UPDATE);
        intent.putExtra(HearingAmplifierService.EXTRA_LEFT_LEVEL, safeLevel(call.getInt("leftLevel", 0)));
        intent.putExtra(HearingAmplifierService.EXTRA_RIGHT_LEVEL, safeLevel(call.getInt("rightLevel", 0)));
        getContext().startService(intent);
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Intent intent = new Intent(getContext(), HearingAmplifierService.class);
        intent.setAction(HearingAmplifierService.ACTION_STOP);
        getContext().startService(intent);
        call.resolve();
    }

    @PluginMethod
    public void getState(PluginCall call) {
        JSObject result = new JSObject();
        result.put("active", HearingAmplifierService.isRunning());
        call.resolve(result);
    }

    private int safeLevel(int level) {
        return Math.max(0, Math.min(level, 45));
    }
}
