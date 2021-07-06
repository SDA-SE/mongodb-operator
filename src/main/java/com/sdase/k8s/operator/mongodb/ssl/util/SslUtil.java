package com.sdase.k8s.operator.mongodb.ssl.util;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.X509TrustedCertificateBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility to help with SSL related stuff. */
public class SslUtil {

  private static final Logger LOG = LoggerFactory.getLogger(SslUtil.class);

  private SslUtil() {
    // this is a utility
  }

  public static SSLContext createSslContext(KeyStore keyStore)
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    var trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
    trustManagerFactory.init(keyStore);

    var sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(null, trustManagerFactory.getTrustManagers(), createSecureRandom());

    return sslContext;
  }

  public static KeyStore createTruststoreFromPemKey(String certificateAsString)
      throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
    try (var parser = new PEMParser(new StringReader(certificateAsString))) {
      var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      var i = 0;
      X509Certificate certificate;
      while ((certificate = parseCert(parser)) != null) {
        keyStore.setCertificateEntry("cert_" + i, certificate);
        i += 1;
      }
      return keyStore;
    }
  }

  private static X509Certificate parseCert(PEMParser parser)
      throws IOException, CertificateException {

    var certificateObject = parser.readObject();
    if (certificateObject == null) {
      return null;
    }
    if (certificateObject instanceof X509CertificateHolder) {
      X509CertificateHolder certHolder = (X509CertificateHolder) certificateObject;
      return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
    if (certificateObject instanceof X509TrustedCertificateBlock) {
      X509CertificateHolder certHolder =
          ((X509TrustedCertificateBlock) certificateObject).getCertificateHolder();
      return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
    throw new CertificateException(
        "Could not read certificate of type " + certificateObject.getClass());
  }

  private static SecureRandom createSecureRandom() throws NoSuchAlgorithmException {
    var algorithmNativePRNG = "NativePRNG";
    var algorithmWindowsPRNG = "Windows-PRNG";
    try {
      return SecureRandom.getInstance(algorithmNativePRNG);
    } catch (NoSuchAlgorithmException e) {
      LOG.warn(
          "Failed to create SecureRandom with algorithm {}. Falling back to {}."
              + "This should only happen on windows machines.",
          algorithmNativePRNG,
          algorithmWindowsPRNG,
          e);
      return SecureRandom.getInstance(algorithmWindowsPRNG);
    }
  }
}
