package com.android.server.wm;

import android.content.ClipData;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Trace;
import android.os.UserHandle;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.DisplayCutout.ParcelableWrapper;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowSession.Stub;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowManager.LayoutParams;
import com.android.internal.os.logging.MetricsLoggerWrapper;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

class Session extends Stub implements DeathRecipient {
    private AlertWindowNotification mAlertWindowNotification;
    private final Set<WindowSurfaceController> mAlertWindowSurfaces = new HashSet();
    private final Set<WindowSurfaceController> mAppOverlaySurfaces = new HashSet();
    final IWindowSessionCallback mCallback;
    final boolean mCanAcquireSleepToken;
    final boolean mCanAddInternalSystemWindow;
    final boolean mCanHideNonSystemOverlayWindows;
    final IInputMethodClient mClient;
    private boolean mClientDead = false;
    private final DragDropController mDragDropController;
    private float mLastReportedAnimatorScale;
    private int mNumWindow = 0;
    private String mPackageName;
    final int mPid;
    private String mRelayoutTag;
    final WindowManagerService mService;
    private boolean mShowingAlertWindowNotificationAllowed;
    private final String mStringName;
    SurfaceSession mSurfaceSession;
    final int mUid;

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0106 A:{Splitter: B:24:0x00e9, ExcHandler: all (th java.lang.Throwable)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:36:0x0117, code:
            android.os.Binder.restoreCallingIdentity(r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Session(WindowManagerService service, IWindowSessionCallback callback, IInputMethodClient client, IInputContext inputContext) {
        this.mService = service;
        this.mCallback = callback;
        this.mClient = client;
        this.mUid = Binder.getCallingUid();
        this.mPid = Binder.getCallingPid();
        this.mLastReportedAnimatorScale = service.getCurrentAnimatorScale();
        boolean z = true;
        this.mCanAddInternalSystemWindow = service.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") == 0;
        this.mCanHideNonSystemOverlayWindows = service.mContext.checkCallingOrSelfPermission("android.permission.HIDE_NON_SYSTEM_OVERLAY_WINDOWS") == 0;
        if (service.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0) {
            z = false;
        }
        this.mCanAcquireSleepToken = z;
        this.mShowingAlertWindowNotificationAllowed = this.mService.mShowAlertWindowNotifications;
        this.mDragDropController = this.mService.mDragDropController;
        StringBuilder sb = new StringBuilder();
        sb.append("Session{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" ");
        sb.append(this.mPid);
        if (this.mUid < 10000) {
            sb.append(":");
            sb.append(this.mUid);
        } else {
            sb.append(":u");
            sb.append(UserHandle.getUserId(this.mUid));
            sb.append('a');
            sb.append(UserHandle.getAppId(this.mUid));
        }
        sb.append("}");
        this.mStringName = sb.toString();
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mService.mInputMethodManager == null && this.mService.mHaveInputMethods) {
                    this.mService.mInputMethodManager = IInputMethodManager.Stub.asInterface(ServiceManager.getService("input_method"));
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mService.mInputMethodManager != null) {
                this.mService.mInputMethodManager.addClient(client, inputContext, this.mUid, this.mPid);
            } else {
                client.setUsingInputMethod(false);
            }
            client.asBinder().linkToDeath(this, 0);
        } catch (RemoteException e) {
            if (this.mService.mInputMethodManager != null) {
                this.mService.mInputMethodManager.removeClient(client);
            }
        } catch (Throwable th) {
        }
        Binder.restoreCallingIdentity(ident);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf("WindowManager", "Window Session Crash", e);
            }
            throw e;
        }
    }

    public void binderDied() {
        try {
            if (this.mService.mInputMethodManager != null) {
                this.mService.mInputMethodManager.removeClient(this.mClient);
            }
        } catch (RemoteException e) {
        }
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mClient.asBinder().unlinkToDeath(this, 0);
                this.mClientDead = true;
                killSessionLocked();
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public int add(IWindow window, int seq, LayoutParams attrs, int viewVisibility, Rect outContentInsets, Rect outStableInsets, InputChannel outInputChannel) {
        return addToDisplay(window, seq, attrs, viewVisibility, 0, new Rect(), outContentInsets, outStableInsets, null, new ParcelableWrapper(), outInputChannel);
    }

    public int addToDisplay(IWindow window, int seq, LayoutParams attrs, int viewVisibility, int displayId, Rect outFrame, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, ParcelableWrapper outDisplayCutout, InputChannel outInputChannel) {
        return this.mService.addWindow(this, window, seq, attrs, viewVisibility, displayId, outFrame, outContentInsets, outStableInsets, outOutsets, outDisplayCutout, outInputChannel);
    }

    public int addWithoutInputChannel(IWindow window, int seq, LayoutParams attrs, int viewVisibility, Rect outContentInsets, Rect outStableInsets) {
        return addToDisplayWithoutInputChannel(window, seq, attrs, viewVisibility, 0, outContentInsets, outStableInsets);
    }

    public int addToDisplayWithoutInputChannel(IWindow window, int seq, LayoutParams attrs, int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets) {
        return this.mService.addWindow(this, window, seq, attrs, viewVisibility, displayId, new Rect(), outContentInsets, outStableInsets, null, new ParcelableWrapper(), null);
    }

    public void remove(IWindow window) {
        this.mService.removeWindow(this, window);
    }

    public void prepareToReplaceWindows(IBinder appToken, boolean childrenOnly) {
        this.mService.setWillReplaceWindows(appToken, childrenOnly);
    }

    public int relayout(IWindow window, int seq, LayoutParams attrs, int requestedWidth, int requestedHeight, int viewFlags, int flags, long frameNumber, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Rect outsets, Rect outBackdropFrame, ParcelableWrapper cutout, MergedConfiguration mergedConfiguration, Surface outSurface) {
        if (this.mRelayoutTag != null) {
            Trace.traceBegin(32, this.mRelayoutTag);
        }
        int res = this.mService.relayoutWindow(this, window, seq, attrs, requestedWidth, requestedHeight, viewFlags, flags, frameNumber, outFrame, outOverscanInsets, outContentInsets, outVisibleInsets, outStableInsets, outsets, outBackdropFrame, cutout, mergedConfiguration, outSurface);
        if (this.mRelayoutTag != null) {
            Trace.traceEnd(32);
        }
        return res;
    }

    public boolean outOfMemory(IWindow window) {
        return this.mService.outOfMemoryWindow(this, window);
    }

    public void setTransparentRegion(IWindow window, Region region) {
        this.mService.setTransparentRegionWindow(this, window, region);
    }

    public void setInsets(IWindow window, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableArea) {
        this.mService.setInsetsWindow(this, window, touchableInsets, contentInsets, visibleInsets, touchableArea);
    }

    public void getDisplayFrame(IWindow window, Rect outDisplayFrame) {
        this.mService.getWindowDisplayFrame(this, window, outDisplayFrame);
    }

    public void finishDrawing(IWindow window) {
        this.mService.finishDrawingWindow(this, window);
    }

    public void setInTouchMode(boolean mode) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mInTouchMode = mode;
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean getInTouchMode() {
        boolean z;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = this.mService.mInTouchMode;
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return z;
    }

    public boolean performHapticFeedback(IWindow window, int effectId, boolean always) {
        boolean performHapticFeedbackLw;
        synchronized (this.mService.mWindowMap) {
            long ident;
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ident = Binder.clearCallingIdentity();
                performHapticFeedbackLw = this.mService.mPolicy.performHapticFeedbackLw(this.mService.windowForClientLocked(this, window, true), effectId, always);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return performHapticFeedbackLw;
    }

    public IBinder performDrag(IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) {
        Throwable th;
        long ident;
        int callerPid = Binder.getCallingPid();
        int callerUid = Binder.getCallingUid();
        long ident2 = Binder.clearCallingIdentity();
        try {
            long ident3 = ident2;
            try {
                IBinder performDrag = this.mDragDropController.performDrag(this.mSurfaceSession, callerPid, callerUid, window, flags, surface, touchSource, touchX, touchY, thumbCenterX, thumbCenterY, data);
                Binder.restoreCallingIdentity(ident3);
                return performDrag;
            } catch (Throwable th2) {
                th = th2;
                ident = ident3;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            ident = ident2;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public void reportDropResult(IWindow window, boolean consumed) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mDragDropController.reportDropResult(window, consumed);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void cancelDragAndDrop(IBinder dragToken) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mDragDropController.cancelDragAndDrop(dragToken);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void dragRecipientEntered(IWindow window) {
        this.mDragDropController.dragRecipientEntered(window);
    }

    public void dragRecipientExited(IWindow window) {
        this.mDragDropController.dragRecipientExited(window);
    }

    public boolean startMovingTask(IWindow window, float startX, float startY) {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean startMovingTask = this.mService.mTaskPositioningController.startMovingTask(window, startX, startY);
            return startMovingTask;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setWallpaperPosition(IBinder window, float x, float y, float xStep, float yStep) {
        synchronized (this.mService.mWindowMap) {
            long ident;
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ident = Binder.clearCallingIdentity();
                this.mService.mRoot.mWallpaperController.setWindowWallpaperPosition(this.mService.windowForClientLocked(this, window, true), x, y, xStep, yStep);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void wallpaperOffsetsComplete(IBinder window) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mRoot.mWallpaperController.wallpaperOffsetsComplete(window);
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void setWallpaperDisplayOffset(IBinder window, int x, int y) {
        synchronized (this.mService.mWindowMap) {
            long ident;
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ident = Binder.clearCallingIdentity();
                this.mService.mRoot.mWallpaperController.setWindowWallpaperDisplayOffset(this.mService.windowForClientLocked(this, window, true), x, y);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public Bundle sendWallpaperCommand(IBinder window, String action, int x, int y, int z, Bundle extras, boolean sync) {
        Throwable th;
        IBinder iBinder;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                try {
                    try {
                        Bundle sendWindowWallpaperCommand = this.mService.mRoot.mWallpaperController.sendWindowWallpaperCommand(this.mService.windowForClientLocked(this, window, true), action, x, y, z, extras, sync);
                        Binder.restoreCallingIdentity(ident);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return sendWindowWallpaperCommand;
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(ident);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    iBinder = window;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void wallpaperCommandComplete(IBinder window, Bundle result) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mRoot.mWallpaperController.wallpaperCommandComplete(window);
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) {
        synchronized (this.mService.mWindowMap) {
            long identity;
            try {
                WindowManagerService.boostPriorityForLockedSection();
                identity = Binder.clearCallingIdentity();
                this.mService.onRectangleOnScreenRequested(token, rectangle);
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public IWindowId getWindowId(IBinder window) {
        return this.mService.getWindowId(window);
    }

    public void pokeDrawLock(IBinder window) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mService.pokeDrawLock(this, window);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updatePointerIcon(IWindow window) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mService.updatePointerIcon(window);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updateTapExcludeRegion(IWindow window, int regionId, int left, int top, int width, int height) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mService.updateTapExcludeRegion(window, regionId, left, top, width, height);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    void windowAddedLocked(String packageName) {
        this.mPackageName = packageName;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("relayoutWindow: ");
        stringBuilder.append(this.mPackageName);
        this.mRelayoutTag = stringBuilder.toString();
        if (this.mSurfaceSession == null) {
            this.mSurfaceSession = new SurfaceSession();
            this.mService.mSessions.add(this);
            if (this.mLastReportedAnimatorScale != this.mService.getCurrentAnimatorScale()) {
                this.mService.dispatchNewAnimatorScaleLocked(this);
            }
        }
        this.mNumWindow++;
    }

    void windowRemovedLocked() {
        this.mNumWindow--;
        killSessionLocked();
    }

    void onWindowSurfaceVisibilityChanged(WindowSurfaceController surfaceController, boolean visible, int type) {
        if (LayoutParams.isSystemAlertWindowType(type)) {
            if (!this.mCanAddInternalSystemWindow) {
                boolean changed;
                if (visible) {
                    changed = this.mAlertWindowSurfaces.add(surfaceController);
                    MetricsLoggerWrapper.logAppOverlayEnter(this.mUid, this.mPackageName, changed, type, true);
                } else {
                    changed = this.mAlertWindowSurfaces.remove(surfaceController);
                    MetricsLoggerWrapper.logAppOverlayExit(this.mUid, this.mPackageName, changed, type, true);
                }
                if (changed) {
                    if (this.mAlertWindowSurfaces.isEmpty()) {
                        cancelAlertWindowNotification();
                    } else if (this.mAlertWindowNotification == null) {
                        this.mAlertWindowNotification = new AlertWindowNotification(this.mService, this.mPackageName);
                        if (this.mShowingAlertWindowNotificationAllowed) {
                            this.mAlertWindowNotification.post();
                        }
                    }
                }
            }
            if (type == 2038) {
                boolean changed2;
                if (visible) {
                    changed2 = this.mAppOverlaySurfaces.add(surfaceController);
                    MetricsLoggerWrapper.logAppOverlayEnter(this.mUid, this.mPackageName, changed2, type, false);
                } else {
                    changed2 = this.mAppOverlaySurfaces.remove(surfaceController);
                    MetricsLoggerWrapper.logAppOverlayExit(this.mUid, this.mPackageName, changed2, type, false);
                }
                if (changed2) {
                    setHasOverlayUi(this.mAppOverlaySurfaces.isEmpty() ^ true);
                }
            }
        }
    }

    void setShowingAlertWindowNotificationAllowed(boolean allowed) {
        this.mShowingAlertWindowNotificationAllowed = allowed;
        if (this.mAlertWindowNotification == null) {
            return;
        }
        if (allowed) {
            this.mAlertWindowNotification.post();
        } else {
            this.mAlertWindowNotification.cancel(false);
        }
    }

    private void killSessionLocked() {
        if (this.mNumWindow <= 0 && this.mClientDead) {
            this.mService.mSessions.remove(this);
            if (this.mSurfaceSession != null) {
                try {
                    this.mSurfaceSession.kill();
                } catch (Exception e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception thrown when killing surface session ");
                    stringBuilder.append(this.mSurfaceSession);
                    stringBuilder.append(" in session ");
                    stringBuilder.append(this);
                    stringBuilder.append(": ");
                    stringBuilder.append(e.toString());
                    Slog.w("WindowManager", stringBuilder.toString());
                }
                this.mSurfaceSession = null;
                this.mAlertWindowSurfaces.clear();
                this.mAppOverlaySurfaces.clear();
                setHasOverlayUi(false);
                cancelAlertWindowNotification();
            }
        }
    }

    private void setHasOverlayUi(boolean hasOverlayUi) {
        this.mService.mH.obtainMessage(58, this.mPid, hasOverlayUi).sendToTarget();
    }

    private void cancelAlertWindowNotification() {
        if (this.mAlertWindowNotification != null) {
            this.mAlertWindowNotification.cancel(true);
            this.mAlertWindowNotification = null;
        }
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mNumWindow=");
        pw.print(this.mNumWindow);
        pw.print(" mCanAddInternalSystemWindow=");
        pw.print(this.mCanAddInternalSystemWindow);
        pw.print(" mAppOverlaySurfaces=");
        pw.print(this.mAppOverlaySurfaces);
        pw.print(" mAlertWindowSurfaces=");
        pw.print(this.mAlertWindowSurfaces);
        pw.print(" mClientDead=");
        pw.print(this.mClientDead);
        pw.print(" mSurfaceSession=");
        pw.println(this.mSurfaceSession);
        pw.print(prefix);
        pw.print("mPackageName=");
        pw.println(this.mPackageName);
    }

    public String toString() {
        return this.mStringName;
    }

    boolean hasAlertWindowSurfaces() {
        return this.mAlertWindowSurfaces.isEmpty() ^ 1;
    }
}
