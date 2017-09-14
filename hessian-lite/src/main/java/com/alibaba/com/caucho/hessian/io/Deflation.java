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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Deflation extends HessianEnvelope {
    public Deflation() {
    }

    public Hessian2Output wrap(Hessian2Output out)
            throws IOException {
        OutputStream os = new DeflateOutputStream(out);

        Hessian2Output filterOut = new Hessian2Output(os);

        filterOut.setCloseStreamOnClose(true);

        return filterOut;
    }

    public Hessian2Input unwrap(Hessian2Input in)
            throws IOException {
        int version = in.readEnvelope();

        String method = in.readMethod();

        if (!method.equals(getClass().getName()))
            throw new IOException("expected hessian Envelope method '" +
                    getClass().getName() + "' at '" + method + "'");

        return unwrapHeaders(in);
    }

    public Hessian2Input unwrapHeaders(Hessian2Input in)
            throws IOException {
        InputStream is = new DeflateInputStream(in);

        Hessian2Input filter = new Hessian2Input(is);

        filter.setCloseStreamOnClose(true);

        return filter;
    }

    static class DeflateOutputStream extends OutputStream {
        private Hessian2Output _out;
        private OutputStream _bodyOut;
        private DeflaterOutputStream _deflateOut;

        DeflateOutputStream(Hessian2Output out)
                throws IOException {
            _out = out;

            _out.startEnvelope(Deflation.class.getName());

            _out.writeInt(0);

            _bodyOut = _out.getBytesOutputStream();

            _deflateOut = new DeflaterOutputStream(_bodyOut);
        }

        public void write(int ch)
                throws IOException {
            _deflateOut.write(ch);
        }

        public void write(byte[] buffer, int offset, int length)
                throws IOException {
            _deflateOut.write(buffer, offset, length);
        }

        public void close()
                throws IOException {
            Hessian2Output out = _out;
            _out = null;

            if (out != null) {
                _deflateOut.close();
                _bodyOut.close();

                out.writeInt(0);

                out.completeEnvelope();

                out.close();
            }
        }
    }

    static class DeflateInputStream extends InputStream {
        private Hessian2Input _in;

        private InputStream _bodyIn;
        private InflaterInputStream _inflateIn;

        DeflateInputStream(Hessian2Input in)
                throws IOException {
            _in = in;

            int len = in.readInt();

            if (len != 0)
                throw new IOException("expected no headers");

            _bodyIn = _in.readInputStream();

            _inflateIn = new InflaterInputStream(_bodyIn);
        }

        public int read()
                throws IOException {
            return _inflateIn.read();
        }

        public int read(byte[] buffer, int offset, int length)
                throws IOException {
            return _inflateIn.read(buffer, offset, length);
        }

        public void close()
                throws IOException {
            Hessian2Input in = _in;
            _in = null;

            if (in != null) {
                _inflateIn.close();
                _bodyIn.close();

                int len = in.readInt();

                if (len != 0)
                    throw new IOException("Unexpected footer");

                in.completeEnvelope();

                in.close();
            }
        }
    }
}
