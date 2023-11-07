/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils.json;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Service {
    String sayHi(String name);

    List<String> testList();

    int testInt();

    int[] testIntArr();

    Integer testInteger();

    Integer[] testIntegerArr();

    List<Integer> testIntegerList();

    short testShort();

    short[] testShortArr();

    Short testSShort();

    Short[] testSShortArr();

    List<Short> testShortList();

    byte testByte();

    byte[] testByteArr();

    Byte testBByte();

    Byte[] testBByteArr();

    ArrayList<Byte> testByteList();

    float testFloat();

    float[] testFloatArr();

    Float testFFloat();

    Float[] testFloatArray();

    List<Float> testFloatList();

    boolean testBoolean();

    boolean[] testBooleanArr();

    Boolean testBBoolean();

    Boolean[] testBooleanArray();

    List<Boolean> testBooleanList();

    char testChar();

    char[] testCharArr();

    Character testCharacter();

    Character[] testCharacterArr();

    List<Character> testCharacterList();

    List<Character[]> testCharacterListArr();

    String testString();

    String[] testStringArr();

    List<String> testStringList();

    List<String[]> testStringListArr();

    String testNull();

    Date testDate();

    Calendar testCalendar();

    LocalTime testLocalTime();

    LocalDate testLocalDate();

    LocalDateTime testLocalDateTime();

    ZonedDateTime testZoneDateTime();

    Map<Integer, String> testMap();

    Set<Integer> testSet();

    Optional<Integer> testOptionalEmpty();

    Optional<Integer> testOptionalInteger();

    Optional<String> testOptionalString();

    Color testEnum();

    Range testRecord();

    Printer testInterface();

    Teacher testObject();

    List<Teacher> testObjectList();

    Student<Integer> testTemplate();

    InputStream testStream() throws FileNotFoundException;

    Iterator<String> testIterator();

    AbstractObject testAbstract();
}
