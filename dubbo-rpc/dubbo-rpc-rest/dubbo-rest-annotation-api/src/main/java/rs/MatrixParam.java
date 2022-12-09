/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package rs;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.Path;
import java.lang.annotation.*;

/**
 * Binds the value(s) of a URI matrix parameter to a resource method parameter,
 * resource class field, or resource class bean property.
 * Values are URL decoded unless this is disabled using the {@link javax.ws.rs.Encoded}
 * annotation. A default value can be specified using the {@link DefaultValue}
 * annotation.
 * <p>
 * Note that the {@code @MatrixParam} {@link #value() annotation value} refers
 * to a name of a matrix parameter that resides in the last matched path segment
 * of the {@link Path}-annotated Java structure that injects the value of the matrix
 * parameter.
 * </p>
 * <p>
 * The type {@code T} of the annotated parameter, field or property must
 * either:
 * </p>
 * <ol>
 * <li>Be a primitive type</li>
 * <li>Have a constructor that accepts a single {@code String} argument</li>
 * <li>Have a static method named {@code valueOf} or {@code fromString}
 * that accepts a single
 * {@code String} argument (see, for example, {@link Integer#valueOf(String)})</li>
 * <li>Have a registered implementation of {@link javax.ws.rs.ext.ParamConverterProvider}
 * JAX-RS extension SPI that returns a {@link javax.ws.rs.ext.ParamConverter}
 * instance capable of a "from string" conversion for the type.</li>
 * <li>Be {@code List<T>}, {@code Set<T>} or
 * {@code SortedSet<T>}, where {@code T} satisfies 2, 3 or 4 above.
 * The resulting collection is read-only.</li>
 * </ol>
 *
 * <p>If the type is not one of the collection types listed in 5 above and the
 * matrix parameter is represented by multiple values then the first value (lexically)
 * of the parameter is used.</p>
 *
 * <p>Because injection occurs at object creation time, use of this annotation
 * on resource class fields and bean properties is only supported for the
 * default per-request resource class lifecycle. Resource classes using
 * other lifecycles should only use this annotation on resource method
 * parameters.</p>
 *
 * @author Paul Sandoz
 * @author Marc Hadley
 * @see DefaultValue
 * @see Encoded
 * @see <a href="http://www.w3.org/DesignIssues/MatrixURIs.html">Matrix URIs</a>
 * @since 1.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MatrixParam {

    /**
     * Defines the name of the URI matrix parameter whose value will be used
     * to initialize the value of the annotated method argument, class field or
     * bean property. The name is specified in decoded form, any percent encoded
     * literals within the value will not be decoded and will instead be
     * treated as literal text. E.g. if the parameter name is "a b" then the
     * value of the annotation is "a b", <i>not</i> "a+b" or "a%20b".
     */
    String value();
}
