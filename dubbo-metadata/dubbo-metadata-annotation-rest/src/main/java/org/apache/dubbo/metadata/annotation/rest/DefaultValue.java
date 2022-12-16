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

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.*;
import java.lang.annotation.*;

/**
 * Defines the default value of request meta-data that is bound using one of the
 * following annotations:
 * {@link PathParam},
 * {@link javax.ws.rs.QueryParam},
 * {@link javax.ws.rs.MatrixParam},
 * {@link CookieParam},
 * {@link javax.ws.rs.FormParam},
 * or {@link javax.ws.rs.HeaderParam}.
 * The default value is used if the corresponding meta-data is not present in the
 * request.
 * <p>
 * If the type of the annotated parameter is {@link java.util.List},
 * {@link java.util.Set} or {@link java.util.SortedSet} then the resulting collection
 * will have a single entry mapped from the supplied default value.
 * </p>
 * <p>
 * If this annotation is not used and the corresponding meta-data is not
 * present in the request, the value will be an empty collection for
 * {@code List}, {@code Set} or {@code SortedSet}, {@code null} for
 * other object types, and the Java-defined default for primitive types.
 * </p>
 *
 * @author Paul Sandoz
 * @author Marc Hadley
 * @see PathParam
 * @see QueryParam
 * @see FormParam
 * @see HeaderParam
 * @see MatrixParam
 * @see CookieParam
 * @since 1.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DefaultValue {

    /**
     * The specified default value.
     */
    String value();
}
