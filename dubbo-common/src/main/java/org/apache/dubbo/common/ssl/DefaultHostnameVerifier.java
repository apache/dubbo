/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.ssl;


import org.apache.dubbo.config.Constants;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;

/**
 * default HostnameVerifier , refer to ZKHostnameVerifier
 *
 *  verify only if  Constants.REST_CLIENT_HOST_NAME_VERIFIER_SWITCH_PROPERTY set true
 */
public class DefaultHostnameVerifier implements HostnameVerifier {

    private static final class SubjectName {

        static final int DNS = 2;
        static final int IP = 7;

        private final String value;
        private final int type;

        SubjectName(final String value, final int type) {
            if (type != DNS && type != IP) {
                throw new IllegalArgumentException("Invalid type: " + type);
            }
            this.value = Objects.requireNonNull(value);
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }

    }

    private static class InetAddressUtils {

        private InetAddressUtils() {
        }

        private static final Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

        private static final Pattern IPV6_STD_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

        private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");

        static boolean isIPv4Address(final String input) {
            return IPV4_PATTERN.matcher(input).matches();
        }

        static boolean isIPv6StdAddress(final String input) {
            return IPV6_STD_PATTERN.matcher(input).matches();
        }

        static boolean isIPv6HexCompressedAddress(final String input) {
            return IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
        }

        static boolean isIPv6Address(final String input) {
            return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
        }

    }

    enum HostNameType {

        IPv4(7),
        IPv6(7),
        DNS(2);

        final int subjectType;

        HostNameType(final int subjectType) {
            this.subjectType = subjectType;
        }

    }


    @Override
    public boolean verify(final String host, final SSLSession session) {
        try {

            if (!hostNameVerifierIsOpen()) {
                return true;
            }

            final Certificate[] certs = session.getPeerCertificates();
            final X509Certificate x509 = (X509Certificate) certs[0];
            verify(host, x509);
            return true;
        } catch (SSLException ex) {
            return false;
        }
    }

    void verify(final String host, final X509Certificate cert) throws SSLException {
        final HostNameType hostType = determineHostFormat(host);
        final List<SubjectName> subjectAlts = getSubjectAltNames(cert);
        if (subjectAlts != null && !subjectAlts.isEmpty()) {
            switch (hostType) {
                case IPv4:
                    matchIPAddress(host, subjectAlts);
                    break;
                case IPv6:
                    matchIPv6Address(host, subjectAlts);
                    break;
                default:
                    matchDNSName(host, subjectAlts);
            }
        } else {
            final X500Principal subjectPrincipal = cert.getSubjectX500Principal();
            final String cn = extractCN(subjectPrincipal.getName(X500Principal.RFC2253));
            if (cn == null) {
                throw new SSLException("Certificate subject for <"
                    + host
                    + "> doesn't contain "
                    + "a common name and does not have alternative names");
            }
            matchCN(host, cn);
        }
    }

