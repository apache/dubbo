package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Dubbo Component Scan {@link Annotation},scans the classpath for annotated components that will be auto-registered as
 * Spring beans. Dubbo-provided {@link Service} and {@link Reference}.
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Service
 * @see Reference
 * @since 2.5.7
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DubboComponentScanRegistrar.class)
public @interface DubboComponentScan {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
     * declarations e.g.: {@code @DubboComponentScan("org.my.pkg")} instead of
     * {@code @DubboComponentScan(basePackages="org.my.pkg")}.
     *
     * @return the base packages to scan
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * Base packages to scan for annotated @Service classes. {@link #value()} is an
     * alias for (and mutually exclusive with) this attribute.
     * <p>
     * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
     * package names.
     *
     * @return the base packages to scan
     */
    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to
     * scan for annotated @Service classes. The package of each class specified will be
     * scanned.
     *
     * @return classes from the base packages to scan
     */
    Class<?>[] basePackageClasses() default {};

}
