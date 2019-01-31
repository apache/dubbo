/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.time;

import java.text.FieldPosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * DatePrinter interface
 */
public interface DatePrinter {

    /**
     * <p>Formats a millisecond {@code long} value.</p>
     *
     * @param millis  the millisecond value to format
     * @return the formatted string
     * @since 2.1
     */
    String format(long millis);

    /**
     * <p>Formats a {@code Date} object using a {@code GregorianCalendar}.</p>
     *
     * @param date  the date to format
     * @return the formatted string
     */
    String format(Date date);

    /**
     * <p>Formats a {@code Calendar} object.</p>
     * The TimeZone set on the Calendar is only used to adjust the time offset.
     * The TimeZone specified during the construction of the Parser will determine the TimeZone
     * used in the formatted string.
     *
     * @param calendar  the calendar to format.
     * @return the formatted string
     */
    String format(Calendar calendar);

    /**
     * <p>Formats a millisecond {@code long} value into the
     * supplied {@code StringBuffer}.</p>
     *
     * @param millis  the millisecond value to format
     * @param buf  the buffer to format into
     * @return the specified string buffer
     * @deprecated Use {{@link #format(long, Appendable)}.
     */
    @Deprecated
    StringBuffer format(long millis, StringBuffer buf);

    /**
     * <p>Formats a {@code Date} object into the
     * supplied {@code StringBuffer} using a {@code GregorianCalendar}.</p>
     *
     * @param date  the date to format
     * @param buf  the buffer to format into
     * @return the specified string buffer
     * @deprecated Use {{@link #format(Date, Appendable)}.
     */
    @Deprecated
    StringBuffer format(Date date, StringBuffer buf);

    /**
     * <p>Formats a {@code Calendar} object into the supplied {@code StringBuffer}.</p>
     * The TimeZone set on the Calendar is only used to adjust the time offset.
     * The TimeZone specified during the construction of the Parser will determine the TimeZone
     * used in the formatted string.
     *
     * @param calendar  the calendar to format
     * @param buf  the buffer to format into
     * @return the specified string buffer
     * @deprecated Use {{@link #format(Calendar, Appendable)}.
     */
    @Deprecated
    StringBuffer format(Calendar calendar, StringBuffer buf);

    /**
     * <p>Formats a millisecond {@code long} value into the
     * supplied {@code Appendable}.</p>
     *
     * @param millis  the millisecond value to format
     * @param buf  the buffer to format into
     * @param <B> the Appendable class type, usually StringBuilder or StringBuffer.
     * @return the specified string buffer
     * @since 3.5
     */
    <B extends Appendable> B format(long millis, B buf);

    /**
     * <p>Formats a {@code Date} object into the
     * supplied {@code Appendable} using a {@code GregorianCalendar}.</p>
     *
     * @param date  the date to format
     * @param buf  the buffer to format into
     * @param <B> the Appendable class type, usually StringBuilder or StringBuffer.
     * @return the specified string buffer
     * @since 3.5
     */
    <B extends Appendable> B format(Date date, B buf);

    /**
     * <p>Formats a {@code Calendar} object into the supplied {@code Appendable}.</p>
     * The TimeZone set on the Calendar is only used to adjust the time offset.
     * The TimeZone specified during the construction of the Parser will determine the TimeZone
     * used in the formatted string.
     *
     * @param calendar  the calendar to format
     * @param buf  the buffer to format into
     * @param <B> the Appendable class type, usually StringBuilder or StringBuffer.
     * @return the specified string buffer
     * @since 3.5
     */
    <B extends Appendable> B format(Calendar calendar, B buf);


    // Accessors
    //-----------------------------------------------------------------------
    /**
     * <p>Gets the pattern used by this printer.</p>
     *
     * @return the pattern, {@link java.text.SimpleDateFormat} compatible
     */
    String getPattern();

    /**
     * <p>Gets the time zone used by this printer.</p>
     *
     * <p>This zone is always used for {@code Date} printing. </p>
     *
     * @return the time zone
     */
    TimeZone getTimeZone();

    /**
     * <p>Gets the locale used by this printer.</p>
     *
     * @return the locale
     */
    Locale getLocale();

    /**
     * <p>Formats a {@code Date}, {@code Calendar} or
     * {@code Long} (milliseconds) object.</p>
     *
     * @param obj  the object to format
     * @param toAppendTo  the buffer to append to
     * @param pos  the position - ignored
     * @return the buffer passed in
     * @see java.text.DateFormat#format(Object, StringBuffer, FieldPosition)
     */
    StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos);
}