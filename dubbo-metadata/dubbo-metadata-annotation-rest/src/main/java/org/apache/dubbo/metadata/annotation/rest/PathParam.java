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

package org.apache.dubbo.metadata.annotation.rest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.Path;
import java.lang.annotation.*;

/**
 * Binds the value of a URI template parameter or a path segment
 * containing the template parameter to a resource method parameter, resource
 * class field, or resource class
 * bean property. The value is URL decoded unless this
 * is disabled using the {@link javax.ws.rs.Encoded &#64;Encoded} annotation.
 * A default value can be specified using the {@link javax.ws.rs.DefaultValue &#64;DefaultValue}
 * annotation.
 *
 * The type of the annotated parameter, field or property must either:
 * <ul>
 * <li>Be {@link javax.ws.rs.core.PathSegment}, the value will be the final
 * segment of the matching part of the path.
 * See {@link javax.ws.rs.core.UriInfo} for a means of retrieving all request
 * path segments.</li>
 * <li>Be {@code List<javax.ws.rs.core.PathSegment>}, the
 * value will be a list of {@code PathSegment} corresponding to the path
 * segment(s) that matched the named template parameter.
 * See {@link javax.ws.rs.core.UriInfo} for a means of retrieving all request
 * path segments.</li>
 * <li>Be a primitive type.</li>
 * <li>Have a constructor that accepts a single String argument.</li>
 * <li>Have a static method named {@code valueOf} or {@code fromString}
 * that accepts a single
 * String argument (see, for example, {@link Integer#valueOf(String)}).</li>
 * <li>Have a registered implementation of {@link javax.ws.rs.ext.ParamConverterProvider}
 * JAX-RS extension SPI that returns a {@link javax.ws.rs.ext.ParamConverter}
 * instance capable of a "from string" conversion for the type.</li>
 * </ul>
 *
 * <p>The injected value corresponds to the latest use (in terms of scope) of
 * the path parameter. E.g. if a class and a sub-resource method are both
 * annotated with a {@link javax.ws.rs.Path &#64;Path} containing the same URI template
 * parameter, use of {@code @PathParam} on a sub-resource method parameter
 * will bind the value matching URI template parameter in the method's
 * {@code @Path} annotation.</p>
 *
 * <p>Because injection occurs at object creation time, use of this annotation
 * on resource class fields and bean properties is only supported for the
 * default per-request resource class lifecycle. Resource classes using
 * other lifecycles should only use this annotation on resource method
 * parameters.</p>
 *
 * @author Paul Sandoz
 * @author Marc Hadley
 * @see Encoded &#64;Encoded
 * @see DefaultValue &#64;DefaultValue
 * @see javax.ws.rs.core.PathSegment
 * @see javax.ws.rs.core.UriInfo
 * @since 1.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathParam {

    /**
     * Defines the name of the URI template parameter whose value will be used
     * to initialize the value of the annotated method parameter, class field or
     * property. See {@link Path#value()} for a description of the syntax of
     * template parameters.
     *
     * <p>E.g. a class annotated with: {@code @Path("widgets/{id}")}
     * can have methods annotated whose arguments are annotated
     * with {@code @PathParam("id")}.
     */
    String value();
}
