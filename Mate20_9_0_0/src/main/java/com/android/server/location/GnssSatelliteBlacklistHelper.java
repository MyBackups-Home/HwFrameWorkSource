package com.android.server.location;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;

class GnssSatelliteBlacklistHelper {
    private static final String BLACKLIST_DELIMITER = ",";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "GnssBlacklistHelper";
    private final GnssSatelliteBlacklistCallback mCallback;
    private final Context mContext;

    interface GnssSatelliteBlacklistCallback {
        void onUpdateSatelliteBlacklist(int[] iArr, int[] iArr2);
    }

    GnssSatelliteBlacklistHelper(Context context, Looper looper, GnssSatelliteBlacklistCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("gnss_satellite_blacklist"), true, new ContentObserver(new Handler(looper)) {
            public void onChange(boolean selfChange) {
                GnssSatelliteBlacklistHelper.this.updateSatelliteBlacklist();
            }
        }, -1);
    }

    void updateSatelliteBlacklist() {
        String blacklist = Global.getString(this.mContext.getContentResolver(), "gnss_satellite_blacklist");
        if (blacklist == null) {
            blacklist = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        int i = 0;
        if (DEBUG) {
            Log.d(TAG, String.format("Update GNSS satellite blacklist: %s", new Object[]{blacklist}));
        }
        try {
            List<Integer> blacklistValues = parseSatelliteBlacklist(blacklist);
            if (blacklistValues.size() % 2 != 0) {
                Log.e(TAG, "blacklist string has odd number of values.Aborting updateSatelliteBlacklist");
                return;
            }
            int length = blacklistValues.size() / 2;
            int[] constellations = new int[length];
            int[] svids = new int[length];
            while (i < length) {
                constellations[i] = ((Integer) blacklistValues.get(i * 2)).intValue();
                svids[i] = ((Integer) blacklistValues.get((i * 2) + 1)).intValue();
                i++;
            }
            this.mCallback.onUpdateSatelliteBlacklist(constellations, svids);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Exception thrown when parsing blacklist string.", e);
        }
    }

    @VisibleForTesting
    static List<Integer> parseSatelliteBlacklist(String blacklist) throws NumberFormatException {
        String[] strings = blacklist.split(BLACKLIST_DELIMITER);
        List<Integer> parsed = new ArrayList(strings.length);
        for (String string : strings) {
            String string2 = string2.trim();
            if (!BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(string2)) {
                int value = Integer.parseInt(string2);
                if (value >= 0) {
                    parsed.add(Integer.valueOf(value));
                } else {
                    throw new NumberFormatException("Negative value is invalid.");
                }
            }
        }
        return parsed;
    }
}
