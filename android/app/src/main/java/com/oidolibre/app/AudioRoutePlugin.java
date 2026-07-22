package com.oidolibre.app;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AudioRoute")
public class AudioRoutePlugin extends Plugin {
    private AudioManager audioManager;
    private AudioDeviceCallback deviceCallback;

    @Override
    public void load() {
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deviceCallback = new AudioDeviceCallback() {
                @Override
                public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                    notifyRouteChanged();
                }

                @Override
                public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                    notifyRouteChanged();
                }
            };

            audioManager.registerAudioDeviceCallback(deviceCallback, null);
        }
    }

    @PluginMethod
    public void getAudioRoute(PluginCall call) {
        call.resolve(currentRoute());
    }

    private void notifyRouteChanged() {
        notifyListeners("audioRouteChanged", currentRoute());
    }

    private JSObject currentRoute() {
        JSObject result = new JSObject();
        boolean connected = false;
        String kind = "speaker";
        String label = "Bocina del teléfono";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

            for (AudioDeviceInfo device : outputs) {
                String detectedKind = safeDeviceKind(device.getType());
                if (detectedKind != null) {
                    connected = true;
                    kind = detectedKind;
                    label = safeDeviceLabel(detectedKind);
                    break;
                }
            }
        }

        result.put("headphonesConnected", connected);
        result.put("kind", kind);
        result.put("label", label);
        return result;
    }

    private String safeDeviceKind(int type) {
        if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
            return "wired";
        }

        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            return "bluetooth";
        }

        if (type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && type == AudioDeviceInfo.TYPE_USB_HEADSET)) {
            return "usb";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            type == AudioDeviceInfo.TYPE_HEARING_AID) {
            return "hearing_aid";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            type == AudioDeviceInfo.TYPE_BLE_HEADSET) {
            return "bluetooth";
        }

        return null;
    }

    private String safeDeviceLabel(String kind) {
        switch (kind) {
            case "wired":
                return "Audífonos alámbricos";
            case "bluetooth":
                return "Audífonos Bluetooth (experimental)";
            case "usb":
                return "Audífonos USB";
            case "hearing_aid":
                return "Dispositivo auditivo";
            default:
                return "Audífonos conectados";
        }
    }

    @Override
    protected void handleOnDestroy() {
        if (audioManager != null && deviceCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.unregisterAudioDeviceCallback(deviceCallback);
        }
        super.handleOnDestroy();
    }
}
