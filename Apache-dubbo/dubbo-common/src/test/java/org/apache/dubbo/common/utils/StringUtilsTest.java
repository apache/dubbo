package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.utils.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class StringUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getServiceKeyInputNullOutputNullPointerException() {

    // Arrange
    final Map<String, String> ps = null;

    // Act
    thrown.expect(NullPointerException.class);
    StringUtils.getServiceKey(ps);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void isAllUpperCaseInputNullOutputFalse() {

    // Arrange
    final String str = null;

    // Act
    final boolean actual = StringUtils.isAllUpperCase(str);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEmptyInputNotNullOutputFalse() {

    // Arrange
    final String str = "3";

    // Act
    final boolean actual = StringUtils.isEmpty(str);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEmptyInputNullOutputTrue() {

    // Arrange
    final String str = null;

    // Act
    final boolean actual = StringUtils.isEmpty(str);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEqualsInputNotNullNotNullOutputTrue() {

    // Arrange
    final String s1 = ",";
    final String s2 = ",";

    // Act
    final boolean actual = StringUtils.isEquals(s1, s2);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEqualsInputNullNotNullOutputFalse() {

    // Arrange
    final String s1 = null;
    final String s2 = "";

    // Act
    final boolean actual = StringUtils.isEquals(s1, s2);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEqualsInputNullNullOutputTrue() {

    // Arrange
    final String s1 = null;
    final String s2 = null;

    // Act
    final boolean actual = StringUtils.isEquals(s1, s2);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNoneEmptyInputNullOutputFalse() {

    // Arrange
    final String[] ss = null;

    // Act
    final boolean actual = StringUtils.isNoneEmpty(ss);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNumericInputNotNullFalseOutputFalse() {

    // Arrange
    final String str = "foo";
    final boolean allowDot = false;

    // Act
    final boolean actual = StringUtils.isNumeric(str, allowDot);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNumericInputNotNullFalseOutputFalse2() {

    // Arrange
    final String str = ".";
    final boolean allowDot = false;

    // Act
    final boolean actual = StringUtils.isNumeric(str, allowDot);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNumericInputNotNullFalseOutputTrue() {

    // Arrange
    final String str = "1";
    final boolean allowDot = false;

    // Act
    final boolean actual = StringUtils.isNumeric(str, allowDot);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNumericInputNotNullTrueOutputFalse() {

    // Arrange
    final String str = "8..";
    final boolean allowDot = true;

    // Act
    final boolean actual = StringUtils.isNumeric(str, allowDot);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNumericInputNullFalseOutputFalse() {

    // Arrange
    final String str = null;
    final boolean allowDot = false;

    // Act
    final boolean actual = StringUtils.isNumeric(str, allowDot);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInput1NotNullOutputNotNull() {

    // Arrange
    final String[] array = {"1"};
    final char split = '!';

    // Act
    final String actual = StringUtils.join(array, split);

    // Assert result
    Assert.assertEquals("1", actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void joinInput1NotNullOutputNotNull2() {

    // Arrange
    final String[] array = {"3"};
    final String split = "1";

    // Act
    final String actual = StringUtils.join(array, split);

    // Assert result
    Assert.assertEquals("3", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInput1OutputNotNull() {

    // Arrange
    final String[] array = {"foo"};

    // Act
    final String actual = StringUtils.join(array);

    // Assert result
    Assert.assertEquals("foo", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInput2NotNullOutputNotNull() {

    // Arrange
    final String[] array = {"foo", "foo"};
    final char split = '!';

    // Act
    final String actual = StringUtils.join(array, split);

    // Assert result
    Assert.assertEquals("foo!foo", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInput2NotNullOutputNotNull2() {

    // Arrange
    final String[] array = {"foo", "foo"};
    final String split = "foo";

    // Act
    final String actual = StringUtils.join(array, split);

    // Assert result
    Assert.assertEquals("foofoofoo", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInputNullNotNullOutputNotNull() {

    // Arrange
    final String[] array = null;
    final char split = '!';

    // Act
    final String actual = StringUtils.join(array, split);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInputNullNotNullOutputNotNull2() {

    // Arrange
    final String[] array = null;
    final String split = "1a 2b 3c";

    // Act
    final String actual = StringUtils.join(array, split);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void joinInputNullOutputNotNull() {

    // Arrange
    final String[] array = null;

    // Act
    final String actual = StringUtils.join(array);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void lengthInputNotNullOutputPositive() {

    // Arrange
    final CharSequence cs = "?";

    // Act
    final int actual = StringUtils.length(cs);

    // Assert result
    Assert.assertEquals(1, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void lengthInputNullOutputZero() {

    // Arrange
    final CharSequence cs = null;

    // Act
    final int actual = StringUtils.length(cs);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void repeatInputNotNullPositiveOutputNegativeArraySizeException() {

    // Arrange
    final String str = "\u5c0b\u8000";
    final int repeat = 1_107_296_256;

    // Act
    thrown.expect(NegativeArraySizeException.class);
    StringUtils.repeat(str, repeat);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void repeatInputNotNullPositiveOutputNotNull() {

    // Arrange
    final String str = "3";
    final int repeat = 1;

    // Act
    final String actual = StringUtils.repeat(str, repeat);

    // Assert result
    Assert.assertEquals("3", actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void repeatInputNotNullPositiveOutputNotNull2() {

    // Arrange
    final char ch = '\"';
    final int repeat = 1;

    // Act
    final String actual = StringUtils.repeat(ch, repeat);

    // Assert result
    Assert.assertEquals("\"", actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void repeatInputNotNullPositiveOutputNotNull4() {

    // Arrange
    final String str = "\u5c0b\u8000";
    final int repeat = 2;

    // Act
    final String actual = StringUtils.repeat(str, repeat);

    // Assert result
    Assert.assertEquals("????", actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void repeatInputNotNullPositiveOutputNullPointerException() {

    // Arrange
    final String str = "\u5c0b\u8000";
    final int repeat = 2;

    // Act
    thrown.expect(NullPointerException.class);
    StringUtils.repeat(str, repeat);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void repeatInputNotNullZeroOutputNotNull() {

    // Arrange
    final char ch = '!';
    final int repeat = 0;

    // Act
    final String actual = StringUtils.repeat(ch, repeat);

    // Assert result
    Assert.assertEquals("3", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void repeatInputNotNullZeroOutputNotNull2() {

    // Arrange
    final String str = "3";
    final int repeat = 0;

    // Act
    final String actual = StringUtils.repeat(str, repeat);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void repeatInputNullNegativeOutputNull() {

    // Arrange
    final String str = null;
    final int repeat = -2_147_483_647;

    // Act
    final String actual = StringUtils.repeat(str, repeat);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void splitInputNullNotNullOutputNullPointerException() {

    // Arrange
    final String str = null;
    final char ch = '\u0000';

    // Act
    thrown.expect(NullPointerException.class);
    StringUtils.split(str, ch);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void stripEndInputNotNullNotNullOutputNotNull() {

    // Arrange
    final String str = ",";
    final String stripChars = "1a 2b 3c";

    // Act
    final String actual = StringUtils.stripEnd(str, stripChars);

    // Assert result
    Assert.assertEquals(",", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void stripEndInputNotNullNotNullOutputNotNull2() {

    // Arrange
    final String str = "3";
    final String stripChars = "3";

    // Act
    final String actual = StringUtils.stripEnd(str, stripChars);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void stripEndInputNotNullNotNullOutputNotNull3() {

    // Arrange
    final String str = "\uffe2\uffe2\uffe0\uffe1";
    final String stripChars = "";

    // Act
    final String actual = StringUtils.stripEnd(str, stripChars);

    // Assert result
    Assert.assertEquals("\uffe2\uffe2\uffe0\uffe1", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void stripEndInputNotNullNullOutputNotNull() {

    // Arrange
    final String str =
        "\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe1\uffe1\u0000\u0000\u0000";
    final String stripChars = null;

    // Act
    final String actual = StringUtils.stripEnd(str, stripChars);

    // Assert result
    Assert.assertEquals(
        "\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe1\uffe1\u0000\u0000\u0000",
        actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void stripEndInputNotNullNullOutputNotNull2() {

    // Arrange
    final String str =
        "\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe1\uffe1\u0000\u0000\f";
    final String stripChars = null;

    // Act
    final String actual = StringUtils.stripEnd(str, stripChars);

    // Assert result
    Assert.assertEquals(
        "\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe2\uffe1\uffe1\u0000\u0000",
        actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void stripEndInputNullNotNullOutputNull() {

    // Arrange
    final String str = null;
    final String stripChars =
        "\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe1\uffe0\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3\uffe3";

    // Act
    final String actual = StringUtils.stripEnd(str, stripChars);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void toArgumentStringInput0OutputNotNull() {

    // Arrange
    final Object[] args = {};

    // Act
    final String actual = StringUtils.toArgumentString(args);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void toArgumentStringInput2OutputNotNull() {

    // Arrange
    final Object[] args = {null, null};

    // Act
    final String actual = StringUtils.toArgumentString(args);

    // Assert result
    Assert.assertEquals("null,null", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toOSStyleKeyInputNullOutputNullPointerException() {

    // Arrange
    final String key = null;

    // Act
    thrown.expect(NullPointerException.class);
    StringUtils.toOSStyleKey(key);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void toQueryStringInput0OutputNotNull() {

    // Arrange
    final HashMap<String, String> ps = new HashMap<String, String>();

    // Act
    final String actual = StringUtils.toQueryString(ps);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toQueryStringInputNullOutputNotNull() {

    // Arrange
    final Map<String, String> ps = null;

    // Act
    final String actual = StringUtils.toQueryString(ps);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toURLKeyInputNullOutputNullPointerException() {

    // Arrange
    final String key = null;

    // Act
    thrown.expect(NullPointerException.class);
    StringUtils.toURLKey(key);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void trimInputNotNullOutputNotNull() {

    // Arrange
    final String str = "\'";

    // Act
    final String actual = StringUtils.trim(str);

    // Assert result
    Assert.assertEquals("\'", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void trimInputNullOutputNull() {

    // Arrange
    final String str = null;

    // Act
    final String actual = StringUtils.trim(str);

    // Assert result
    Assert.assertNull(actual);
  }
}
