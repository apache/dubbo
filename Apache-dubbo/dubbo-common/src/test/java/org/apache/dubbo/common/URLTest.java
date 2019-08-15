package org.apache.dubbo.common;

import static org.mockito.AdditionalMatchers.or;

import org.apache.dubbo.common.URL;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Method;
import java.util.Map;

public class URLTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void appendDefaultPortInputNotNullNegativeOutputNotNull() {

    // Arrange
    final String address = "1234";
    final int defaultPort = -536_870_941;

    // Act
    final String actual = URL.appendDefaultPort(address, defaultPort);

    // Assert result
    Assert.assertEquals("1234", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void appendDefaultPortInputNotNullPositiveOutputNotNull() {

    // Arrange
    final String address = "1234";
    final int defaultPort = 1_073_741_824;

    // Act
    final String actual = URL.appendDefaultPort(address, defaultPort);

    // Assert result
    Assert.assertEquals("1234:1073741824", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void appendDefaultPortInputNotNullPositiveOutputNotNull2() {

    // Arrange
    final String address = ";;;;;;;;;;;;;;;:7";
    final int defaultPort = 3;

    // Act
    final String actual = URL.appendDefaultPort(address, defaultPort);

    // Assert result
    Assert.assertEquals(";;;;;;;;;;;;;;;:7", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void appendDefaultPortInputNotNullPositiveOutputNotNull3() {

    // Arrange
    final String address = ";;;;;;;;;;;;;;;:+0";
    final int defaultPort = 9;

    // Act
    final String actual = URL.appendDefaultPort(address, defaultPort);

    // Assert result
    Assert.assertEquals(";;;;;;;;;;;;;;;:9", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void buildKeyInputNotNullNotNullNotNullOutputNotNull() {

    // Arrange
    final String path = "3";
    final String group = "3";
    final String version = "3";

    // Act
    final String actual = URL.buildKey(path, group, version);

    // Assert result
    Assert.assertEquals("3/3:3", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void buildKeyInputNotNullNullNullOutputNotNull() {

    // Arrange
    final String path = "?";
    final String group = null;
    final String version = null;

    // Act
    final String actual = URL.buildKey(path, group, version);

    // Assert result
    Assert.assertEquals("?", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void equalsInputNotNullOutputFalse() {

    // Arrange
    final URL uRL = new URL();
    final URL obj = new URL();

    // Act
    final boolean actual = uRL.equals(obj);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void equalsInputNullOutputFalse() {

    // Arrange
    final URL uRL = new URL();
    final Object obj = null;

    // Act
    final boolean actual = uRL.equals(obj);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getAbsolutePathOutputNull() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final String actual = uRL.getAbsolutePath();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getAddressOutputNull() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final String actual = uRL.getAddress();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getHostOutputNull() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final String actual = uRL.getHost();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void getMethodParameterAndDecodedInputNotNullNotNullNotNullOutputNullPointerException() {

    // Arrange
    final URL uRL = new URL();
    final String method = "";
    final String key = "";
    final String defaultValue = "?";

    // Act
    thrown.expect(NullPointerException.class);
    uRL.getMethodParameterAndDecoded(method, key, defaultValue);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void getMethodPositiveIntParameterInputNotNullNotNullZeroOutputIllegalArgumentException() {

    // Arrange
    final URL uRL = new URL();
    final String method = "1a 2b 3c";
    final String key = "A1B2C3";
    final int defaultValue = 0;

    // Act
    thrown.expect(IllegalArgumentException.class);
    uRL.getMethodPositiveIntParameter(method, key, defaultValue);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void getMethodPositiveParameterInputNotNullNotNullZeroOutputIllegalArgumentException() {

    // Arrange
    final URL uRL = new URL();
    final String method = "2";
    final String key = "3";
    final int defaultValue = 0;

    // Act
    thrown.expect(IllegalArgumentException.class);
    uRL.getMethodPositiveParameter(method, key, defaultValue);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void getMethodPositiveParameterInputNotNullNotNullZeroOutputIllegalArgumentException2() {

    // Arrange
    final URL uRL = new URL();
    final String method = "2";
    final String key = "3";
    final double defaultValue = 0.0;

    // Act
    thrown.expect(IllegalArgumentException.class);
    uRL.getMethodPositiveParameter(method, key, defaultValue);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void getMethodPositiveParameterInputNotNullNotNullZeroOutputIllegalArgumentException3() {

    // Arrange
    final URL uRL = new URL();
    final String method = "3";
    final String key = "3";
    final float defaultValue = 0.0f;

    // Act
    thrown.expect(IllegalArgumentException.class);
    uRL.getMethodPositiveParameter(method, key, defaultValue);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void getMethodPositiveParameterInputNotNullNotNullZeroOutputIllegalArgumentException4() {

    // Arrange
    final URL uRL = new URL();
    final String method = "3";
    final String key = "2";
    final short defaultValue = (short)0;

    // Act
    thrown.expect(IllegalArgumentException.class);
    uRL.getMethodPositiveParameter(method, key, defaultValue);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void getMethodPositiveParameterInputNotNullNotNullZeroOutputIllegalArgumentException5() {

    // Arrange
    final URL uRL = new URL();
    final String method = "3";
    final String key = "BAZ";
    final byte defaultValue = (byte)0;

    // Act
    thrown.expect(IllegalArgumentException.class);
    uRL.getMethodPositiveParameter(method, key, defaultValue);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void getParameterAndDecodedInputNullOutputNullPointerException() {

    // Arrange
    final URL uRL = new URL();
    final String key = null;

    // Act
    thrown.expect(NullPointerException.class);
    uRL.getParameterAndDecoded(key);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void getParametersOutputNull() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final Map<String, String> actual = uRL.getParameters();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getPasswordOutputNull() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final String actual = uRL.getPassword();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getPathOutputNull() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final String actual = uRL.getPath();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getPortInputZeroOutputZero() {

    // Arrange
    final URL uRL = new URL();
    final int defaultPort = 0;

    // Act
    final int actual = uRL.getPort(defaultPort);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getPortOutputZero() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final int actual = uRL.getPort();

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void getPositiveParameterInputNotNullNegativeOutputIllegalArgumentException() {

    // Arrange
    final URL uRL = new URL();
    final String key = "\ue000";
    final short defaultValue = (short)-32_255;

    // Act
    thrown.expect(IllegalArgumentException.class);
    uRL.getPositiveParameter(key, defaultValue);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void getProtocolOutputNull() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final String actual = uRL.getProtocol();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getUsernameOutputNull() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final String actual = uRL.getUsername();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void hashCodeOutputPositive() {

    // Arrange
    final URL uRL = new URL();

    // Act
    final int actual = uRL.hashCode();

    // Assert result
    Assert.assertEquals(1_742_810_335, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void hasParameterInputNullOutputNullPointerException() {

    // Arrange
    final URL uRL = new URL();
    final String key = null;

    // Act
    thrown.expect(NullPointerException.class);
    uRL.hasParameter(key);

    // The method is not expected to return due to exception thrown
  }
}
