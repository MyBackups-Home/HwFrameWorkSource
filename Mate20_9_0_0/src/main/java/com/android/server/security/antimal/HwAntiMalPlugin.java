package com.android.server.security.antimal;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.core.IHwSecurityPlugin.Creator;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.android.security.IHwAntiMalPlugin.Stub;

public class HwAntiMalPlugin extends Stub implements IHwSecurityPlugin {
    private static final String ANTIMAL_KEY_LAUNCHER_NAME = "launcher_name";
    private static final String ANTIMAL_KEY_PROTECT_TYPE = "protect_type";
    private static final int ANTIMAL_PROTECT_TYPE_DEFAULT = 0;
    private static final int ANTIMAL_PROTECT_TYPE_LAUNCHER = 2;
    private static final int ANTIMAL_PROTECT_TYPE_NEW_DIVECE = 1;
    public static final Creator CREATOR = new Creator() {
        public IHwSecurityPlugin createPlugin(Context context) {
            Slog.d(HwAntiMalPlugin.TAG, "createPlugin");
            return new HwAntiMalPlugin(context);
        }

        public String getPluginPermission() {
            return HwAntiMalPlugin.MANAGE_USE_SECURITY;
        }
    };
    private static final String DEFAULT_HW_LAUNCHER = "com.huawei.android.launcher";
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final boolean IS_DOMESTIC_RELEASE = "1".equalsIgnoreCase(SystemProperties.get("ro.logsystem.usertype", "3"));
    private static final boolean IS_ROOT = "0".equalsIgnoreCase(SystemProperties.get("ro.secure", "1"));
    private static final String MANAGE_USE_SECURITY = "com.huawei.permission.MANAGE_USE_SECURITY";
    private static final String TAG = "HwAntiMalPlugin";
    private static final boolean mAntimalProtection = "true".equalsIgnoreCase(SystemProperties.get("ro.product.antimal_protection", "true"));
    private Context mContext;
    private HwAntiMalStatus mHwAntiMalStatus = null;

    public HwAntiMalPlugin(Context context) {
        this.mContext = context;
        this.mHwAntiMalStatus = new HwAntiMalStatus(this.mContext);
        this.mHwAntiMalStatus.checkIfTopAppsDisabled();
    }

    public IBinder asBinder() {
        return this;
    }

    public void onStart() {
    }

    public void onStop() {
    }

    public boolean isAntiMalProtectionOn(Bundle params) {
        if (params == null) {
            Slog.e(TAG, "Invalid input params!");
            return false;
        } else if (!IS_CHINA_AREA) {
            Slog.d(TAG, "Not in valid area!");
            return false;
        } else if (mAntimalProtection) {
            int protect_type = params.getInt(ANTIMAL_KEY_PROTECT_TYPE, 0);
            String launcher = params.getString(ANTIMAL_KEY_LAUNCHER_NAME, "");
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("protect_type:");
            stringBuilder.append(protect_type);
            stringBuilder.append(" launcher:");
            stringBuilder.append(launcher);
            Slog.d(str, stringBuilder.toString());
            return isAntiMalProtectionOnByType(protect_type, launcher);
        } else {
            Slog.d(TAG, "AntimalProtection control is close!");
            return false;
        }
    }

    private boolean isAntiMalProtectionOnByType(int type, String launcher) {
        if (type == 2) {
            if (TextUtils.isEmpty(launcher)) {
                Slog.d(TAG, "Input is empty!");
                launcher = "com.huawei.android.launcher";
            }
            try {
                if (this.mHwAntiMalStatus.isAllowedSetHomeActivityForAntiMal(this.mContext.getPackageManager().getPackageInfo(launcher, 0), 0, false)) {
                    return false;
                }
            } catch (NameNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not found name:");
                stringBuilder.append(launcher);
                Slog.d(str, stringBuilder.toString());
                return true;
            }
        } else if (IS_ROOT) {
            Slog.d(TAG, "Device is root !");
            return false;
        } else if (!IS_DOMESTIC_RELEASE) {
            Slog.d(TAG, "Beta version !");
            return false;
        } else if (type == 1) {
            return this.mHwAntiMalStatus.isNeedRestrictForAntimal(true);
        }
        return true;
    }
}
