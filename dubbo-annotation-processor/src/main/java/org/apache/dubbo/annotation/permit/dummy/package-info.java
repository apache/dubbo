/*
 * Authored by Project Lombok and licensed by MIT License, which is attached below:
 *
 * Copyright (C) 2018-2021 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * This package recreates the type hierarchy of {@code java.lang.reflect.AccessibleObject} and friends (such as {@code java.lang.reflect.Method});
 * its purpose is to allow us to ask {@code sun.misc.internal.Unsafe} about the exact offset of the {@code override} field of {@code AccessibleObject};
 * asking about that field directly doesn't work after jdk14, presumably because the fields of AO are expressly hidden somehow.
 * <p>
 * NB: It's usually 12, on the vast majority of OS, VM, and architecture combos.
 */
package org.apache.dubbo.annotation.permit.dummy;
