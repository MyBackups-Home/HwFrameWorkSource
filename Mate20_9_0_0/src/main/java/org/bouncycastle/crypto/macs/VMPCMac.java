package org.bouncycastle.crypto.macs;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class VMPCMac implements Mac {
    private byte[] P = null;
    private byte[] T;
    private byte g;
    private byte n = (byte) 0;
    private byte s = (byte) 0;
    private byte[] workingIV;
    private byte[] workingKey;
    private byte x1;
    private byte x2;
    private byte x3;
    private byte x4;

    private void initKey(byte[] bArr, byte[] bArr2) {
        int i;
        this.s = (byte) 0;
        this.P = new byte[256];
        for (int i2 = 0; i2 < 256; i2++) {
            this.P[i2] = (byte) i2;
        }
        for (int i3 = 0; i3 < 768; i3++) {
            i = i3 & 255;
            this.s = this.P[((this.s + this.P[i]) + bArr[i3 % bArr.length]) & 255];
            byte b = this.P[i];
            this.P[i] = this.P[this.s & 255];
            this.P[this.s & 255] = b;
        }
        for (int i4 = 0; i4 < 768; i4++) {
            i = i4 & 255;
            this.s = this.P[((this.s + this.P[i]) + bArr2[i4 % bArr2.length]) & 255];
            byte b2 = this.P[i];
            this.P[i] = this.P[this.s & 255];
            this.P[this.s & 255] = b2;
        }
        this.n = (byte) 0;
    }

    public int doFinal(byte[] bArr, int i) throws DataLengthException, IllegalStateException {
        for (int i2 = 1; i2 < 25; i2++) {
            this.s = this.P[(this.s + this.P[this.n & 255]) & 255];
            this.x4 = this.P[((this.x4 + this.x3) + i2) & 255];
            this.x3 = this.P[((this.x3 + this.x2) + i2) & 255];
            this.x2 = this.P[((this.x2 + this.x1) + i2) & 255];
            this.x1 = this.P[((this.x1 + this.s) + i2) & 255];
            this.T[this.g & 31] = (byte) (this.T[this.g & 31] ^ this.x1);
            this.T[(this.g + 1) & 31] = (byte) (this.T[(this.g + 1) & 31] ^ this.x2);
            this.T[(this.g + 2) & 31] = (byte) (this.T[(this.g + 2) & 31] ^ this.x3);
            this.T[(this.g + 3) & 31] = (byte) (this.T[(this.g + 3) & 31] ^ this.x4);
            this.g = (byte) ((this.g + 4) & 31);
            byte b = this.P[this.n & 255];
            this.P[this.n & 255] = this.P[this.s & 255];
            this.P[this.s & 255] = b;
            this.n = (byte) ((this.n + 1) & 255);
        }
        for (int i3 = 0; i3 < 768; i3++) {
            int i4 = i3 & 255;
            this.s = this.P[((this.s + this.P[i4]) + this.T[i3 & 31]) & 255];
            byte b2 = this.P[i4];
            this.P[i4] = this.P[this.s & 255];
            this.P[this.s & 255] = b2;
        }
        Object obj = new byte[20];
        for (int i5 = 0; i5 < 20; i5++) {
            int i6 = i5 & 255;
            this.s = this.P[(this.s + this.P[i6]) & 255];
            obj[i5] = this.P[(this.P[this.P[this.s & 255] & 255] + 1) & 255];
            byte b3 = this.P[i6];
            this.P[i6] = this.P[this.s & 255];
            this.P[this.s & 255] = b3;
        }
        System.arraycopy(obj, 0, bArr, i, obj.length);
        reset();
        return obj.length;
    }

    public String getAlgorithmName() {
        return "VMPC-MAC";
    }

    public int getMacSize() {
        return 20;
    }

    public void init(CipherParameters cipherParameters) throws IllegalArgumentException {
        if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            KeyParameter keyParameter = (KeyParameter) parametersWithIV.getParameters();
            if (parametersWithIV.getParameters() instanceof KeyParameter) {
                this.workingIV = parametersWithIV.getIV();
                if (this.workingIV == null || this.workingIV.length < 1 || this.workingIV.length > 768) {
                    throw new IllegalArgumentException("VMPC-MAC requires 1 to 768 bytes of IV");
                }
                this.workingKey = keyParameter.getKey();
                reset();
                return;
            }
            throw new IllegalArgumentException("VMPC-MAC Init parameters must include a key");
        }
        throw new IllegalArgumentException("VMPC-MAC Init parameters must include an IV");
    }

    public void reset() {
        initKey(this.workingKey, this.workingIV);
        this.n = (byte) 0;
        this.x4 = (byte) 0;
        this.x3 = (byte) 0;
        this.x2 = (byte) 0;
        this.x1 = (byte) 0;
        this.g = (byte) 0;
        this.T = new byte[32];
        for (int i = 0; i < 32; i++) {
            this.T[i] = (byte) 0;
        }
    }

    public void update(byte b) throws IllegalStateException {
        this.s = this.P[(this.s + this.P[this.n & 255]) & 255];
        b = (byte) (b ^ this.P[(this.P[this.P[this.s & 255] & 255] + 1) & 255]);
        this.x4 = this.P[(this.x4 + this.x3) & 255];
        this.x3 = this.P[(this.x3 + this.x2) & 255];
        this.x2 = this.P[(this.x2 + this.x1) & 255];
        this.x1 = this.P[((this.x1 + this.s) + b) & 255];
        this.T[this.g & 31] = (byte) (this.T[this.g & 31] ^ this.x1);
        this.T[(this.g + 1) & 31] = (byte) (this.T[(this.g + 1) & 31] ^ this.x2);
        this.T[(this.g + 2) & 31] = (byte) (this.T[(this.g + 2) & 31] ^ this.x3);
        this.T[(this.g + 3) & 31] = (byte) (this.T[(this.g + 3) & 31] ^ this.x4);
        this.g = (byte) ((this.g + 4) & 31);
        b = this.P[this.n & 255];
        this.P[this.n & 255] = this.P[this.s & 255];
        this.P[this.s & 255] = b;
        this.n = (byte) ((this.n + 1) & 255);
    }

    public void update(byte[] bArr, int i, int i2) throws DataLengthException, IllegalStateException {
        if (i + i2 <= bArr.length) {
            for (int i3 = 0; i3 < i2; i3++) {
                update(bArr[i + i3]);
            }
            return;
        }
        throw new DataLengthException("input buffer too short");
    }
}
