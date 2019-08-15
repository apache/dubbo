package org.apache.dubbo.common.io;

import org.apache.dubbo.common.io.UnsafeByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;

public class UnsafeByteArrayOutputStreamTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void constructorInputPositiveOutputIllegalArgumentException() {

    // Arrange
    final int size = 32;

    // Act, creating object to test constructor
    thrown.expect(IllegalArgumentException.class);
    final UnsafeByteArrayOutputStream unsafeByteArrayOutputStream =
        new UnsafeByteArrayOutputStream(size);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void sizeOutputZero() {

    // Arrange
    final UnsafeByteArrayOutputStream unsafeByteArrayOutputStream =
        new UnsafeByteArrayOutputStream();

    // Act
    final int actual = unsafeByteArrayOutputStream.size();

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toStringInputNotNullOutputUnsupportedEncodingException()
      throws UnsupportedEncodingException {

    // Arrange
    final UnsafeByteArrayOutputStream unsafeByteArrayOutputStream =
        new UnsafeByteArrayOutputStream();
    final String charset = ",";

    // Act
    thrown.expect(UnsupportedEncodingException.class);
    unsafeByteArrayOutputStream.toString(charset);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void writeInputNullNegativeZeroOutputIndexOutOfBoundsException() {

    // Arrange
    final UnsafeByteArrayOutputStream unsafeByteArrayOutputStream =
        new UnsafeByteArrayOutputStream();
    final byte[] b = null;
    final int off = -2_147_483_648;
    final int len = 0;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    unsafeByteArrayOutputStream.write(b, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void writeInputZeroOutputVoid() {

    // Arrange
    final UnsafeByteArrayOutputStream unsafeByteArrayOutputStream =
        new UnsafeByteArrayOutputStream();
    final int b = 0;

    // Act
    unsafeByteArrayOutputStream.write(b);

    // Assert side effects
    Assert.assertEquals(1, unsafeByteArrayOutputStream.mCount);
  }
}
