package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.utils.ClassUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class ClassUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void convertPrimitiveInputNullNotNullOutputNotNull() {

    // Arrange
    final Class type = null;
    final String value = "1";

    // Act
    final Object actual = ClassUtils.convertPrimitive(type, value);

    // Assert result
    Assert.assertEquals("1", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void convertPrimitiveInputNullNullOutputNull() {

    // Arrange
    final Class type = null;
    final String value = null;

    // Act
    final Object actual = ClassUtils.convertPrimitive(type, value);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isTypeMatchInputNullNotNullOutputTrue() {

    // Arrange
    final Class type = null;
    final String value = "3";

    // Act
    final boolean actual = ClassUtils.isTypeMatch(type, value);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void resolvePrimitiveClassNameInputNotNullOutputNull() {

    // Arrange
    final String name = "?????????";

    // Act
    final Class actual = ClassUtils.resolvePrimitiveClassName(name);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void resolvePrimitiveClassNameInputNullOutputNull() {

    // Arrange
    final String name = null;

    // Act
    final Class actual = ClassUtils.resolvePrimitiveClassName(name);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void simpleClassNameInputNullOutputNullPointerException() {

    // Arrange
    final Class clazz = null;

    // Act
    thrown.expect(NullPointerException.class);
    ClassUtils.simpleClassName(clazz);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void toShortStringInputNullOutputNotNull() {

    // Arrange
    final Object obj = null;

    // Act
    final String actual = ClassUtils.toShortString(obj);

    // Assert result
    Assert.assertEquals("null", actual);
  }
}
