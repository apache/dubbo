package org.apache.dubbo.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate alias for field when generic invoking. <br>
 * <p>
 *     sometimes you receive json string which contains non-camelcase key name,e.g. order_id,
 *     but the java bean needs camelcase name,e.g orderId, you should convert manually each other,
 *     basing GenericAlias annotation it could be converted automatically.
 * </p>
 *
 * @author rolandhe
 * @date 2019/6/11下午9:33
 */
@Target({ElementType.METHOD,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GenericAlias {
    String value();
}
