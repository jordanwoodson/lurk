package io.github.jordanwoodson.extension.shared;

import android.content.Context;
import android.net.ConnectivityManager;

/** The device's network, for settings that depend on it. */
public final class Network {
    private Network() {
    }

    /**
     * Whether the active network is metered (mobile data, or a Wi-Fi hotspot marked as metered). False when
     * it can't tell. Needs ACCESS_NETWORK_STATE, which the app must declare.
     */
    public static boolean isMetered(Context context) {
        if (context == null) return false;
        try {
            ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
            return manager != null && manager.isActiveNetworkMetered();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
