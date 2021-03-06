package org.bouncycastle.x509;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.CertificatePair;
import org.bouncycastle.jcajce.util.BCJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.provider.X509CertificateObject;

public class X509CertificatePair {
    private final JcaJceHelper bcHelper = new BCJcaJceHelper();
    private X509Certificate forward;
    private X509Certificate reverse;

    public X509CertificatePair(X509Certificate x509Certificate, X509Certificate x509Certificate2) {
        this.forward = x509Certificate;
        this.reverse = x509Certificate2;
    }

    public X509CertificatePair(CertificatePair certificatePair) throws CertificateParsingException {
        if (certificatePair.getForward() != null) {
            this.forward = new X509CertificateObject(certificatePair.getForward());
        }
        if (certificatePair.getReverse() != null) {
            this.reverse = new X509CertificateObject(certificatePair.getReverse());
        }
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null || !(obj instanceof X509CertificatePair)) {
            return false;
        }
        X509CertificatePair x509CertificatePair = (X509CertificatePair) obj;
        boolean equals = this.forward != null ? this.forward.equals(x509CertificatePair.forward) : x509CertificatePair.forward == null;
        boolean equals2 = this.reverse != null ? this.reverse.equals(x509CertificatePair.reverse) : x509CertificatePair.reverse == null;
        if (equals && equals2) {
            z = true;
        }
        return z;
    }

    public byte[] getEncoded() throws CertificateEncodingException {
        try {
            Certificate instance;
            Certificate certificate = null;
            if (this.forward != null) {
                instance = Certificate.getInstance(new ASN1InputStream(this.forward.getEncoded()).readObject());
                if (instance == null) {
                    throw new CertificateEncodingException("unable to get encoding for forward");
                }
            }
            instance = null;
            if (this.reverse != null) {
                certificate = Certificate.getInstance(new ASN1InputStream(this.reverse.getEncoded()).readObject());
                if (certificate == null) {
                    throw new CertificateEncodingException("unable to get encoding for reverse");
                }
            }
            return new CertificatePair(instance, certificate).getEncoded(ASN1Encoding.DER);
        } catch (Throwable e) {
            throw new ExtCertificateEncodingException(e.toString(), e);
        } catch (Throwable e2) {
            throw new ExtCertificateEncodingException(e2.toString(), e2);
        }
    }

    public X509Certificate getForward() {
        return this.forward;
    }

    public X509Certificate getReverse() {
        return this.reverse;
    }

    public int hashCode() {
        int i = -1;
        if (this.forward != null) {
            i = -1 ^ this.forward.hashCode();
        }
        return this.reverse != null ? (i * 17) ^ this.reverse.hashCode() : i;
    }
}
