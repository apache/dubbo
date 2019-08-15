package org.apache.dubbo.common.io;

import org.apache.dubbo.common.io.Bytes;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;

public class BytesTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullNegativeNegativeNotNullOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = ",";
    final int off = -838_860_801;
    final int len = -1_308_622_845;
    final String code = "BAZ";

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.base642bytes(str, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullNegativePositiveNullOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = "3";
    final int off = -1;
    final int len = 65_536;
    final char[] code = null;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.base642bytes(str, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullPositiveNegativeNotNullOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = ",";
    final int off = 1_308_622_847;
    final int len = -1_308_622_845;
    final String code = "BAZ";

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.base642bytes(str, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullPositiveNegativeNullOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = "3";
    final int off = 1_073_741_822;
    final int len = -1_073_741_819;
    final char[] code = null;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.base642bytes(str, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullPositivePositive8OutputIllegalArgumentException() {

    // Arrange
    final String str = "2";
    final int off = 2_147_483_647;
    final int len = 98_304;
    final char[] code = {'\u8000', '\u0000', '\u0000', '\u0000',
                         '\u0000', '\u0000', '\u0000', '\u0000'};

    // Act
    thrown.expect(IllegalArgumentException.class);
    Bytes.base642bytes(str, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullPositivePositiveNotNullOutputIllegalArgumentException() {

    // Arrange
    final String str = "2";
    final int off = 2_147_483_643;
    final int len = 1_115_684_867;
    final String code = "Bar";

    // Act
    thrown.expect(IllegalArgumentException.class);
    Bytes.base642bytes(str, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullPositivePositiveNotNullOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = ",";
    final int off = 1_308_622_847;
    final int len = 3;
    final String code = "BAZ";

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.base642bytes(str, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullPositivePositiveNullOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = "3";
    final int off = 1_073_741_822;
    final int len = 5;
    final char[] code = null;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.base642bytes(str, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullPositiveZeroNotNullOutput0() {

    // Arrange
    final String str = ",";
    final int off = 234_881_023;
    final int len = 0;
    final String code = "BAZ";

    // Act
    final byte[] actual = Bytes.base642bytes(str, off, len, code);

    // Assert result
    Assert.assertArrayEquals(new byte[] {}, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void base642bytesInputNotNullPositiveZeroNullOutput0() {

    // Arrange
    final String str = "3";
    final int off = 32_768;
    final int len = 0;
    final char[] code = null;

    // Act
    final byte[] actual = Bytes.base642bytes(str, off, len, code);

    // Assert result
    Assert.assertArrayEquals(new byte[] {}, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2base64Input0PositiveNegativeNullOutputIndexOutOfBoundsException() {

    // Arrange
    final byte[] bs = {};
    final int off = 1;
    final int len = -1_612_709_888;
    final char[] code = null;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.bytes2base64(bs, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2base64Input8ZeroPositive0OutputIllegalArgumentException() {

    // Arrange
    final byte[] bs = {(byte)56, (byte)40, (byte)0,  (byte)1,
                       (byte)40, (byte)33, (byte)57, (byte)120};
    final int off = 0;
    final int len = 6;
    final char[] code = {};

    // Act
    thrown.expect(IllegalArgumentException.class);
    Bytes.bytes2base64(bs, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2base64Input9PositivePositiveNullOutputIndexOutOfBoundsException() {

    // Arrange
    final byte[] bs = {(byte)57, (byte)57, (byte)57, (byte)57, (byte)57,
                       (byte)57, (byte)57, (byte)56, (byte)57};
    final int off = 3;
    final int len = 536_870_912;
    final char[] code = null;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.bytes2base64(bs, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2base64InputNullNegativeNegativeNullOutputIndexOutOfBoundsException() {

    // Arrange
    final byte[] bs = null;
    final int off = -2_147_483_631;
    final int len = -1_612_709_888;
    final char[] code = null;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.bytes2base64(bs, off, len, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2base64InputNullNegativePositiveNotNullOutputIllegalArgumentException() {

    // Arrange
    final byte[] b = null;
    final int offset = -1_811_414_712;
    final int length = 581_537_213;
    final String code = ",";

    // Act
    thrown.expect(IllegalArgumentException.class);
    Bytes.bytes2base64(b, offset, length, code);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2doubleInput10ZeroOutputZero() {

    // Arrange
    final byte[] b = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
                      (byte)0, (byte)0, (byte)0, (byte)1, (byte)1};
    final int off = 0;

    // Act
    final double actual = Bytes.bytes2double(b, off);

    // Assert result
    Assert.assertEquals(0.0, actual, 0.0);
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2floatInput6ZeroOutputZero() {

    // Arrange
    final byte[] b = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)1, (byte)1};
    final int off = 0;

    // Act
    final float actual = Bytes.bytes2float(b, off);

    // Assert result
    Assert.assertEquals(0.0f, actual, 0.0f);
  }

  // Test written by Diffblue Cover.

  @Test
  public void bytes2hexInput0ZeroZeroOutputNotNull() {

    // Arrange
    final byte[] bs = {};
    final int off = 0;
    final int len = 0;

    // Act
    final String actual = Bytes.bytes2hex(bs, off, len);

    // Assert result
    Assert.assertEquals("1a 2b 3c", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2hexInput1PositiveZeroOutputIndexOutOfBoundsException() {

    // Arrange
    final byte[] bs = {(byte)-31};
    final int off = 117_439_744;
    final int len = 0;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.bytes2hex(bs, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2hexInputNullNegativeNegativeOutputIndexOutOfBoundsException() {

    // Arrange
    final byte[] bs = null;
    final int off = -16_777_984;
    final int len = -67_108_863;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.bytes2hex(bs, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2hexInputNullPositiveNegativeOutputIndexOutOfBoundsException() {

    // Arrange
    final byte[] bs = null;
    final int off = 1;
    final int len = -67_108_863;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.bytes2hex(bs, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2intInput6ZeroOutputZero() {

    // Arrange
    final byte[] b = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)1, (byte)1};
    final int off = 0;

    // Act
    final int actual = Bytes.bytes2int(b, off);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2longInput10ZeroOutputZero() {

    // Arrange
    final byte[] b = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
                      (byte)0, (byte)0, (byte)0, (byte)1, (byte)1};
    final int off = 0;

    // Act
    final long actual = Bytes.bytes2long(b, off);

    // Assert result
    Assert.assertEquals(0L, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2shortInput2ZeroOutputZero() {

    // Arrange
    final byte[] b = {(byte)0, (byte)0};
    final int off = 0;

    // Act
    final short actual = Bytes.bytes2short(b, off);

    // Assert result
    Assert.assertEquals((short)0, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void hex2bytesInputNotNullNegativeNegativeOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = "a/b/c";
    final int off = -536_870_914;
    final int len = -2_145_386_494;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.hex2bytes(str, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void hex2bytesInputNotNullPositiveNegativeOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = ",";
    final int off = 2_147_483_646;
    final int len = -2_145_386_494;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.hex2bytes(str, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void hex2bytesInputNotNullPositivePositiveOutputIllegalArgumentException() {

    // Arrange
    final String str = "foo";
    final int off = 1_048_576;
    final int len = 5;

    // Act
    thrown.expect(IllegalArgumentException.class);
    Bytes.hex2bytes(str, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void hex2bytesInputNotNullPositivePositiveOutputIndexOutOfBoundsException() {

    // Arrange
    final String str = "foo";
    final int off = 1_048_576;
    final int len = 4;

    // Act
    thrown.expect(IndexOutOfBoundsException.class);
    Bytes.hex2bytes(str, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void hex2bytesInputNotNullPositivePositiveOutputStringIndexOutOfBoundsException() {

    // Arrange
    final String str = "foo";
    final int off = 2_147_483_644;
    final int len = 4;

    // Act
    thrown.expect(StringIndexOutOfBoundsException.class);
    Bytes.hex2bytes(str, off, len);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void hex2bytesInputNotNullPositiveZeroOutput0() {

    // Arrange
    final String str = "a/b/c";
    final int off = 5;
    final int len = 0;

    // Act
    final byte[] actual = Bytes.hex2bytes(str, off, len);

    // Assert result
    Assert.assertArrayEquals(new byte[] {}, actual);
  }
}
