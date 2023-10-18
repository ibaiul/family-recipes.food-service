package eus.ibai.family.recipes.food.wm.test;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Properties;

public class ZookeeperUtils {

    private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

    private static final SecureRandom RANDOM_GENERATOR = new SecureRandom();

    private static final String PASSWORD = "123456";

    static {
        Security.addProvider(PROVIDER);
        X500Name.setDefaultStyle(RFC4519Style.INSTANCE);
    }

    public static void createJksFiles(File serverKeystoreFile, File serverTruststoreFile, File clientKeystoreFile, File clientTruststoreFile) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", PROVIDER);
        keyPairGenerator.initialize(4096, RANDOM_GENERATOR);
        String signatureAlgorithm = "SHA256WithRSA";

        KeyPair caKeyPair = keyPairGenerator.generateKeyPair();
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(caKeyPair.getPrivate());
        X500Name caSubject = createX500Name("ca");
        X509Certificate caCert = getSignedCert(caKeyPair, caSubject, caSubject, contentSigner);

        KeyPair serverKeyPair = keyPairGenerator.generateKeyPair();
        X500Name serverSubject = createX500Name("server");
        X509Certificate serverCert = getSignedCert(serverKeyPair, serverSubject, caSubject, contentSigner);

        KeyPair clientKeyPair = keyPairGenerator.generateKeyPair();
        X500Name clientSubject = createX500Name("client");
        X509Certificate clientCert = getSignedCert(clientKeyPair, clientSubject, caSubject, contentSigner);

        KeyStore serverKeystore = createEmptyKeystore();
        addPrivateKeyToKeystore(serverKeystore, "server", serverKeyPair.getPrivate(), new Certificate[]{serverCert, caCert});
        persistKeystore(serverKeystore, serverKeystoreFile);

        createTruststore(serverTruststoreFile, caCert);

        KeyStore clientKeystore = createEmptyKeystore();
        addPrivateKeyToKeystore(clientKeystore, "client", clientKeyPair.getPrivate(), new Certificate[]{clientCert, caCert});
        persistKeystore(clientKeystore, clientKeystoreFile);

        createTruststore(clientTruststoreFile, caCert);
    }

    private static void createTruststore(File file, Certificate certificate) throws Exception {
        KeyStore keystore = createEmptyKeystore();
        addPublicCertificateKeyToKeystore(keystore, "ca", certificate);
        persistKeystore(keystore, file);
    }

    private static X509Certificate getSignedCert(KeyPair keyPair, X500Name subject, X500Name issuer, ContentSigner contentSigner) throws Exception {
        PublicKey keyPublic = keyPair.getPublic();
        byte[] keyPublicEncoded = keyPublic.getEncoded();
        SubjectPublicKeyInfo keyPublicInfo = SubjectPublicKeyInfo.getInstance(keyPublicEncoded);

        ZonedDateTime notBefore = ZonedDateTime.now();
        ZonedDateTime notAfter = notBefore.plusYears(1);

        Date dateNotBefore = Date.from(notBefore.toInstant());
        Date dateNotAfter = Date.from(notAfter.toInstant());

        try (ByteArrayInputStream bais = new ByteArrayInputStream(keyPublicEncoded); ASN1InputStream ais = new ASN1InputStream(bais)) {
            ASN1Sequence asn1Sequence = (ASN1Sequence) ais.readObject();

            SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(asn1Sequence);
            SubjectKeyIdentifier subjectPublicKeyId = new BcX509ExtensionUtils().createSubjectKeyIdentifier(subjectPublicKeyInfo);

            BigInteger sn = new BigInteger(Long.SIZE, RANDOM_GENERATOR);
            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, sn, dateNotBefore, dateNotAfter, subject, keyPublicInfo);
            if (subject.equals(issuer)) {
                certBuilder.addExtension(Extension.authorityKeyIdentifier, false, subjectPublicKeyId);
                certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            }
            X509CertificateHolder certHolder = certBuilder.addExtension(Extension.subjectKeyIdentifier, false, subjectPublicKeyId)
                    .build(contentSigner);

            return new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certHolder);
        }
    }

    private static X500Name createX500Name(String cn) {
        return new X500NameBuilder(RFC4519Style.INSTANCE)
                .addRDN(RFC4519Style.c, new DERUTF8String("XX"))
                .addRDN(RFC4519Style.st, new DERUTF8String("Bizkaia"))
                .addRDN(RFC4519Style.l, new DERUTF8String("Bilbo"))
                .addRDN(RFC4519Style.o, new DERUTF8String("O"))
                .addRDN(RFC4519Style.ou, new DERUTF8String("OU"))
                .addRDN(RFC4519Style.cn, new DERUTF8String(cn))
                .build();
    }

    public static KeyStore createEmptyKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        return  keyStore;
    }

    private static void addPrivateKeyToKeystore(KeyStore keyStore, String alias, PrivateKey key, Certificate[] certs) throws Exception {
        keyStore.setKeyEntry(alias, key, PASSWORD.toCharArray(), certs);
    }

    private static void addPublicCertificateKeyToKeystore(KeyStore keyStore, String alias, Certificate cert) throws Exception {
        keyStore.setCertificateEntry(alias, cert);
    }

    public static void persistKeystore(KeyStore keyStore, File file) throws Exception {
        keyStore.store(new FileOutputStream(file), PASSWORD.toCharArray());
    }

    public static void createClientProperties(File propertyFile, File keystoreFile, File truststoreFile) throws IOException {
        Properties properties = new Properties();
        properties.put("zookeeper.sasl.client", "true");
        properties.put("zookeeper.client.secure", "true");
        properties.put("zookeeper.clientCnxnSocket", "org.apache.zookeeper.ClientCnxnSocketNetty");
        properties.put("zookeeper.ssl.keyStore.location", keystoreFile.getAbsolutePath());
        properties.put("zookeeper.ssl.keyStore.password", PASSWORD);
        properties.put("zookeeper.ssl.trustStore.location", truststoreFile.getAbsolutePath());
        properties.put("zookeeper.ssl.trustStore.password", PASSWORD);
        properties.put("zookeeper.ssl.hostnameVerification", "false");
        properties.store(new FileOutputStream(propertyFile), null);
    }
}