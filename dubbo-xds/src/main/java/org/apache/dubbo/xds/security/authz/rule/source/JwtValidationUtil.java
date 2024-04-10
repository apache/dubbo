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
package org.apache.dubbo.xds.security.authz.rule.source;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.lang.JoseException;

public class JwtValidationUtil {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JwtValidationUtil.class);

    public static JwtClaims extractJwtClaims(String jwks, String token) {
        if (StringUtils.isBlank(jwks) || StringUtils.isBlank(token)) {
            return null;
        }
        try {
            // don't validate jwt's attribute, just validate the sign
            JwtConsumerBuilder jwtConsumerBuilder = new JwtConsumerBuilder().setSkipAllValidators();
            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(token);
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jwks);
            JwksVerificationKeyResolver jwksResolver = new JwksVerificationKeyResolver(jsonWebKeySet.getJsonWebKeys());
            jwtConsumerBuilder.setVerificationKeyResolver(jwksResolver);
            JwtConsumer jwtConsumer = jwtConsumerBuilder.build();
            JwtContext jwtContext = jwtConsumer.process(token);
            return jwtContext.getJwtClaims();
        } catch (JoseException e) {
            logger.warn("", "", "", "Invalid jwks = " + jwks);
        } catch (InvalidJwtException e) {
            logger.warn("", "", "", "Invalid jwt token" + token + "for jwks " + jwks);
        }
        return null;
    }
}
