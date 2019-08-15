package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void isEmptyMapInput1OutputFalse() {

    // Arrange
    final HashMap map = new HashMap();
    map.put(null, null);

    // Act
    final boolean actual = CollectionUtils.isEmptyMap(map);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEmptyMapInputNullOutputTrue() {

    // Arrange
    final Map map = null;

    // Act
    final boolean actual = CollectionUtils.isEmptyMap(map);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinAllInputNotNullNotNullOutput0() {

    // Arrange
    final HashMap<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
    final String separator = ",";

    // Act
    final Map<String, List<String>> actual = CollectionUtils.joinAll(map, separator);

    // Assert result
    final HashMap<String, List<String>> hashMap = new HashMap<String, List<String>>();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinAllInputNullNotNullOutputNull() {

    // Arrange
    final Map<String, Map<String, String>> map = null;
    final String separator = ",";

    // Act
    final Map<String, List<String>> actual = CollectionUtils.joinAll(map, separator);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void joinInput0NotNullOutput0() {

    // Arrange
    final HashMap<String, String> map = new HashMap<String, String>();
    final String separator = "2";

    // Act
    final List<String> actual = CollectionUtils.join(map, separator);

    // Assert result
    final ArrayList<String> arrayList = new ArrayList<String>();
    Assert.assertEquals(arrayList, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInput0NotNullOutputNotNull() {

    // Arrange
    final ArrayList<String> list = new ArrayList<String>();
    final String separator = "foo";

    // Act
    final String actual = CollectionUtils.join(list, separator);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInput1NotNullOutputNotNull() {

    // Arrange
    final ArrayList<String> list = new ArrayList<String>();
    list.add("1");
    final String separator = "foo";

    // Act
    final String actual = CollectionUtils.join(list, separator);

    // Assert result
    Assert.assertEquals("1", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInputNullNotNullOutputNull() {

    // Arrange
    final Map<String, String> map = null;
    final String separator = "/";

    // Act
    final List<String> actual = CollectionUtils.join(map, separator);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void mapEqualsInput00OutputTrue() {

    // Arrange
    final HashMap map1 = new HashMap();
    final HashMap map2 = new HashMap();

    // Act
    final boolean actual = CollectionUtils.mapEquals(map1, map2);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void mapEqualsInput1NullOutputFalse() {

    // Arrange
    final HashMap map1 = new HashMap();
    map1.put(null, null);
    final Map map2 = null;

    // Act
    final boolean actual = CollectionUtils.mapEquals(map1, map2);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void mapEqualsInput01OutputFalse() {

    // Arrange
    final HashMap map1 = new HashMap();
    final HashMap map2 = new HashMap();
    map2.put(null, null);

    // Act
    final boolean actual = CollectionUtils.mapEquals(map1, map2);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void mapEqualsInputNullNullOutputTrue() {

    // Arrange
    final Map map1 = null;
    final Map map2 = null;

    // Act
    final boolean actual = CollectionUtils.mapEquals(map1, map2);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void sortSimpleNameInput0Output0() {

    // Arrange
    final ArrayList<String> list = new ArrayList<String>();

    // Act
    final List<String> actual = CollectionUtils.sortSimpleName(list);

    // Assert result
    final ArrayList<String> arrayList = new ArrayList<String>();
    Assert.assertEquals(arrayList, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void sortSimpleNameInputNullOutputNull() {

    // Arrange
    final List<String> list = null;

    // Act
    final List<String> actual = CollectionUtils.sortSimpleName(list);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void splitAllInputNotNullNotNullOutput0() {

    // Arrange
    final HashMap<String, List<String>> list = new HashMap<String, List<String>>();
    final String separator = ",";

    // Act
    final Map<String, Map<String, String>> actual = CollectionUtils.splitAll(list, separator);

    // Assert result
    final HashMap<String, Map<String, String>> hashMap = new HashMap<String, Map<String, String>>();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void splitAllInputNullNotNullOutputNull() {

    // Arrange
    final Map<String, List<String>> list = null;
    final String separator = ",";

    // Act
    final Map<String, Map<String, String>> actual = CollectionUtils.splitAll(list, separator);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void splitInput0NotNullOutput0() {

    // Arrange
    final ArrayList<String> list = new ArrayList<String>();
    final String separator = "foo";

    // Act
    final Map<String, String> actual = CollectionUtils.split(list, separator);

    // Assert result
    final HashMap<String, String> hashMap = new HashMap<String, String>();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void splitInput1NotNullOutput1() {

    // Arrange
    final ArrayList<String> list = new ArrayList<String>();
    list.add("");
    final String separator = "??";

    // Act
    final Map<String, String> actual = CollectionUtils.split(list, separator);

    // Assert result
    final HashMap<String, String> hashMap = new HashMap<String, String>();
    hashMap.put("", "");
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void splitInput1NotNullOutput12() {

    // Arrange
    final ArrayList<String> list = new ArrayList<String>();
    list.add("\uf000");
    final String separator = "";

    // Act
    final Map<String, String> actual = CollectionUtils.split(list, separator);

    // Assert result
    final HashMap<String, String> hashMap = new HashMap<String, String>();
    hashMap.put("", "");
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void splitInput1NullOutputNullPointerException() {

    // Arrange
    final ArrayList<String> list = new ArrayList<String>();
    list.add(null);
    final String separator = null;

    // Act
    thrown.expect(NullPointerException.class);
    CollectionUtils.split(list, separator);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void splitInputNullNotNullOutputNull() {

    // Arrange
    final List<String> list = null;
    final String separator = "3";

    // Act
    final Map<String, String> actual = CollectionUtils.split(list, separator);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void toMapInput0Output0() {

    // Arrange
    final Object[] pairs = {};

    // Act
    final Map actual = CollectionUtils.toMap(pairs);

    // Assert result
    final HashMap hashMap = new HashMap();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void toMapInput1OutputIllegalArgumentException() {

    // Arrange
    final Object[] pairs = {null};

    // Act
    thrown.expect(IllegalArgumentException.class);
    CollectionUtils.toMap(pairs);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void toMapInput2Output1() {

    // Arrange
    final Object[] pairs = {null, null};

    // Act
    final Map actual = CollectionUtils.toMap(pairs);

    // Assert result
    final HashMap hashMap = new HashMap();
    hashMap.put(null, null);
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toMapInputNullOutput0() {

    // Arrange
    final Object[] pairs = null;

    // Act
    final Map actual = CollectionUtils.toMap(pairs);

    // Assert result
    final HashMap hashMap = new HashMap();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toStringMapInput0Output0() {

    // Arrange
    final String[] pairs = {};

    // Act
    final Map<String, String> actual = CollectionUtils.toStringMap(pairs);

    // Assert result
    final HashMap<String, String> hashMap = new HashMap<String, String>();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void toStringMapInput1OutputIllegalArgumentException() {

    // Arrange
    final String[] pairs = {null};

    // Act
    thrown.expect(IllegalArgumentException.class);
    CollectionUtils.toStringMap(pairs);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void toStringMapInput2Output1() {

    // Arrange
    final String[] pairs = {null, null};

    // Act
    final Map<String, String> actual = CollectionUtils.toStringMap(pairs);

    // Assert result
    final HashMap<String, String> hashMap = new HashMap<String, String>();
    hashMap.put(null, null);
    Assert.assertEquals(hashMap, actual);
  }
}
