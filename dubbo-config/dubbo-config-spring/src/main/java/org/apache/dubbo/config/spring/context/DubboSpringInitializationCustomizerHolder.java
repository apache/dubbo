package org.apache.dubbo.config.spring.context;

import java.util.HashSet;
import java.util.Set;

/**
 * Hold a set of DubboSpringInitializationCustomizer, for register customizers by programing.
 * <p>All customizers are store in thread local, and they will be clear after apply once.</p>
 *
 * <p>Usages:</p>
 *<pre>
 * DubboSpringInitializationCustomizerHolder.get().addCustomizer(customizer1);
 * ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(..);
 * ...
 * DubboSpringInitializationCustomizerHolder.get().addCustomizer(customizer2);
 * ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext(..);
 * </pre>
 */
public class DubboSpringInitializationCustomizerHolder {

    private static final ThreadLocal<DubboSpringInitializationCustomizerHolder> holders = ThreadLocal.withInitial(() ->
        new DubboSpringInitializationCustomizerHolder());

    public static DubboSpringInitializationCustomizerHolder get() {
        return holders.get();
    }

    private Set<DubboSpringInitializationCustomizer> customizers = new HashSet<>();

    public void addCustomizer(DubboSpringInitializationCustomizer customizer) {
        this.customizers.add(customizer);
    }

    public void clearCustomizers() {
        this.customizers = new HashSet<>();
    }

    public Set<DubboSpringInitializationCustomizer> getCustomizers() {
        return customizers;
    }

}
