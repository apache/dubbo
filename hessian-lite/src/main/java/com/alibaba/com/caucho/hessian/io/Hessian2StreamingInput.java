/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
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
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
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
import java.io.InputStream;

/**
 * Output stream for Hessian 2 streaming requests.
 */
public class Hessian2StreamingInput
{
  private Hessian2Input _in;
  
  /**
   * Creates a new Hessian input stream, initialized with an
   * underlying input stream.
   *
   * @param is the underlying output stream.
   */
  public Hessian2StreamingInput(InputStream is)
  {
    _in = new Hessian2Input(new StreamingInputStream(is));
  }

  /**
   * Read the next object
   */
  public Object readObject()
    throws IOException
  {
    return _in.readStreamingObject();
  }

  /**
   * Close the output.
   */
  public void close()
    throws IOException
  {
    _in.close();
  }

  static class StreamingInputStream extends InputStream {
    private InputStream _is;
    private int _length;

    StreamingInputStream(InputStream is)
    {
      _is = is;
    }

    public int read()
      throws IOException
    {
      InputStream is = _is;
      
      while (_length == 0) {
	int code = is.read();

	if (code < 0)
	  return -1;
	else if (code != 'p' && code != 'P')
	  throw new HessianProtocolException("expected streaming packet at 0x"
					     + Integer.toHexString(code & 0xff));

	int d1 = is.read();
	int d2 = is.read();

	if (d2 < 0)
	  return -1;
	
	_length = (d1 << 8) + d2;
      }

      _length--;
      return is.read();
    }

    public int read(byte []buffer, int offset, int length)
      throws IOException
    {
      InputStream is = _is;
      
      while (_length == 0) {
	int code = is.read();

	if (code < 0)
	  return -1;
	else if (code != 'p' && code != 'P') {
	  throw new HessianProtocolException("expected streaming packet at 0x"
					     + Integer.toHexString(code & 0xff)
					     + " (" + (char) code + ")");
	}

	int d1 = is.read();
	int d2 = is.read();

	if (d2 < 0)
	  return -1;
	
	_length = (d1 << 8) + d2;
      }

      int sublen = _length;
      if (length < sublen)
	sublen = length;

      sublen = is.read(buffer, offset, sublen);

      if (sublen < 0)
	return -1;

      _length -= sublen;

      return sublen;
    }
  }
}
