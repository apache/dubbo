package org.apache.dubbo.security.cert;

public class CertPair {
    private final String privateKey;
    private final String publicKey;
    private final String trustCerts;
    private final long createTime;
    private final long expireTime;

    public CertPair(String privateKey, String publicKey, String trustCerts, long createTime, long expireTime) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.trustCerts = trustCerts;
        this.createTime = createTime;
        this.expireTime = expireTime;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getTrustCerts() {
        return trustCerts;
    }

    public long getCreateTime() {
        return createTime;
    }

    public boolean isExpire() {
        return System.currentTimeMillis() < expireTime;
    }
}
