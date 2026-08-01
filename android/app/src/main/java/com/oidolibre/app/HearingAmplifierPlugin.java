package com.oidolibre.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PermissionState;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
        name = "HearingAmplifier",
        permissions = {
                @Permission(alias = "microphone", strings = { Manifest.permission.RECORD_AUDIO }),
                @Permission(alias = "notifications", strings = { Manifest.permission.POST_NOTIFICATIONS })
        }
)
public class HearingAmplifierPlugin extends Plugin {
    @PluginMethod
    public void start(PluginCall call) {
        if (getPermissionState("microphone") != PermissionState.GRANTED) {
            requestPermissionForAlias("microphone", call, "microphonePermissionCallback");
            return;
        }

        startService(call);
    }

    @PermissionCallback
    private void microphonePermissionCallback(PluginCall call) {
        if (getPermissionState("microphone") == PermissionState.GRANTED) {
            startService(call);
        } else {
            call.reject("Oído Libre necesita permiso para usar el micrófono");
        }
    }

    private void startService(PluginCall call) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionForAlias("notifications", call, "notificationPermissionCallback");
            return;
        }

        launchAmplifier(call);
    }

    @PermissionCallback
    private void notificationPermissionCallback(PluginCall call) {
        // La amplificación puede funcionar aunque la persona decida no mostrar notificaciones.
        launchAmplifier(call);
    }

    private void launchAmplifier(PluginCall call) {

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
        result.put("paused", HearingAmplifierService.isPaused());
        call.resolve(result);
    }

    private int safeLevel(int level) {
        return Math.max(0, Math.min(level, 45));
    }
}
