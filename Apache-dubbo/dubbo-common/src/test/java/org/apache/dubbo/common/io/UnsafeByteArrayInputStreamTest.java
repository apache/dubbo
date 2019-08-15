package org.apache.dubbo.common.io;

import org.apache.dubbo.common.io.UnsafeByteArrayInputStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;

public class UnsafeByteArrayInputStreamTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void availableOutputZero() {

    // Arrange
    final byte[] myByteArray = {};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);

    // Act
    final int actual = unsafeByteArrayInputStream.available();

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void markSupportedOutputTrue() {

    // Arrange
    final byte[] myByteArray = {};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);

    // Act
    final boolean actual = unsafeByteArrayInputStream.markSupported();

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void positionOutputZero() {

    // Arrange
    final byte[] myByteArray = {};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);

    // Act
    final int actual = unsafeByteArrayInputStream.position();

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void readInput1NegativeZeroOutputIndexOutOfBoundsException() {

    // Arrange
    final byte[] myByteArray = {};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);
    final byte[] b = {(byte)0};
    final int off = -2_147_483_616;
    final int len = 0;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    unsafeByteArrayInputStream.read(b, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void readInput1ZeroZeroOutputNegative() {

    // Arrange
    final byte[] myByteArray = {};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);
    final byte[] b = {(byte)0};
    final int off = 0;
    final int len = 0;

    // Act
    final int actual = unsafeByteArrayInputStream.read(b, off, len);

    // Assert result
    Assert.assertEquals(-1, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void readInput10PositiveZeroOutputZero() {

    // Arrange
    final byte[] myByteArray = {(byte)0};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);
    final byte[] b = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
                      (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
    final int off = 8;
    final int len = 0;

    // Act
    final int actual = unsafeByteArrayInputStream.read(b, off, len);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void readInputNullNegativeZeroOutputNullPointerException() {

    // Arrange
    final byte[] myByteArray = {};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);
    final byte[] b = null;
    final int off = -2_147_483_616;
    final int len = 0;

    // Act
    thrown.expect(NullPointerException.class);
    unsafeByteArrayInputStream.read(b, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void readOutputZero() {

    // Arrange
    final byte[] myByteArray = {(byte)0};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);

    // Act
    final int actual = unsafeByteArrayInputStream.read();

    // Assert side effects
    Assert.assertEquals(1, unsafeByteArrayInputStream.mPosition);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void sizeOutputZero() {

    // Arrange
    final byte[] myByteArray = {};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);

    // Act
    final int actual = unsafeByteArrayInputStream.size();

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void skipInputPositiveOutputPositive() {

    // Arrange
    final byte[] myByteArray = {(byte)0};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);
    final long len = 1L;

    // Act
    final long actual = unsafeByteArrayInputStream.skip(len);

    // Assert side effects
    Assert.assertEquals(1, unsafeByteArrayInputStream.mPosition);

    // Assert result
    Assert.assertEquals(1L, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void skipInputPositiveOutputPositive2() {

    // Arrange
    final byte[] myByteArray = {(byte)0};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);
    final long len = 8_796_093_022_209L;

    // Act
    final long actual = unsafeByteArrayInputStream.skip(len);

    // Assert side effects
    Assert.assertEquals(1, unsafeByteArrayInputStream.mPosition);

    // Assert result
    Assert.assertEquals(1L, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void skipInputZeroOutputZero() {

    // Arrange
    final byte[] myByteArray = {};
    final UnsafeByteArrayInputStream unsafeByteArrayInputStream =
        new UnsafeByteArrayInputStream(myByteArray);
    final long len = 0L;

    // Act
    final long actual = unsafeByteArrayInputStream.skip(len);

    // Assert result
    Assert.assertEquals(0L, actual);
  }
}
