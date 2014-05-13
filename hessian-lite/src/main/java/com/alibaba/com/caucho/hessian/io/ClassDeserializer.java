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
import java.util.HashMap;

/**
 * Deserializing a JDK 1.2 Class.
 */
public class ClassDeserializer extends AbstractMapDeserializer {
  private static final HashMap<String,Class> _primClasses
    = new HashMap<String,Class>();

  private ClassLoader _loader;
  
  public ClassDeserializer(ClassLoader loader)
  {
    _loader = loader;
  }
  
  public Class getType()
  {
    return Class.class;
  }
  
  public Object readMap(AbstractHessianInput in)
    throws IOException
  {
    int ref = in.addRef(null);
    
    String name = null;
    
    while (! in.isEnd()) {
      String key = in.readString();

      if (key.equals("name"))
	name = in.readString();
      else
	in.readObject();
    }
      
    in.readMapEnd();

    Object value = create(name);

    in.setRef(ref, value);

    return value;
  }
  
  public Object readObject(AbstractHessianInput in, String []fieldNames)
    throws IOException
  {
    int ref = in.addRef(null);
    
    String name = null;
    
    for (int i = 0; i < fieldNames.length; i++) {
      if ("name".equals(fieldNames[i]))
        name = in.readString();
      else
	in.readObject();
    }

    Object value = create(name);

    in.setRef(ref, value);

    return value;
  }

  Object create(String name)
    throws IOException
  {
    if (name == null)
      throw new IOException("Serialized Class expects name.");

    Class cl = _primClasses.get(name);

    if (cl != null)
      return cl;

    try {
      if (_loader != null)
        return Class.forName(name, false, _loader);
      else
        return Class.forName(name);
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    }
  }

  static {
    _primClasses.put("void", void.class);
    _primClasses.put("boolean", boolean.class);
    _primClasses.put("java.lang.Boolean", Boolean.class);
    _primClasses.put("byte", byte.class);
    _primClasses.put("java.lang.Byte", Byte.class);
    _primClasses.put("char", char.class);
    _primClasses.put("java.lang.Character", Character.class);
    _primClasses.put("short", short.class);
    _primClasses.put("java.lang.Short", Short.class);
    _primClasses.put("int", int.class);
    _primClasses.put("java.lang.Integer", Integer.class);
    _primClasses.put("long", long.class);
    _primClasses.put("java.lang.Long", Long.class);
    _primClasses.put("float", float.class);
    _primClasses.put("java.lang.Float", Float.class);
    _primClasses.put("double", double.class);
    _primClasses.put("java.lang.Double", Double.class);
    _primClasses.put("java.lang.String", String.class);
  }
}
