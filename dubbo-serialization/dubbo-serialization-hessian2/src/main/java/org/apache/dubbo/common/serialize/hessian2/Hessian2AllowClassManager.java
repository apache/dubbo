package org.apache.dubbo.common.serialize.hessian2;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Arrays;
import java.util.Set;

/**
 * Inspired by Fastjson2
 * see com.alibaba.fastjson2.filter.ContextAutoTypeBeforeHandler#apply(java.lang.String, java.lang.Class, long)
 */
public class Hessian2AllowClassManager implements AllowClassNotifyListener {
    private static final long MAGIC_HASH_CODE = 0xcbf29ce484222325L;
    private static final long MAGIC_PRIME = 0x100000001b3L;
    private static final Logger logger = LoggerFactory.getLogger(Hessian2AllowClassManager.class);
    private volatile SerializeCheckStatus checkStatus = AllowClassNotifyListener.DEFAULT_STATUS;
    private final static Set<String> warnedClasses = new ConcurrentHashSet<>(1);
    private volatile long[] allowPrefixes = new long[0];

    public Hessian2AllowClassManager(FrameworkModel frameworkModel) {
        SerializeSecurityManager serializeSecurityManager = frameworkModel.getBeanFactory().getOrRegisterBean(SerializeSecurityManager.class);
        serializeSecurityManager.registerListener(this);
    }

    @Override
    public void notify(SerializeCheckStatus status, Set<String> prefixList) {
        this.checkStatus = status;
        long[] array = new long[prefixList.size()];

        int index = 0;
        for (String name : prefixList) {
            if (name == null || name.isEmpty()) {
                continue;
            }

            long hashCode = MAGIC_HASH_CODE;
            for (int j = 0; j < name.length(); ++j) {
                char ch = name.charAt(j);
                hashCode ^= ch;
                hashCode *= MAGIC_PRIME;
            }

            array[index++] = hashCode;
        }

        if (index != array.length) {
            array = Arrays.copyOf(array, index);
        }
        Arrays.sort(array);
        this.allowPrefixes = array;
    }

    public Class<?> loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (checkStatus == SerializeCheckStatus.DISABLED) {
            return Class.forName(className, false, classLoader);
        }

        long hash = MAGIC_HASH_CODE;
        for (int i = 0, typeNameLength = className.length(); i < typeNameLength; ++i) {
            char ch = className.charAt(i);
            if (ch == '$') {
                ch = '.';
            }
            hash ^= ch;
            hash *= MAGIC_PRIME;

            if (Arrays.binarySearch(allowPrefixes, hash) >= 0) {
                return Class.forName(className, false, classLoader);
            }
        }

        if (checkStatus == SerializeCheckStatus.STRICT) {
            throw new ClassNotFoundException(className);
        } else {
            Class<?> clazz = Class.forName(className, false, classLoader);
            if (warnedClasses.add(className)) {
                logger.error("[Serialization Security] Serialized class " + clazz.getName() + " is not in allow list. " +
                    "Current mode is `WARN`, will allow to deserialize it by default. " +
                    "Dubbo will set to `STRICT` mode by default in the future. " +
                    "Please add it into security/serialize.allowlist or follow FAQ to configure it.");
            }
            return clazz;
        }
    }
}
