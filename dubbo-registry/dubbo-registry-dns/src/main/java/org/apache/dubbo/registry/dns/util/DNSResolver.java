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
package org.apache.dubbo.registry.dns.util;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DNSResolver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Resolver resolver;

    /**
     * mark if already upgrade to TCP protocol of resolver
     */
    private boolean upgradeToTCP = false;

    public DNSResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    public List<Record> resolve(String path) {
        List<Record> recordList = new LinkedList<>();
        try {
            Lookup aRecordLookup = new Lookup(path, Type.A);
            Lookup aaaaRecordLookup = new Lookup(path, Type.AAAA);
            Lookup srvRecordLookup = new Lookup(path, Type.SRV);

            aRecordLookup.setResolver(resolver);
            aaaaRecordLookup.setResolver(resolver);
            srvRecordLookup.setResolver(resolver);

            Record[] aRecords = aRecordLookup.run();
            Record[] aaaaRecords = aaaaRecordLookup.run();
            Record[] srvRecords = srvRecordLookup.run();

            // UDP protocol may cause message buffer error in some platform
            boolean networkError = (aaaaRecordLookup.getResult() == Lookup.TRY_AGAIN) ||
                    (aaaaRecordLookup.getResult() == Lookup.TRY_AGAIN) ||
                    (srvRecordLookup.getResult() == Lookup.TRY_AGAIN);

            if (networkError && !upgradeToTCP) {
                if (logger.isInfoEnabled()) {
                    logger.info("DNS lookup failed due to network error. " +
                            "Try use TCP to resolve.");
                }

                resolver.setTCP(true);
                upgradeToTCP = true;
                return resolve(path);
            }

            if (aRecords != null) {
                recordList.addAll(Arrays.asList(aRecords));
            }
            if (aaaaRecords != null) {
                recordList.addAll(Arrays.asList(aaaaRecords));
            }
            if (srvRecords != null) {
                recordList.addAll(Arrays.asList(srvRecords));
            }

        } catch (TextParseException e) {
            String message = "Parse DNS host error! " + e.getLocalizedMessage();
            logger.error(message);
            throw new IllegalStateException(message);
        }

        return recordList;
    }
}
