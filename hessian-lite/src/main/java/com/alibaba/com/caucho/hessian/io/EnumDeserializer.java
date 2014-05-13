/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.alibaba.com.caucho.hessian.io;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Deserializing an enum valued object
 */
public class EnumDeserializer extends AbstractDeserializer {
  private Class _enumType;
  private Method _valueOf;
  
  public EnumDeserializer(Class cl)
  {
    // hessian/33b[34], hessian/3bb[78]
    if (cl.isEnum())
      _enumType = cl;
    else if (cl.getSuperclass().isEnum())
      _enumType = cl.getSuperclass();
    else
      throw new RuntimeException("Class " + cl.getName() + " is not an enum");

    try {
      _valueOf = _enumType.getMethod("valueOf",
			     new Class[] { Class.class, String.class });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public Class getType()
  {
    return _enumType;
  }
  
  public Object readMap(AbstractHessianInput in)
    throws IOException
  {
    String name = null;
    
    while (! in.isEnd()) {
      String key = in.readString();

      if (key.equals("name"))
        name = in.readString();
      else
	in.readObject();
    }

    in.readMapEnd();

    Object obj = create(name);
    
    in.addRef(obj);

    return obj;
  }
  
  public Object readObject(AbstractHessianInput in, String []fieldNames)
    throws IOException
  {
    String name = null;

    for (int i = 0; i < fieldNames.length; i++) {
      if ("name".equals(fieldNames[i]))
        name = in.readString();
      else
	in.readObject();
    }

    Object obj = create(name);

    in.addRef(obj);

    return obj;
  }

  private Object create(String name)
    throws IOException
  {
    if (name == null)
      throw new IOException(_enumType.getName() + " expects name.");

    try {
      return _valueOf.invoke(null, _enumType, name);
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    }
  }
}
