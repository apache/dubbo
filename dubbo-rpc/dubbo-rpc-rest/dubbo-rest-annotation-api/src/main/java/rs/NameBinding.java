/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.HttpMethod;
import java.lang.annotation.*;

/**
 * Meta-annotation used to create name binding annotations for filters
 * and interceptors.
 * <p>
 * Name binding via annotations is only supported as part of the Server API.
 * In name binding, a <i>name-binding</i> annotation is first defined using the
 * {@code @NameBinding} meta-annotation:
 *
 * <pre>
 *  &#64;Target({ ElementType.TYPE, ElementType.METHOD })
 *  &#64;Retention(value = RetentionPolicy.RUNTIME)
 *  <b>&#64;NameBinding</b>
 *  <b>public @interface Logged</b> { }
 * </pre>
 *
 * The defined name-binding annotation is then used to decorate a filter or interceptor
 * class (more than one filter or interceptor may be decorated with the same name-binding
 * annotation):
 *
 * <pre>
 *  <b>&#64;Logged</b>
 *  public class LoggingFilter
 *          implements ContainerRequestFilter, ContainerResponseFilter {
 *      ...
 *  }
 * </pre>
 *
 * At last, the name-binding annotation is applied to the resource method(s) to which the
 * name-bound JAX-RS provider(s) should be bound to:
 *
 * <pre>
 *  &#64;Path("/")
 *  public class MyResourceClass {
 *      &#64;GET
 *      &#64;Produces("text/plain")
 *      &#64;Path("{name}")
 *      <b>&#64;Logged</b>
 *      public String hello(@PathParam("name") String name) {
 *          return "Hello " + name;
 *      }
 *  }
 * </pre>
 *
 * A name-binding annotation may also be attached to a custom JAX-RS
 * {@link javax.ws.rs.core.Application} subclass. In such case a name-bound JAX-RS provider
 * bound by the annotation will be applied to all {@link HttpMethod resource and sub-resource
 * methods} in the JAX-RS application:
 *
 * <pre>
 *  <b>&#64;Logged</b>
 *  &#64;ApplicationPath("myApp")
 *  public class MyApplication extends javax.ws.rs.core.Application {
 *      ...
 *  }
 * </pre>
 * </p>
 *
 * @author Santiago Pericas-Geertsen
 * @author Marek Potociar
 * @since 2.0
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NameBinding {
}
