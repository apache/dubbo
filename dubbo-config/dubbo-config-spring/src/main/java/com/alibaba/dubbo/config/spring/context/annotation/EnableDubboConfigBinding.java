package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.AbstractConfig;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.DubboConfigBindingBeanPostProcessor;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertySources;

import java.lang.annotation.*;

/**
 * Enables Spring's annotation-driven {@link AbstractConfig Dubbo Config} from {@link PropertySources properties}.
 * <p>
 * Default , {@link #prefix()} associates with a prefix of {@link PropertySources properties}, e,g. "dubbo.application."
 * or "dubbo.application"
 * <pre class="code">
 * <p>
 * </pre>
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see DubboConfigBindingRegistrar
 * @see DubboConfigBindingBeanPostProcessor
 * @see EnableDubboConfigBindings
 * @since 2.5.8
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DubboConfigBindingRegistrar.class)
public @interface EnableDubboConfigBinding {

    /**
     * The name prefix of the properties that are valid to bind to {@link AbstractConfig Dubbo Config}.
     *
     * @return the name prefix of the properties to bind
     */
    String prefix();

    /**
     * @return The binding type of {@link AbstractConfig Dubbo Config}.
     * @see AbstractConfig
     * @see ApplicationConfig
     * @see ModuleConfig
     * @see RegistryConfig
     */
    Class<? extends AbstractConfig> type();

    /**
     * It indicates whether {@link #prefix()} binding to multiple Spring Beans.
     *
     * @return the default value is <code>false</code>
     */
    boolean multiple() default false;

}
