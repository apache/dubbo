package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.UrlUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RunWith(PowerMockRunner.class)
public class UrlUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void convertRegisterInputNotNullOutput0() {

    // Arrange
    final HashMap<String, Map<String, String>> register =
        new HashMap<String, Map<String, String>>();

    // Act
    final Map<String, Map<String, String>> actual = UrlUtils.convertRegister(register);

    // Assert result
    final HashMap<String, Map<String, String>> hashMap = new HashMap<String, Map<String, String>>();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void convertSubscribeInputNotNullOutput0() {

    // Arrange
    final HashMap<String, String> subscribe = new HashMap<String, String>();

    // Act
    final Map<String, String> actual = UrlUtils.convertSubscribe(subscribe);

    // Assert result
    final HashMap<String, String> hashMap = new HashMap<String, String>();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isItemMatchInputNotNullNotNullOutputFalse() {

    // Arrange
    final String pattern = "3";
    final String value = "\'";

    // Act
    final boolean actual = UrlUtils.isItemMatch(pattern, value);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isItemMatchInputNotNullNotNullOutputTrue() {

    // Arrange
    final String pattern = "3";
    final String value = "3";

    // Act
    final boolean actual = UrlUtils.isItemMatch(pattern, value);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isItemMatchInputNotNullNullOutputTrue() {

    // Arrange
    final String pattern = "*";
    final String value = null;

    // Act
    final boolean actual = UrlUtils.isItemMatch(pattern, value);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isItemMatchInputNullNullOutputTrue() {

    // Arrange
    final String pattern = null;
    final String value = null;

    // Act
    final boolean actual = UrlUtils.isItemMatch(pattern, value);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isMatchCategoryInputNotNullNotNullOutputFalse() {

    // Arrange
    final String category = "1";
    final String categories = "foo";

    // Act
    final boolean actual = UrlUtils.isMatchCategory(category, categories);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isMatchCategoryInputNotNullNotNullOutputTrue() {

    // Arrange
    final String category = "9:>}";
    final String categories =
        "----:::::---------------------------------------------------------*<<<<<<<<<<<<<<<<<<<<9:>}--erb";

    // Act
    final boolean actual = UrlUtils.isMatchCategory(category, categories);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isMatchGlobPatternInputNotNullNullOutputTrue() {

    // Arrange
    final String pattern = "*";
    final String value = null;

    // Act
    final boolean actual = UrlUtils.isMatchGlobPattern(pattern, value);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @PrepareForTest({UrlUtils.class, Pattern.class})
  @Test
  public void parseURLsInputNotNull0OutputNull() throws Exception {

    // Arrange
    final String address = "foo";
    final HashMap<String, String> defaults = new HashMap<String, String>();

    // Act
    final List<URL> actual = UrlUtils.parseURLs(address, defaults);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void revertNotifyInputNotNullOutput0() {

    // Arrange
    final HashMap<String, Map<String, String>> notify = new HashMap<String, Map<String, String>>();

    // Act
    final Map<String, Map<String, String>> actual = UrlUtils.revertNotify(notify);

    // Assert result
    final HashMap<String, Map<String, String>> hashMap = new HashMap<String, Map<String, String>>();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void revertNotifyInputNullOutputNull() {

    // Arrange
    final Map<String, Map<String, String>> notify = null;

    // Act
    final Map<String, Map<String, String>> actual = UrlUtils.revertNotify(notify);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void revertRegisterInputNotNullOutput0() {

    // Arrange
    final HashMap<String, Map<String, String>> register =
        new HashMap<String, Map<String, String>>();

    // Act
    final Map<String, Map<String, String>> actual = UrlUtils.revertRegister(register);

    // Assert result
    final HashMap<String, Map<String, String>> hashMap = new HashMap<String, Map<String, String>>();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void revertSubscribeInputNotNullOutput0() {

    // Arrange
    final HashMap<String, String> subscribe = new HashMap<String, String>();

    // Act
    final Map<String, String> actual = UrlUtils.revertSubscribe(subscribe);

    // Assert result
    final HashMap<String, String> hashMap = new HashMap<String, String>();
    Assert.assertEquals(hashMap, actual);
  }
}
