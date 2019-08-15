package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;

public class ArrayUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void isEmptyInput0OutputTrue() {

    // Arrange
    final Object[] array = {};

    // Act
    final boolean actual = ArrayUtils.isEmpty(array);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEmptyInputNullOutputTrue() {

    // Arrange
    final Object[] array = null;

    // Act
    final boolean actual = ArrayUtils.isEmpty(array);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void isNotEmptyInput0OutputFalse() {

    // Arrange
    final Object[] array = {};

    // Act
    final boolean actual = ArrayUtils.isNotEmpty(array);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void isNotEmptyInput1OutputTrue() {

    // Arrange
    final Object[] array = {null};

    // Act
    final boolean actual = ArrayUtils.isNotEmpty(array);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNotEmptyInputNullOutputFalse() {

    // Arrange
    final Object[] array = null;

    // Act
    final boolean actual = ArrayUtils.isNotEmpty(array);

    // Assert result
    Assert.assertFalse(actual);
  }
}
