package com.google.android.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.google.android.maps.Overlay.Snappable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class MyLocationOverlay extends Overlay implements SensorListener, LocationListener, Snappable {
    private static final String[] DESIRED_PROVIDER_NAMES = new String[]{"gps", "network"};
    private static final Paint LOCATION_ACCURACY_FILL_PAINT = new Paint();
    private static final Paint LOCATION_ACCURACY_STROKE_PAINT = new Paint();
    private Drawable mCompassArrow;
    private Drawable mCompassBase;
    private final Context mContext;
    private final MapController mController;
    private final ArrayList<NameAndDate> mEnabledProviders = new ArrayList(2);
    private volatile boolean mIsCompassEnabled = false;
    private volatile boolean mIsMyLocationEnabled = false;
    private volatile boolean mIsOnScreen = true;
    private volatile Location mLastFix = null;
    private volatile boolean mLocationChangedSinceLastDraw = false;
    private LevelListDrawable mLocationDot;
    private int mLocationDotHalfHeight;
    private int mLocationDotHalfWidth;
    private final MapView mMapView;
    private volatile GeoPoint mMyLocation = null;
    private volatile long mMyLocationTime;
    Location mNetworkLocation = null;
    Handler mNetworkLocationHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (MyLocationOverlay.this.mNetworkLocation != null) {
                MyLocationOverlay.this.onLocationChanged(MyLocationOverlay.this.mNetworkLocation);
            }
        }
    };
    private volatile float mOrientation = Float.NaN;
    private volatile GeoPoint mPreviousMyLocation = null;
    private final Queue<Runnable> mRunOnFirstFix = new LinkedList();
    private final Point mTempPoint = new Point();
    private final Rect mTempRect = new Rect();

    private static class NameAndDate {
        public long date = Long.MIN_VALUE;
        public String name;

        public NameAndDate(String name) {
            this.name = name;
        }
    }

    static {
        LOCATION_ACCURACY_FILL_PAINT.setARGB(30, 0, 0, 255);
        LOCATION_ACCURACY_FILL_PAINT.setStyle(Style.FILL);
        LOCATION_ACCURACY_STROKE_PAINT.setARGB(100, 0, 0, 255);
        LOCATION_ACCURACY_STROKE_PAINT.setStrokeWidth(2.5f);
        LOCATION_ACCURACY_STROKE_PAINT.setStyle(Style.STROKE);
        LOCATION_ACCURACY_STROKE_PAINT.setAntiAlias(true);
    }

    public MyLocationOverlay(Context context, MapView mapView) {
        if (mapView != null) {
            this.mContext = context;
            this.mMapView = mapView;
            this.mController = mapView.getController();
            return;
        }
        throw new IllegalArgumentException("mapView == null");
    }

    private LevelListDrawable getLocationDot() {
        if (this.mLocationDot == null) {
            this.mLocationDot = (LevelListDrawable) this.mContext.getResources().getDrawable(drawable.ic_maps_indicator_current_position_anim);
            this.mLocationDotHalfWidth = this.mLocationDot.getIntrinsicWidth() / 2;
            this.mLocationDotHalfHeight = this.mLocationDot.getIntrinsicHeight() / 2;
            this.mLocationDot.setBounds(-this.mLocationDotHalfWidth, -this.mLocationDotHalfHeight, this.mLocationDotHalfWidth, this.mLocationDotHalfHeight);
        }
        return this.mLocationDot;
    }

    private Drawable getCompassBase() {
        if (this.mCompassBase == null) {
            this.mCompassBase = this.mContext.getResources().getDrawable(drawable.compass_base);
            int w = this.mCompassBase.getIntrinsicWidth() / 2;
            int h = this.mCompassBase.getIntrinsicHeight() / 2;
            this.mCompassBase.setBounds(-w, -h, w, h);
        }
        return this.mCompassBase;
    }

    private Drawable getCompassArrow() {
        if (this.mCompassArrow == null) {
            this.mCompassArrow = this.mContext.getResources().getDrawable(drawable.compass_arrow);
            int w = this.mCompassArrow.getIntrinsicWidth() / 2;
            this.mCompassArrow.setBounds(-w, -28, w, this.mCompassArrow.getIntrinsicHeight() - 28);
        }
        return this.mCompassArrow;
    }

    public synchronized boolean enableCompass() {
        if (!this.mIsCompassEnabled) {
            SensorManager sm = (SensorManager) this.mContext.getSystemService("sensor");
            if (sm != null) {
                sm.registerListener(this, 128, 2);
                this.mIsCompassEnabled = true;
                this.mMapView.postInvalidate();
            } else {
                Log.w("Maps.MyLocationOverlay", "Compass SensorManager was unavailable.");
            }
        }
        return this.mIsCompassEnabled;
    }

    public synchronized void disableCompass() {
        if (this.mIsCompassEnabled) {
            SensorManager sm = (SensorManager) this.mContext.getSystemService("sensor");
            if (sm != null) {
                sm.unregisterListener(this, 128);
            }
            this.mMapView.postInvalidate();
            this.mIsCompassEnabled = false;
        }
    }

    public boolean isCompassEnabled() {
        return this.mIsCompassEnabled;
    }

    public synchronized boolean enableMyLocation() {
        StringBuilder stringBuilder;
        LocationManager service = (LocationManager) this.mContext.getSystemService("location");
        service.removeUpdates(this);
        this.mEnabledProviders.clear();
        this.mIsMyLocationEnabled = false;
        for (String name : DESIRED_PROVIDER_NAMES) {
            try {
                if (service.isProviderEnabled(name)) {
                    this.mIsMyLocationEnabled = true;
                    this.mEnabledProviders.add(new NameAndDate(name));
                    service.requestLocationUpdates(name, 0, 0.0f, this);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Request updates from ");
                    stringBuilder2.append(name);
                    Log.i("Maps.MyLocationOverlay", stringBuilder2.toString());
                }
            } catch (SecurityException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't get provider ");
                stringBuilder.append(name);
                stringBuilder.append(": ");
                stringBuilder.append(e.getMessage());
                Log.w("Maps.MyLocationOverlay", stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't get provider ");
                stringBuilder.append(name);
                stringBuilder.append(": ");
                stringBuilder.append(e2.getMessage());
                Log.w("Maps.MyLocationOverlay", stringBuilder.toString());
            }
        }
        if (!this.mIsMyLocationEnabled) {
            Log.w("Maps.MyLocationOverlay", "None of the desired Location Providers are available");
        }
        return this.mIsMyLocationEnabled;
    }

    public synchronized void disableMyLocation() {
        ((LocationManager) this.mContext.getSystemService("location")).removeUpdates(this);
        this.mEnabledProviders.clear();
        this.mIsMyLocationEnabled = false;
        this.mNetworkLocation = null;
        this.mNetworkLocationHandler.removeMessages(1);
    }

    public synchronized void onSensorChanged(int sensor, float[] values) {
        if (this.mIsCompassEnabled) {
            this.mOrientation = values[0];
            Rect r = getCompassBase().getBounds();
            this.mMapView.postInvalidate(r.left + 50, r.top + 58, r.right + 50, r.bottom + 58);
        }
    }

    private boolean isLocationOnScreen(MapView mapView, GeoPoint location) {
        Point tempPoint = new Point();
        mapView.getProjection().toPixels(location, tempPoint);
        Rect screen = new Rect();
        screen.set(0, 0, mapView.getWidth(), mapView.getHeight());
        return screen.contains(tempPoint.x, tempPoint.y);
    }

    public synchronized void onLocationChanged(Location location) {
        if (location.getProvider().equals("network")) {
            this.mNetworkLocationHandler.removeMessages(1);
            if (this.mNetworkLocation == null) {
                this.mNetworkLocation = new Location(location);
            } else {
                this.mNetworkLocation.set(location);
            }
            this.mNetworkLocationHandler.sendMessageDelayed(this.mNetworkLocationHandler.obtainMessage(1), 15000);
        }
        if (this.mIsMyLocationEnabled) {
            long now = SystemClock.elapsedRealtime();
            long then = now - 10000;
            String name = location.getProvider();
            Iterator it = this.mEnabledProviders.iterator();
            while (it.hasNext()) {
                NameAndDate provider = (NameAndDate) it.next();
                if (provider.name.equals(name)) {
                    provider.date = now;
                    break;
                } else if (provider.name.equals("gps") && provider.date > then) {
                    Log.i("Maps.MyLocationOverlay", "Got fallback update soon after preferred udpate, ignoring");
                    return;
                }
            }
            this.mLocationChangedSinceLastDraw = true;
            this.mPreviousMyLocation = this.mMyLocation;
            this.mMyLocation = new GeoPoint((int) (location.getLatitude() * 1000000.0d), (int) (location.getLongitude() * 1000000.0d));
            this.mMyLocationTime = SystemClock.elapsedRealtime();
            this.mLastFix = location;
            if (isLocationOnScreen(this.mMapView, this.mMyLocation)) {
                this.mMapView.postInvalidate();
            }
            while (true) {
                Runnable runnable = (Runnable) this.mRunOnFirstFix.poll();
                Runnable runnable2 = runnable;
                if (runnable != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Running deferred on first fix: ");
                    stringBuilder.append(runnable2);
                    Log.i("Maps.MyLocationOverlay", stringBuilder.toString());
                    new Thread(runnable2).start();
                } else {
                    return;
                }
            }
        }
    }

    private void clearNetworkLocationRebroadcasts() {
        this.mNetworkLocation = null;
        this.mNetworkLocationHandler.removeMessages(1);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (provider.equals("network") && status != 2) {
            clearNetworkLocationRebroadcasts();
        }
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
        if (provider.equals("network")) {
            clearNetworkLocationRebroadcasts();
        }
    }

    public boolean onSnapToItem(int x, int y, Point snapPoint, MapView mapView) {
        if (!isCloseToPoint(x, y, mapView)) {
            return false;
        }
        snapPoint.x = this.mTempPoint.x;
        snapPoint.y = this.mTempPoint.y;
        return true;
    }

    public boolean onTap(GeoPoint p, MapView map) {
        if (this.mMyLocation == null) {
            return false;
        }
        map.getProjection().toPixels(p, this.mTempPoint);
        if (!isCloseToPoint(this.mTempPoint.x, this.mTempPoint.y, map)) {
            return false;
        }
        dispatchTap();
        return true;
    }

    private boolean isCloseToPoint(int x, int y, MapView mapView) {
        boolean z = false;
        if (this.mMyLocation == null) {
            return false;
        }
        mapView.getProjection().toPixels(this.mMyLocation, this.mTempPoint);
        long dx = Math.abs(((long) x) - ((long) this.mTempPoint.x));
        long dy = Math.abs(((long) y) - ((long) this.mTempPoint.y));
        if (((float) ((dx * dx) + (dy * dy))) < 32.0f * 32.0f) {
            z = true;
        }
        return z;
    }

    protected boolean dispatchTap() {
        return false;
    }

    /* JADX WARNING: Missing block: B:18:0x003e, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
        if (shadow) {
            return false;
        }
        if (this.mMyLocation != null) {
            if (SystemClock.elapsedRealtime() - this.mMyLocationTime < 60000) {
                drawMyLocation(canvas, mapView, this.mLastFix, this.mMyLocation, when);
            } else {
                this.mMyLocation = null;
                this.mMapView.postInvalidate();
            }
        }
        if (this.mIsCompassEnabled && !Float.isNaN(this.mOrientation)) {
            drawCompass(canvas, this.mOrientation);
        }
    }

    private float isect(float c, float radius, float isect) {
        float disc = (((radius * radius) - (c * c)) + ((2.0f * c) * isect)) - (isect * isect);
        if (disc > 0.0f) {
            return (float) Math.sqrt((double) disc);
        }
        return 0.0f;
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x0157  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void drawMyLocation(Canvas canvas, MapView mapView, Location lastFix, GeoPoint myLocation, long when) {
        Canvas canvas2 = canvas;
        if (this.mIsMyLocationEnabled) {
            int height;
            int width;
            Projection converter;
            LevelListDrawable locationDot = getLocationDot();
            Rect bounds = locationDot.getBounds();
            Projection converter2 = mapView.getProjection();
            converter2.toPixels(myLocation, this.mTempPoint);
            int x = this.mTempPoint.x;
            int y = this.mTempPoint.y;
            float radius = 0.0f;
            if (lastFix.hasAccuracy()) {
                radius = converter2.metersToEquatorPixels((float) ((int) lastFix.getAccuracy()));
            }
            float radius2 = radius;
            int level = (((int) (when % 1000)) * 10000) / 1000;
            locationDot.setLevel(level);
            int width2 = mapView.getWidth();
            int height2 = mapView.getHeight();
            int i;
            if (radius2 > 0.0f) {
                float lineCenter;
                canvas2.drawCircle((float) x, (float) y, radius2, LOCATION_ACCURACY_FILL_PAINT);
                canvas2.drawCircle((float) x, (float) y, radius2, LOCATION_ACCURACY_STROKE_PAINT);
                float halfChord = isect((float) y, radius2, 1.0f);
                if (halfChord > 0.0f) {
                    lineCenter = 1.0f;
                    height = height2;
                    width = width2;
                    canvas2.drawLine(((float) x) - halfChord, 1.0f, ((float) x) + halfChord, lineCenter, LOCATION_ACCURACY_STROKE_PAINT);
                } else {
                    lineCenter = 1.0f;
                    height = height2;
                    width = width2;
                    i = level;
                }
                float lineCenter2 = lineCenter;
                halfChord = isect((float) y, radius2, ((float) height) - lineCenter2);
                if (halfChord > 0.0f) {
                    converter = converter2;
                    converter2 = lineCenter2;
                    canvas2.drawLine(((float) x) - halfChord, ((float) height) - lineCenter2, ((float) x) + halfChord, ((float) height) - lineCenter2, LOCATION_ACCURACY_STROKE_PAINT);
                } else {
                    converter = converter2;
                    converter2 = lineCenter2;
                }
                halfChord = isect((float) x, radius2, converter2);
                if (halfChord > 0.0f) {
                    canvas2.drawLine(converter2, ((float) y) - halfChord, converter2, ((float) y) + halfChord, LOCATION_ACCURACY_STROKE_PAINT);
                }
                halfChord = isect((float) x, radius2, ((float) width) - converter2);
                if (halfChord > 0.0f) {
                    canvas2.drawLine(((float) width) - converter2, ((float) y) - halfChord, ((float) width) - converter2, ((float) y) + halfChord, LOCATION_ACCURACY_STROKE_PAINT);
                }
            } else {
                height = height2;
                width = width2;
                i = level;
                converter = converter2;
            }
            Overlay.drawAt(canvas2, locationDot, x, y, false);
            this.mTempRect.set(0, 0, width, height);
            this.mIsOnScreen = this.mTempRect.intersects(bounds.left + x, bounds.top + y, bounds.right + x, bounds.bottom + y);
            if (this.mLocationChangedSinceLastDraw && this.mController != null) {
                this.mTempRect.inset(width / 20, height / 20);
                if (!this.mTempRect.contains(x, y)) {
                    Projection converter3;
                    boolean wasOnScreen = false;
                    if (this.mPreviousMyLocation != null) {
                        converter3 = converter;
                        converter3.toPixels(this.mPreviousMyLocation, this.mTempPoint);
                        wasOnScreen = this.mTempRect.contains(this.mTempPoint.x, this.mTempPoint.y);
                    } else {
                        converter3 = converter;
                    }
                    if (wasOnScreen) {
                        converter3.toPixels(this.mMyLocation, this.mTempPoint);
                        this.mController.animateTo(this.mMyLocation);
                    }
                    if (this.mIsOnScreen) {
                        int w = this.mLocationDotHalfWidth;
                        int h = this.mLocationDotHalfHeight;
                        this.mMapView.postInvalidateDelayed(250, x - w, y - h, x + w, y + h);
                    }
                    this.mLocationChangedSinceLastDraw = false;
                }
            }
            if (this.mIsOnScreen) {
            }
            this.mLocationChangedSinceLastDraw = false;
        }
    }

    protected void drawCompass(Canvas canvas, float bearing) {
        canvas.save();
        canvas.translate(50.0f, 58.0f);
        Overlay.drawAt(canvas, getCompassBase(), 0, 0, false);
        canvas.rotate(-bearing);
        Overlay.drawAt(canvas, getCompassArrow(), 0, 0, false);
        canvas.restore();
    }

    public GeoPoint getMyLocation() {
        return this.mMyLocation;
    }

    public Location getLastFix() {
        return this.mLastFix;
    }

    public float getOrientation() {
        return this.mOrientation;
    }

    public boolean isMyLocationEnabled() {
        return this.mIsMyLocationEnabled;
    }

    public synchronized boolean runOnFirstFix(Runnable runnable) {
        if (this.mMyLocation != null) {
            runnable.run();
            return true;
        }
        this.mRunOnFirstFix.offer(runnable);
        return false;
    }

    public void onAccuracyChanged(int sensor, int accuracy) {
    }
}
