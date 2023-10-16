package eus.ibai.family.recipes.food.wm.test;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Properties;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.*;
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

public class TlsUtils {

    private static final String CERTIFICATE_ALIAS = "YOUR_CERTIFICATE_NAME";
    private static final String CERTIFICATE_ALGORITHM = "RSA";
    private static final String CERTIFICATE_DN = "CN=cn, O=o, L=L, ST=il, C= c";
    private static final String CERTIFICATE_NAME = "keystore.test";
    private static final int CERTIFICATE_BITS = 1024;

    private static final BouncyCastleProvider bcProvider = new BouncyCastleProvider();

    private static final SecureRandom prng = new SecureRandom();

    static {
        Security.addProvider(bcProvider);
        X500Name.setDefaultStyle(RFC4519Style.INSTANCE);
    }

    public static void createKeystore(File serverKeystoreFile, File serverTruststoreFile, File clientKeystoreFile, File clientTruststoreFile) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", bcProvider);
        keyPairGenerator.initialize(4096, prng);
        String signatureAlgorithm = "SHA256WithRSA";

        KeyPair caKeyPair = keyPairGenerator.generateKeyPair();
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(caKeyPair.getPrivate());
        X500Name caSubject = createX500Name("ca");
        X509Certificate caCert = getSelfSignedCert(caKeyPair, caSubject, caSubject, contentSigner);

        KeyPair serverKeyPair = keyPairGenerator.generateKeyPair();
        X500Name serverSubject = createX500Name("server");
        X509Certificate serverCert = getSelfSignedCert(serverKeyPair, serverSubject, caSubject, contentSigner);

        KeyPair clientKeyPair = keyPairGenerator.generateKeyPair();
        X500Name clientSubject = createX500Name("client");
        X509Certificate clientCert = getSelfSignedCert(clientKeyPair, clientSubject, caSubject, contentSigner);

//        saveCert(caCert, clientKeyPair.getPrivate(), "ca", file);
//        saveCert(clientCert, clientKeyPair.getPrivate(), "client", file);
//        addCertsToKeystore(new Certificate[]{clientCert, caCert}, clientKeyPair.getPrivate(), "client", file);

        KeyStore serverKeystore = createEmptyKeystore();
        addPrivateKeyToKeystore(serverKeystore, "server", serverKeyPair.getPrivate(), new Certificate[]{serverCert, caCert});
        persistKeystore(serverKeystore, serverKeystoreFile);

        createTruststore(serverTruststoreFile, caCert);

        KeyStore clientKeystore = createEmptyKeystore();
        addPrivateKeyToKeystore(clientKeystore, "client", clientKeyPair.getPrivate(), new Certificate[]{clientCert, caCert});
        persistKeystore(clientKeystore, clientKeystoreFile);

        createTruststore(clientTruststoreFile, caCert);

