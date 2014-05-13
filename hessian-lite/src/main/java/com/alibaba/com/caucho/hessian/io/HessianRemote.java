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

/**
 * Encapsulates a remote address when no stub is available, e.g. for
 * Java MicroEdition.
 */
public class HessianRemote {
  private String type;
  private String url;

  /**
   * Creates a new Hessian remote object.
   *
   * @param type the remote stub interface
   * @param url the remote url
   */
  public HessianRemote(String type, String url)
  {
    this.type = type;
    this.url = url;
  }

  /**
   * Creates an uninitialized Hessian remote.
   */
  public HessianRemote()
  {
  }

  /**
   * Returns the remote api class name.
   */
  public String getType()
  {
    return type;
  }

  /**
   * Returns the remote URL.
   */
  public String getURL()
  {
    return url;
  }

  /**
   * Sets the remote URL.
   */
  public void setURL(String url)
  {
    this.url = url;
  }

  /**
   * Defines the hashcode.
   */
  public int hashCode()
  {
    return url.hashCode();
  }

  /**
   * Defines equality
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof HessianRemote))
      return false;

    HessianRemote remote = (HessianRemote) obj;

    return url.equals(remote.url);
  }

  /**
   * Readable version of the remote.
   */
  public String toString()
  {
    return "[HessianRemote " + url + "]";
  }
}
