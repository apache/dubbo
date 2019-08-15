package org.apache.dubbo.common.compiler.support;

import org.apache.dubbo.common.compiler.support.ClassUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Map;

public class ClassUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputNotNullOutputNotNull() {

    // Arrange
    final char v = '!';

    // Act
    final Character actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertEquals(new Character('!'), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputNullOutputNull() {

    // Arrange
    final Object v = null;

    // Act
    final Object actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputTrueOutputTrue() {

    // Arrange
    final boolean v = true;

    // Act
    final Boolean actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputZeroOutputZero() {

    // Arrange
    final byte v = (byte)0;

    // Act
    final Byte actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertEquals(new Byte((byte)0), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputZeroOutputZero2() {

    // Arrange
    final short v = (short)0;

    // Act
    final Short actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertEquals(new Short((short)0), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputZeroOutputZero3() {

    // Arrange
    final int v = 0;

    // Act
    final Integer actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertEquals(new Integer(0), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputZeroOutputZero4() {

    // Arrange
    final long v = 0L;

    // Act
    final Long actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertEquals(new Long(0L), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputZeroOutputZero5() {

    // Arrange
    final float v = 0.0f;

    // Act
    final Float actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertEquals(0.0f, actual, 0.0f);
  }

  // Test written by Diffblue Cover.
  @Test
  public void boxedInputZeroOutputZero6() {

    // Arrange
    final double v = 0.0;

    // Act
    final Double actual = ClassUtils.boxed(v);

    // Assert result
    Assert.assertEquals(0.0, actual, 0.0);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getBoxedClassInputNullOutputNull() {

    // Arrange
    final Class type = null;

    // Act
    final Class actual = ClassUtils.getBoxedClass(type);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getGenericClassInputNullOutputNullPointerException() {

    // Arrange
    final Class cls = null;

    // Act
    thrown.expect(NullPointerException.class);
    ClassUtils.getGenericClass(cls);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void getGenericClassInputNullZeroOutputNullPointerException() {

    // Arrange
    final Class cls = null;
    final int i = 0;

    // Act
    thrown.expect(NullPointerException.class);
    ClassUtils.getGenericClass(cls, i);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void getInitCodeInputNullOutputNotNull() {

    // Arrange
    final Class type = null;

    // Act
    final String actual = ClassUtils.getInitCode(type);

    // Assert result
    Assert.assertEquals("null", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getSimpleClassNameInputNotNullOutputNotNull() {

    // Arrange
    final String qualifiedName = ",";

    // Act
    final String actual = ClassUtils.getSimpleClassName(qualifiedName);

    // Assert result
    Assert.assertEquals(",", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getSimpleClassNameInputNotNullOutputNotNull2() {

    // Arrange
    final String qualifiedName = ".";

    // Act
    final String actual = ClassUtils.getSimpleClassName(qualifiedName);

    // Assert result
    Assert.assertEquals("", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getSimpleClassNameInputNullOutputNull() {

    // Arrange
    final String qualifiedName = null;

    // Act
    final String actual = ClassUtils.getSimpleClassName(qualifiedName);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void getSizeInput0OutputZero() {

    // Arrange
    final ArrayList object = new ArrayList();

    // Act
    final int actual = ClassUtils.getSize(object);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getSizeInput0OutputZero2() {

    // Arrange
    final HashMap object = new HashMap();

    // Act
    final int actual = ClassUtils.getSize(object);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getSizeInputNullOutputZero() {

    // Arrange
    final Object object = null;

    // Act
    final int actual = ClassUtils.getSize(object);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNotEmptyInputNullOutputFalse() {

    // Arrange
    final Object object = null;

    // Act
    final boolean actual = ClassUtils.isNotEmpty(object);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isNotEmptyInputZeroOutputFalse() {

    // Arrange
    final Object object = 0;

    // Act
    final boolean actual = ClassUtils.isNotEmpty(object);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void searchMethodInputNullNotNullNullOutputNoSuchMethodException()
      throws NoSuchMethodException {

    // Arrange
    final Class currentClass = null;
    final String name = "BAZ";
    final Class[] parameterTypes = null;

    // Act
    thrown.expect(NoSuchMethodException.class);
    ClassUtils.searchMethod(currentClass, name, parameterTypes);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void toMapInput0Output0() {

    // Arrange
    final Map.Entry[] entries = {};

    // Act
    final Map actual = ClassUtils.toMap(entries);

    // Assert result
    final HashMap hashMap = new HashMap();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toMapInputNullOutput0() {

    // Arrange
    final Map.Entry[] entries = null;

    // Act
    final Map actual = ClassUtils.toMap(entries);

    // Assert result
    final HashMap hashMap = new HashMap();
    Assert.assertEquals(hashMap, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputFalseOutputFalse() {

    // Arrange
    final Boolean v = false;

    // Act
    final boolean actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNotNullOutputNotNull() {

    // Arrange
    final Character v = '!';

    // Act
    final char actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals('!', actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputFalse() {

    // Arrange
    final Boolean v = null;

    // Act
    final boolean actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputNotNull() {

    // Arrange
    final Character v = null;

    // Act
    final char actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals('\u0000', actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputNull() {

    // Arrange
    final Object v = null;

    // Act
    final Object actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputZero() {

    // Arrange
    final Short v = null;

    // Act
    final short actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals((short)0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputZero2() {

    // Arrange
    final Byte v = null;

    // Act
    final byte actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals((byte)0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputZero3() {

    // Arrange
    final Integer v = null;

    // Act
    final int actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputZero4() {

    // Arrange
    final Float v = null;

    // Act
    final float actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals(0.0f, actual, 0.0f);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputZero5() {

    // Arrange
    final Long v = null;

    // Act
    final long actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals(0L, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputNullOutputZero6() {

    // Arrange
    final Double v = null;

    // Act
    final double actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals(0.0, actual, 0.0);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputZeroOutputZero() {

    // Arrange
    final Short v = (short)0;

    // Act
    final short actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals((short)0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputZeroOutputZero2() {

    // Arrange
    final Byte v = (byte)0;

    // Act
    final byte actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals((byte)0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputZeroOutputZero3() {

    // Arrange
    final Integer v = 0;

    // Act
    final int actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputZeroOutputZero4() {

    // Arrange
    final Float v = 0.0f;

    // Act
    final float actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals(0.0f, actual, 0.0f);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputZeroOutputZero5() {

    // Arrange
    final Long v = 0L;

    // Act
    final long actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals(0L, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void unboxedInputZeroOutputZero6() {

    // Arrange
    final Double v = 0.0;

    // Act
    final double actual = ClassUtils.unboxed(v);

    // Assert result
    Assert.assertEquals(0.0, actual, 0.0);
  }
}