        System.out.println("SERVUS!!");
    }

    private static void createTruststore(File file, Certificate certificate) throws Exception {
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", bcProvider);
//        keyPairGenerator.initialize(4096, prng);
//        String signatureAlgorithm = "SHA256WithRSA";

//        KeyPair caKeyPair = keyPairGenerator.generateKeyPair();
//        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(caKeyPair.getPrivate());
//        X500Name caSubject = createX500Name("ca");
//        X509Certificate caCert = getSelfSignedCert(caKeyPair, caSubject, caSubject, contentSigner);

//        KeyPair clientKeyPair = keyPairGenerator.generateKeyPair();
//        X500Name clientSubject = createX500Name("client");
//        X509Certificate clientCert = getSelfSignedCert(clientKeyPair, clientSubject, caSubject, contentSigner);

////        saveCert(caCert, clientKeyPair.getPrivate(), "ca", file);
////        saveCert(clientCert, clientKeyPair.getPrivate(), "client", file);
////        addCertsToKeystore(new Certificate[]{clientCert, caCert}, clientKeyPair.getPrivate(), "client", file);
        KeyStore keystore = createEmptyKeystore();
        addPublicCertificateKeyToKeystore(keystore, "ca", certificate);
        persistKeystore(keystore, file);
        System.out.println("SERVUS!!");
    }

    private static X509Certificate getSelfSignedCert(KeyPair keyPair, X500Name subject, X500Name issuer, ContentSigner contentSigner) throws Exception {


        BigInteger sn = new BigInteger(Long.SIZE, prng);


        PublicKey keyPublic = keyPair.getPublic();
        byte[] keyPublicEncoded = keyPublic.getEncoded();
        SubjectPublicKeyInfo keyPublicInfo = SubjectPublicKeyInfo.getInstance(keyPublicEncoded);

        ZonedDateTime notBefore = ZonedDateTime.now();
        ZonedDateTime notAfter = notBefore.plusYears(100);

        Date dateNotBefore = Date.from(notBefore.toInstant());
        Date dateNotAfter = Date.from(notAfter.toInstant());

        try (ByteArrayInputStream bais = new ByteArrayInputStream(keyPublicEncoded);
             ASN1InputStream ais = new ASN1InputStream(bais)) {
            ASN1Sequence asn1Sequence = (ASN1Sequence) ais.readObject();

            SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(asn1Sequence);
            SubjectKeyIdentifier subjectPublicKeyId = new BcX509ExtensionUtils().createSubjectKeyIdentifier(subjectPublicKeyInfo);

            /*
             * Now build the Certificate, add some Extensions & sign it...
             */
            
            /*
             * BasicConstraints instantiated with "CA=true"
             * The BasicConstraints Extension is usually marked "critical=true"
             *
             * The Subject Key Identifier extension identifies the public key certified by this certificate.
             * This extension provides a way of distinguishing public keys if more than one is available for
             * a given subject name.
             */
            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, sn, dateNotBefore, dateNotAfter, subject, keyPublicInfo);
            if (subject.equals(issuer)) {
                certBuilder.addExtension(Extension.authorityKeyIdentifier, false, subjectPublicKeyId);
                certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            }
            X509CertificateHolder certHolder = certBuilder
                    .addExtension(Extension.subjectKeyIdentifier, false, subjectPublicKeyId)
                    .build(contentSigner);

            return new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certHolder);
        }
    }

    public static X500Name createX500Name(String cn) {
        X500Name subject = new X500NameBuilder(RFC4519Style.INSTANCE)
                .addRDN(RFC4519Style.c, new DERUTF8String("XX"))
                .addRDN(RFC4519Style.st, new DERUTF8String("Bizkaia"))
                .addRDN(RFC4519Style.l, new DERUTF8String("Bilbo"))
                .addRDN(RFC4519Style.o, new DERUTF8String("O"))
                .addRDN(RFC4519Style.ou, new DERUTF8String("OU"))
                .addRDN(RFC4519Style.cn, new DERUTF8String(cn))
                .build();

//        X500Name subject = new X500Name(RFC4519Style.INSTANCE, new RDN[]{new RDN(
//                new AttributeTypeAndValue[]{
//                        new AttributeTypeAndValue(RFC4519Style.cn, new DERUTF8String(cn)),
//                        new AttributeTypeAndValue(RFC4519Style.ou, new DERUTF8String("OU")),
//                        new AttributeTypeAndValue(RFC4519Style.o, new DERUTF8String("O")),
//                        new AttributeTypeAndValue(RFC4519Style.l, new DERUTF8String("Bilbo")),
//                        new AttributeTypeAndValue(RFC4519Style.st, new DERUTF8String("Bizkaia")),
//                        new AttributeTypeAndValue(RFC4519Style.c, new DERUTF8String("XX"))
//                })});
        return subject;
    }

    @SuppressWarnings("deprecation")
//    public static X509Certificate createRootCACertificate() throws Exception {
//        X509Certificate cert = null;
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(CERTIFICATE_ALGORITHM);
//        keyPairGenerator.initialize(CERTIFICATE_BITS, new SecureRandom());
//        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//
//        // GENERATE THE X509 CERTIFICATE
//        X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
//        v3CertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
//        v3CertGen.setIssuerDN(new X509Principal(CERTIFICATE_DN));
//        v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24));
//        v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
//        v3CertGen.setSubjectDN(new X509Principal(CERTIFICATE_DN));
//        v3CertGen.setPublicKey(keyPair.getPublic());
//        v3CertGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
//        cert = v3CertGen.generateX509Certificate(keyPair.getPrivate());
//        saveCert(cert, keyPair.getPrivate());
//        return cert;
//    }

    private static void saveCert(X509Certificate cert, PrivateKey key, String alias, File file) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(alias, key, "123456".toCharArray(), new java.security.cert.Certificate[]{cert});
        keyStore.store(new FileOutputStream(file), "123456".toCharArray());
    }

    private static KeyStore createEmptyKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        return  keyStore;
    }

    private static void addPrivateKeyToKeystore(KeyStore keyStore, String alias, PrivateKey key, Certificate[] certs) throws Exception {
        keyStore.setKeyEntry(alias, key, "123456".toCharArray(), certs);
    }

    private static void addPublicCertificateKeyToKeystore(KeyStore keyStore, String alias, Certificate cert) throws Exception {
        keyStore.setCertificateEntry(alias, cert);
    }

    private static void persistKeystore(KeyStore keyStore, File file) throws Exception {
        keyStore.store(new FileOutputStream(file), "123456".toCharArray());
    }

    public static void createClientProperties(File propertyFile, File keystoreFile, File truststoreFile) throws IOException {
        Properties properties = new Properties();
        properties.put("zookeeper.sasl.client", "true");
        properties.put("zookeeper.client.secure", "true");
        properties.put("zookeeper.clientCnxnSocket", "org.apache.zookeeper.ClientCnxnSocketNetty");
        properties.put("zookeeper.ssl.keyStore.location", keystoreFile.getAbsolutePath());
        properties.put("zookeeper.ssl.keyStore.password", "123456");
        properties.put("zookeeper.ssl.trustStore.location", truststoreFile.getAbsolutePath());
        properties.put("zookeeper.ssl.trustStore.password", "123456");
        properties.put("zookeeper.ssl.hostnameVerification", "false");
        properties.store(new FileOutputStream(propertyFile), null);
    }
}