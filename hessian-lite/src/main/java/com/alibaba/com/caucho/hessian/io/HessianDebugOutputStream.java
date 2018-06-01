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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Debugging output stream for Hessian requests.
 */
public class HessianDebugOutputStream extends OutputStream {
    private OutputStream _os;

    private HessianDebugState _state;

    /**
     * Creates an uninitialized Hessian input stream.
     */
    public HessianDebugOutputStream(OutputStream os, PrintWriter dbg) {
        _os = os;

        _state = new HessianDebugState(dbg);
    }

    /**
     * Creates an uninitialized Hessian input stream.
     */
    public HessianDebugOutputStream(OutputStream os, Logger log, Level level) {
        this(os, new PrintWriter(new LogWriter(log, level)));
    }

    public void startTop2() {
        _state.startTop2();
    }

    /**
     * Writes a character.
     */
    @Override
    public void write(int ch)
            throws IOException {
        ch = ch & 0xff;

        _os.write(ch);

        _state.next(ch);
    }

    @Override
    public void flush()
            throws IOException {
        _os.flush();
    }

    /**
     * closes the stream.
     */
    @Override
    public void close()
            throws IOException {
        OutputStream os = _os;
        _os = null;

        if (os != null)
            os.close();

        _state.println();
    }

    static class LogWriter extends Writer {
        private Logger _log;
        private Level _level;
        private StringBuilder _sb = new StringBuilder();

        LogWriter(Logger log, Level level) {
            _log = log;
            _level = level;
        }

        public void write(char ch) {
            if (ch == '\n' && _sb.length() > 0) {
                _log.log(_level, _sb.toString());
                _sb.setLength(0);
            } else
                _sb.append((char) ch);
        }

        @Override
        public void write(char[] buffer, int offset, int length) {
            for (int i = 0; i < length; i++) {
                char ch = buffer[offset + i];

                if (ch == '\n' && _sb.length() > 0) {
                    _log.log(_level, _sb.toString());
                    _sb.setLength(0);
                } else
                    _sb.append((char) ch);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
