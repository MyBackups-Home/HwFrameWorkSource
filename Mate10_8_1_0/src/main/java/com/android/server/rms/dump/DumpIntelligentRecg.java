package com.android.server.rms.dump;

import android.content.Context;
import android.util.Slog;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import java.io.PrintWriter;
import junit.framework.Assert;

public final class DumpIntelligentRecg extends Assert {
    private static final int DUMP_BLUETOOTH = 3;
    private static final int DUMP_CHECKVISIBLEWINDOW = 4;
    private static final int DUMP_CONTINUE = 1;
    private static final int DUMP_FROZEN = 2;
    private static final int DUMP_RETURN = 0;
    private static final String TAG = "TestIntelligentRecg";

    public static final void dumpIntelligentRecg(PrintWriter pw, Context context, String[] args) {
        int userId = 0;
        String pkg = null;
        int type = 3;
        int ret = 0;
        if (pw != null && context != null && args != null) {
            boolean hasUid = false;
            boolean hasUserId = false;
            boolean hasPkg = false;
            boolean hasType = false;
            boolean hasCheckVw = false;
            int i = 0;
            int length = args.length;
            while (i < length) {
                String arg = args[i];
                if (hasUid) {
                    if (ret == 2) {
                        try {
                            AwareIntelligentRecg.getInstance().dumpFrozen(pw, Integer.parseInt(arg));
                        } catch (NumberFormatException e) {
                            Slog.e(TAG, "dump args is illegal!");
                        }
                    } else if (ret == 3) {
                        AwareIntelligentRecg.getInstance().dumpBluetooth(pw, Integer.parseInt(arg));
                    }
                    return;
                }
                if (hasCheckVw) {
                    if (hasUserId) {
                        if (!hasPkg) {
                            hasPkg = true;
                            pkg = arg;
                        } else if (!hasType) {
                            hasType = true;
                            type = Integer.parseInt(arg);
                        }
                        i++;
                    } else {
                        hasUserId = true;
                        try {
                            userId = Integer.parseInt(arg);
                        } catch (NumberFormatException e2) {
                            Slog.e(TAG, "dump args is illegal!");
                        }
                        i++;
                    }
                }
                ret = dumpDump(pw, arg);
                if (ret != 0) {
                    if (ret == 2 || ret == 3) {
                        hasUid = true;
                        i++;
                    } else {
                        if (ret == 4) {
                            hasCheckVw = true;
                        }
                        i++;
                    }
                } else {
                    return;
                }
            }
            if (hasCheckVw) {
                AwareIntelligentRecg.getInstance().dumpIsVisibleWindow(pw, userId, pkg, type);
            }
        }
    }

    private static final int dumpDump(PrintWriter pw, String arg) {
        if ("enable".equals(arg)) {
            AwareIntelligentRecg.commEnable();
            return 0;
        } else if ("disable".equals(arg)) {
            AwareIntelligentRecg.commDisable();
            return 0;
        } else if ("enable_log".equals(arg)) {
            AwareIntelligentRecg.enableDebug();
            return 0;
        } else if ("disable_log".equals(arg)) {
            AwareIntelligentRecg.disableDebug();
            return 0;
        } else if ("input".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpInputMethod(pw);
            return 0;
        } else if ("access".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpAccessibility(pw);
            return 0;
        } else if ("noclean".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpNotClean(pw);
            return 0;
        } else if (!"gmscaller".equals(arg)) {
            return dumpDumpEx(pw, arg);
        } else {
            AwareIntelligentRecg.getInstance().dumpGmsCallerList(pw);
            return 0;
        }
    }

    private static final int dumpDumpEx(PrintWriter pw, String arg) {
        int ret = 0;
        if ("tts".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpTts(pw);
        } else if ("push".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpPushSDK(pw);
        } else if ("wallpaper".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpWallpaper(pw);
        } else if ("toast".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpToastWindow(pw);
        } else if ("dtts".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpDefaultTts(pw);
        } else if ("alarm".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpAlarms(pw);
        } else if ("alarmaction".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpAlarmActions(pw);
        } else if ("frozen".equals(arg)) {
            ret = 2;
        } else if ("bluetooth".equals(arg)) {
            ret = 3;
        } else if ("fa".equals(arg)) {
            AwareFakeActivityRecg.self().dumpRecgFakeActivity(pw, null);
        } else if ("kbg".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpKbgApp(pw);
        } else if ("apptype".equals(arg)) {
            AwareIntelligentRecg.getInstance().dumpDefaultAppType(pw);
        } else if ("checkvw".equals(arg)) {
            return 4;
        } else {
            if ("hwstop".equals(arg)) {
                AwareIntelligentRecg.getInstance().dumpHwStopList(pw);
            } else if ("camerarecord".equals(arg)) {
                AwareIntelligentRecg.getInstance().dumpCameraRecording(pw);
            } else if ("screenrecord".equals(arg)) {
                AwareIntelligentRecg.getInstance().dumpScreenRecording(pw);
            } else {
                ret = 1;
            }
        }
        return ret;
    }
}
