package android.support.v4.graphics;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.Typeface.Builder;
import android.graphics.fonts.FontVariationAxis;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.provider.FontsContractCompat.FontInfo;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

@RequiresApi(26)
@RestrictTo({Scope.LIBRARY_GROUP})
public class TypefaceCompatApi26Impl extends TypefaceCompatApi21Impl {
    private static final String ABORT_CREATION_METHOD = "abortCreation";
    private static final String ADD_FONT_FROM_ASSET_MANAGER_METHOD = "addFontFromAssetManager";
    private static final String ADD_FONT_FROM_BUFFER_METHOD = "addFontFromBuffer";
    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD = "createFromFamiliesWithDefault";
    private static final String DEFAULT_FAMILY = "sans-serif";
    private static final String FONT_FAMILY_CLASS = "android.graphics.FontFamily";
    private static final String FREEZE_METHOD = "freeze";
    private static final int RESOLVE_BY_FONT_TABLE = -1;
    private static final String TAG = "TypefaceCompatApi26Impl";
    protected final Method mAbortCreation;
    protected final Method mAddFontFromAssetManager;
    protected final Method mAddFontFromBuffer;
    protected final Method mCreateFromFamiliesWithDefault;
    protected final Class mFontFamily;
    protected final Constructor mFontFamilyCtor;
    protected final Method mFreeze;

