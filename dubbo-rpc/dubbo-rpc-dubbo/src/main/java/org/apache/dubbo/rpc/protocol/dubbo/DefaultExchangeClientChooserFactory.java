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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.utils.AtomicPositiveInteger;
import org.apache.dubbo.remoting.exchange.ExchangeClient;

/**
 * DefaultExchangeClientChooserFactory
 */
public class DefaultExchangeClientChooserFactory implements ExchangeClientChooserFactory {

    private static boolean isPowerOfTwo(int val) {
        //2^n,& is better than %
        return (val & -val) == val;
    }

    public ExchangeClientChooser newChooser(ExchangeClient[] exchangeClients) {
        if (exchangeClients.length == 0) {
            return new SingleExchangeClientChooser(exchangeClients);
        } else if (isPowerOfTwo(exchangeClients.length)) {
            return new PowerOfTwoExchangeClientChooser(exchangeClients);
        } else {
            return new GenericExchangeClientChooser(exchangeClients);
        }
    }

    private static final class PowerOfTwoExchangeClientChooser implements ExchangeClientChooser {

        private final AtomicPositiveInteger idx = new AtomicPositiveInteger();

        private final ExchangeClient[] exchangeClients;

        PowerOfTwoExchangeClientChooser(ExchangeClient[] exchangeClients) {
            this.exchangeClients = exchangeClients;
        }

        @Override
        public ExchangeClient next() {
            return exchangeClients[idx.getAndIncrement() & exchangeClients.length - 1];
        }
    }

    private static final class GenericExchangeClientChooser implements ExchangeClientChooser {

        private final AtomicPositiveInteger idx = new AtomicPositiveInteger();

        private final ExchangeClient[] exchangeClients;

        GenericExchangeClientChooser(ExchangeClient[] exchangeClients) {
            this.exchangeClients = exchangeClients;
        }

        @Override
        public ExchangeClient next() {
            return exchangeClients[Math.abs(idx.getAndIncrement() % exchangeClients.length)];
        }
    }

    private static final class SingleExchangeClientChooser implements ExchangeClientChooser {

        private final ExchangeClient exchangeClient;

        SingleExchangeClientChooser(ExchangeClient[] exchangeClients) {
            this.exchangeClient = exchangeClients[0];
        }

        @Override
        public ExchangeClient next() {
            return exchangeClient;
        }
    }

}
