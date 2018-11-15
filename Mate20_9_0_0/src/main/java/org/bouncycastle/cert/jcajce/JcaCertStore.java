package org.bouncycastle.cert.jcajce;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.util.CollectionStore;

public class JcaCertStore extends CollectionStore {
    public JcaCertStore(Collection collection) throws CertificateEncodingException {
        super(convertCerts(collection));
    }

    private static Collection convertCerts(Collection collection) throws CertificateEncodingException {
        Collection arrayList = new ArrayList(collection.size());
        for (Object next : collection) {
            if (next instanceof X509Certificate) {
                try {
                    arrayList.add(new X509CertificateHolder(((X509Certificate) next).getEncoded()));
                } catch (IOException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to read encoding: ");
                    stringBuilder.append(e.getMessage());
                    throw new CertificateEncodingException(stringBuilder.toString());
                }
            }
            arrayList.add((X509CertificateHolder) next);
        }
        return arrayList;
    }
}