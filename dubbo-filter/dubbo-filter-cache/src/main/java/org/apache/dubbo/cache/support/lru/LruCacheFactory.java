<<<<<<< HEAD
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
package org.apache.dubbo.cache.support.lru;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.support.AbstractCacheFactory;
import org.apache.dubbo.common.URL;

/**
 * Implement {@link org.apache.dubbo.cache.CacheFactory} by extending {@link AbstractCacheFactory} and provide
 * instance of new {@link LruCache}.
 *
 * @see AbstractCacheFactory
 * @see LruCache
 * @see Cache
 */
public class LruCacheFactory extends AbstractCacheFactory {

    public static final String NAME = "lru";

    /**
     * Takes url as an method argument and return new instance of cache store implemented by LruCache.
     * @param url url of the method
     * @return ThreadLocalCache instance of cache
     */
    @Override
    protected Cache createCache(URL url) {
        return new LruCache(url);
    }

}
=======
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
package org.apache.dubbo.cache.support.lru;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.support.AbstractCacheFactory;
import org.apache.dubbo.common.URL;

/**
 * Implement {@link org.apache.dubbo.cache.CacheFactory} by extending {@link AbstractCacheFactory} and provide
 * instance of new {@link LruCache}.
 *
 * @see AbstractCacheFactory
 * @see LruCache
 * @see Cache
 */
public class LruCacheFactory extends AbstractCacheFactory {

    /**
     * Takes url as an method argument and return new instance of cache store implemented by LruCache.
     * @param url url of the method
     * @return ThreadLocalCache instance of cache
     */
    @Override
    protected Cache createCache(URL url) {
        return new LruCache(url);
    }

}
>>>>>>> origin/3.2