    /* JADX WARNING: Removed duplicated region for block: B:4:0x0029 A:{Splitter: B:1:0x0003, ExcHandler: java.lang.ClassNotFoundException (r0_2 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:4:0x0029, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x002a, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Unable to collect necessary methods for class ");
            r2.append(r0.getClass().getName());
            android.util.Log.e(r1, r2.toString(), r0);
            r1 = null;
            r2 = null;
            r3 = null;
            r4 = null;
            r5 = null;
            r6 = null;
            r0 = null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public TypefaceCompatApi26Impl() {
        Method addFontFromBuffer;
        Method freeze;
        Method abortCreation;
        Class fontFamily;
        Method createFromFamiliesWithDefault;
        Method abortCreation2;
        Constructor fontFamilyCtor;
        try {
            Class fontFamily2 = obtainFontFamily();
            Constructor fontFamilyCtor2 = obtainFontFamilyCtor(fontFamily2);
            Method addFontFromAssetManager = obtainAddFontFromAssetManagerMethod(fontFamily2);
            addFontFromBuffer = obtainAddFontFromBufferMethod(fontFamily2);
            freeze = obtainFreezeMethod(fontFamily2);
            abortCreation = obtainAbortCreationMethod(fontFamily2);
            Constructor constructor = fontFamilyCtor2;
            fontFamily = fontFamily2;
            createFromFamiliesWithDefault = obtainCreateFromFamiliesWithDefaultMethod(fontFamily2);
            abortCreation2 = abortCreation;
            abortCreation = freeze;
            freeze = addFontFromBuffer;
            addFontFromBuffer = addFontFromAssetManager;
            fontFamilyCtor = constructor;
        } catch (ReflectiveOperationException e) {
        }
        this.mFontFamily = fontFamily;
        this.mFontFamilyCtor = fontFamilyCtor;
        this.mAddFontFromAssetManager = addFontFromBuffer;
        this.mAddFontFromBuffer = freeze;
        this.mFreeze = abortCreation;
        this.mAbortCreation = abortCreation2;
        this.mCreateFromFamiliesWithDefault = createFromFamiliesWithDefault;
    }

    private boolean isFontFamilyPrivateAPIAvailable() {
        if (this.mAddFontFromAssetManager == null) {
            Log.w(TAG, "Unable to collect necessary private methods. Fallback to legacy implementation.");
        }
        return this.mAddFontFromAssetManager != null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x000a A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalAccessException (r0_2 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x000a A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalAccessException (r0_2 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x000a, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0010, code:
            throw new java.lang.RuntimeException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Object newFamily() {
        try {
            return this.mFontFamilyCtor.newInstance(new Object[0]);
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0041 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalAccessException (r0_3 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x0041, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0047, code:
            throw new java.lang.RuntimeException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean addFontFromAssetManager(Context context, Object family, String fileName, int ttcIndex, int weight, int style, @Nullable FontVariationAxis[] axes) {
        try {
            return ((Boolean) this.mAddFontFromAssetManager.invoke(family, new Object[]{context.getAssets(), fileName, Integer.valueOf(0), Boolean.valueOf(false), Integer.valueOf(ttcIndex), Integer.valueOf(weight), Integer.valueOf(style), axes})).booleanValue();
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x002c A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalAccessException (r0_3 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x002c, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0032, code:
            throw new java.lang.RuntimeException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean addFontFromBuffer(Object family, ByteBuffer buffer, int ttcIndex, int weight, int style) {
        try {
            return ((Boolean) this.mAddFontFromBuffer.invoke(family, new Object[]{buffer, Integer.valueOf(ttcIndex), null, Integer.valueOf(weight), Integer.valueOf(style)})).booleanValue();
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0028 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalAccessException (r0_2 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x0028, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x002e, code:
            throw new java.lang.RuntimeException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Array.set(Array.newInstance(this.mFontFamily, 1), 0, family);
            return (Typeface) this.mCreateFromFamiliesWithDefault.invoke(null, new Object[]{familyArray, Integer.valueOf(-1), Integer.valueOf(-1)});
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0010 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalAccessException (r0_3 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x0010, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0016, code:
            throw new java.lang.RuntimeException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean freeze(Object family) {
        try {
            return ((Boolean) this.mFreeze.invoke(family, new Object[0])).booleanValue();
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x000a A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalAccessException (r0_1 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x000a, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0010, code:
            throw new java.lang.RuntimeException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void abortCreation(Object family) {
        try {
            this.mAbortCreation.invoke(family, new Object[0]);
        } catch (ReflectiveOperationException e) {
        }
    }

    public Typeface createFromFontFamilyFilesResourceEntry(Context context, FontFamilyFilesResourceEntry entry, Resources resources, int style) {
        if (!isFontFamilyPrivateAPIAvailable()) {
            return super.createFromFontFamilyFilesResourceEntry(context, entry, resources, style);
        }
        Object fontFamily = newFamily();
        FontFileResourceEntry[] entries = entry.getEntries();
        int length = entries.length;
        int i = 0;
        while (i < length) {
            FontFileResourceEntry fontFile = entries[i];
            if (addFontFromAssetManager(context, fontFamily, fontFile.getFileName(), fontFile.getTtcIndex(), fontFile.getWeight(), fontFile.isItalic(), FontVariationAxis.fromFontVariationSettings(fontFile.getVariationSettings()))) {
                i++;
            } else {
                abortCreation(fontFamily);
                return null;
            }
        }
        if (freeze(fontFamily)) {
            return createFromFamiliesWithDefault(fontFamily);
        }
        return null;
    }

    public Typeface createFromFontInfo(Context context, @Nullable CancellationSignal cancellationSignal, @NonNull FontInfo[] fonts, int style) {
        ParcelFileDescriptor pfd;
        Throwable th;
        Throwable th2;
        CancellationSignal cancellationSignal2 = cancellationSignal;
        FontInfo[] fontInfoArr = fonts;
        int i = style;
        if (fontInfoArr.length < 1) {
            return null;
        }
        if (isFontFamilyPrivateAPIAvailable()) {
            Map<Uri, ByteBuffer> uriBuffer = FontsContractCompat.prepareFontData(context, fontInfoArr, cancellationSignal2);
            Object fontFamily = newFamily();
            boolean atLeastOneFont = false;
            for (FontInfo font : fontInfoArr) {
                ByteBuffer fontBuffer = (ByteBuffer) uriBuffer.get(font.getUri());
                if (fontBuffer != null) {
                    if (addFontFromBuffer(fontFamily, fontBuffer, font.getTtcIndex(), font.getWeight(), font.isItalic())) {
                        atLeastOneFont = true;
                    } else {
                        abortCreation(fontFamily);
                        return null;
                    }
                }
            }
            if (!atLeastOneFont) {
                abortCreation(fontFamily);
                return null;
            } else if (freeze(fontFamily)) {
                return Typeface.create(createFromFamiliesWithDefault(fontFamily), i);
            } else {
                return null;
            }
        }
        FontInfo bestFont = findBestInfo(fontInfoArr, i);
        try {
            pfd = context.getContentResolver().openFileDescriptor(bestFont.getUri(), "r", cancellationSignal2);
            if (pfd == null) {
                if (pfd != null) {
                    pfd.close();
                }
                return null;
            }
            try {
                Typeface build = new Builder(pfd.getFileDescriptor()).setWeight(bestFont.getWeight()).setItalic(bestFont.isItalic()).build();
                if (pfd != null) {
                    pfd.close();
                }
                return build;
            } catch (Throwable th3) {
                th2 = th3;
                th = th3;
            }
        } catch (IOException e) {
            return null;
        }
        if (pfd != null) {
            if (th2 != null) {
                try {
                    pfd.close();
                } catch (Throwable th32) {
                    th2.addSuppressed(th32);
                }
            } else {
                pfd.close();
            }
        }
        throw th;
        throw th;
    }

    @Nullable
    public Typeface createFromResourcesFontFile(Context context, Resources resources, int id, String path, int style) {
        if (!isFontFamilyPrivateAPIAvailable()) {
            return super.createFromResourcesFontFile(context, resources, id, path, style);
        }
        Object fontFamily = newFamily();
        if (!addFontFromAssetManager(context, fontFamily, path, 0, -1, -1, null)) {
            abortCreation(fontFamily);
            return null;
        } else if (freeze(fontFamily)) {
            return createFromFamiliesWithDefault(fontFamily);
        } else {
            return null;
        }
    }

    protected Class obtainFontFamily() throws ClassNotFoundException {
        return Class.forName(FONT_FAMILY_CLASS);
    }

    protected Constructor obtainFontFamilyCtor(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getConstructor(new Class[0]);
    }

    protected Method obtainAddFontFromAssetManagerMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(ADD_FONT_FROM_ASSET_MANAGER_METHOD, new Class[]{AssetManager.class, String.class, Integer.TYPE, Boolean.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, FontVariationAxis[].class});
    }

    protected Method obtainAddFontFromBufferMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(ADD_FONT_FROM_BUFFER_METHOD, new Class[]{ByteBuffer.class, Integer.TYPE, FontVariationAxis[].class, Integer.TYPE, Integer.TYPE});
    }

    protected Method obtainFreezeMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(FREEZE_METHOD, new Class[0]);
    }

    protected Method obtainAbortCreationMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(ABORT_CREATION_METHOD, new Class[0]);
    }

    protected Method obtainCreateFromFamiliesWithDefaultMethod(Class fontFamily) throws NoSuchMethodException {
        Object familyArray = Array.newInstance(fontFamily, 1);
        Method m = Typeface.class.getDeclaredMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD, new Class[]{familyArray.getClass(), Integer.TYPE, Integer.TYPE});
        m.setAccessible(true);
        return m;
    }
}
