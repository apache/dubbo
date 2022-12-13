/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;

/**
 * Associates the name of a HTTP method with an annotation. A Java method annotated
 * with a runtime annotation that is itself annotated with this annotation will
 * be used to handle HTTP requests of the indicated HTTP method. It is an error
 * for a method to be annotated with more than one annotation that is annotated
 * with {@code HttpMethod}.
 *
 * @author Paul Sandoz
 * @author Marc Hadley
 * @see javax.ws.rs.GET
 * @see javax.ws.rs.POST
 * @see javax.ws.rs.PUT
 * @see javax.ws.rs.DELETE
 * @see javax.ws.rs.PATCH
 * @see javax.ws.rs.HEAD
 * @see javax.ws.rs.OPTIONS
 * @since 1.0
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpMethod {

    /**
     * HTTP GET method.
     */
    public static final String GET = "GET";
    /**
     * HTTP POST method.
     */
    public static final String POST = "POST";
    /**
     * HTTP PUT method.
     */
    public static final String PUT = "PUT";
    /**
     * HTTP DELETE method.
     */
    public static final String DELETE = "DELETE";
    /**
     * HTTP PATCH method.
     *
     * @since 2.1
     */
    public static final String PATCH = "PATCH";
    /**
     * HTTP HEAD method.
     */
    public static final String HEAD = "HEAD";
    /**
     * HTTP OPTIONS method.
     */
    public static final String OPTIONS = "OPTIONS";

    /**
     * Specifies the name of a HTTP method. E.g. "GET".
     */
    String value();
}