    private static void matchIPAddress(final String host, final List<SubjectName> subjectAlts) throws SSLException {
        for (int i = 0; i < subjectAlts.size(); i++) {
            final SubjectName subjectAlt = subjectAlts.get(i);
            if (subjectAlt.getType() == SubjectName.IP) {
                if (host.equals(subjectAlt.getValue())) {
                    return;
                }
            }
        }
        throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match any "
            + "of the subject alternative names: " + subjectAlts);
    }

    private static void matchIPv6Address(final String host, final List<SubjectName> subjectAlts) throws SSLException {
        if (host == null) {
            return;
        }
        final String normalisedHost = normaliseAddress(host);
        for (int i = 0; i < subjectAlts.size(); i++) {
            final SubjectName subjectAlt = subjectAlts.get(i);
            if (subjectAlt.getType() == SubjectName.IP) {
                final String normalizedSubjectAlt = normaliseAddress(subjectAlt.getValue());
                if (normalisedHost.equals(normalizedSubjectAlt)) {
                    return;
                }
            }
        }
        throw new SSLPeerUnverifiedException("Certificate for <"
            + host
            + "> doesn't match any "
            + "of the subject alternative names: "
            + subjectAlts);
    }

    private static void matchDNSName(final String host, final List<SubjectName> subjectAlts) throws SSLException {
        final String normalizedHost = host.toLowerCase(Locale.ROOT);
        for (int i = 0; i < subjectAlts.size(); i++) {
            final SubjectName subjectAlt = subjectAlts.get(i);
            if (subjectAlt.getType() == SubjectName.DNS) {
                final String normalizedSubjectAlt = subjectAlt.getValue().toLowerCase(Locale.ROOT);
                if (matchIdentityStrict(normalizedHost, normalizedSubjectAlt)) {
                    return;
                }
            }
        }
        throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match any "
            + "of the subject alternative names: " + subjectAlts);
    }

    private static void matchCN(final String host, final String cn) throws SSLException {
        final String normalizedHost = host.toLowerCase(Locale.ROOT);
        final String normalizedCn = cn.toLowerCase(Locale.ROOT);
        if (!matchIdentityStrict(normalizedHost, normalizedCn)) {
            throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match "
                + "common name of the certificate subject: " + cn);
        }
    }

    private static boolean matchIdentity(final String host, final String identity, final boolean strict) {
        // "...Names may contain the wildcard
        // character * which is considered to match any single domain name
        // component or component fragment..."
        // Based on this statement presuming only singular wildcard is legal
        final int asteriskIdx = identity.indexOf('*');
        if (asteriskIdx != -1) {
            final String prefix = identity.substring(0, asteriskIdx);
            final String suffix = identity.substring(asteriskIdx + 1);
            if (!prefix.isEmpty() && !host.startsWith(prefix)) {
                return false;
            }
            if (!suffix.isEmpty() && !host.endsWith(suffix)) {
                return false;
            }
            // Additional sanity checks on content selected by wildcard can be done here
            if (strict) {
                final String remainder = host.substring(prefix.length(), host.length() - suffix.length());
                return !remainder.contains(".");
            }
            return true;
        }
        return host.equalsIgnoreCase(identity);
    }

    private static boolean matchIdentityStrict(final String host, final String identity) {
        return matchIdentity(host, identity, true);
    }

    private static String extractCN(final String subjectPrincipal) throws SSLException {
        if (subjectPrincipal == null) {
            return null;
        }
        try {
            final LdapName subjectDN = new LdapName(subjectPrincipal);
            final List<Rdn> rdns = subjectDN.getRdns();
            for (int i = rdns.size() - 1; i >= 0; i--) {
                final Rdn rds = rdns.get(i);
                final Attributes attributes = rds.toAttributes();
                final Attribute cn = attributes.get("cn");
                if (cn != null) {
                    try {
                        final Object value = cn.get();
                        if (value != null) {
                            return value.toString();
                        }
                    } catch (final NoSuchElementException ignore) {
                        // ignore exception
                    } catch (final NamingException ignore) {
                        // ignore exception
                    }
                }
            }
            return null;
        } catch (final InvalidNameException e) {
            throw new SSLException(subjectPrincipal + " is not a valid X500 distinguished name");
        }
    }

    private static HostNameType determineHostFormat(final String host) {
        if (InetAddressUtils.isIPv4Address(host)) {
            return HostNameType.IPv4;
        }
        String s = host;
        if (s.startsWith("[") && s.endsWith("]")) {
            s = host.substring(1, host.length() - 1);
        }
        if (InetAddressUtils.isIPv6Address(s)) {
            return HostNameType.IPv6;
        }
        return HostNameType.DNS;
    }

    private static List<SubjectName> getSubjectAltNames(final X509Certificate cert) {
        try {
            final Collection<List<?>> entries = cert.getSubjectAlternativeNames();
            if (entries == null) {
                return Collections.emptyList();
            }
            final List<SubjectName> result = new ArrayList<SubjectName>();
            for (List<?> entry : entries) {
                final Integer type = entry.size() >= 2 ? (Integer) entry.get(0) : null;
                if (type != null) {
                    if (type == SubjectName.DNS || type == SubjectName.IP) {
                        final Object o = entry.get(1);
                        if (o instanceof String) {
                            result.add(new SubjectName((String) o, type));
                        } else if (o instanceof byte[]) {
                            // TODO ASN.1 DER encoded form
                        }
                    }
                }
            }
            return result;
        } catch (final CertificateParsingException ignore) {
            return Collections.emptyList();
        }
    }

    /*
     * Normalize IPv6 or DNS name.
     */
    private static String normaliseAddress(final String hostname) {
        if (hostname == null) {
            return hostname;
        }
        try {
            final InetAddress inetAddress = InetAddress.getByName(hostname);
            return inetAddress.getHostAddress();
        } catch (final UnknownHostException unexpected) { // Should not happen, because we check for IPv6 address above
            return hostname;
        }
    }

    public boolean hostNameVerifierIsOpen() {
        String property = System.getProperty(Constants.REST_CLIENT_HOST_NAME_VERIFIER_SWITCH_PROPERTY);
        if (property == null) {
            return false;
        }

        try {
            return Boolean.valueOf(property);
        } catch (Exception e) {
            return false;
        }
    }

}

