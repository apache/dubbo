package org.apache.dubbo.remoting.transport.netty4.logging;

import org.apache.dubbo.remoting.transport.netty4.logging.MessageFormatter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;

public class MessageFormatterTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void getThrowableCandidateInput1OutputNull() {

    // Arrange
    final Object[] argArray = {null};

    // Act
    final Throwable actual = MessageFormatter.getThrowableCandidate(argArray);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getThrowableCandidateInputNullOutputNull() {

    // Arrange
    final Object[] argArray = null;

    // Act
    final Throwable actual = MessageFormatter.getThrowableCandidate(argArray);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isDoubleEscapedInputNotNullPositiveOutputFalse() {

    // Arrange
    final String messagePattern = "a\'b\'c";
    final int delimeterStartIndex = 4;

    // Act
    final boolean actual = MessageFormatter.isDoubleEscaped(messagePattern, delimeterStartIndex);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isDoubleEscapedInputNotNullPositiveOutputStringIndexOutOfBoundsException() {

    // Arrange
    final String messagePattern = "foo";
    final int delimeterStartIndex = 536_870_914;

    // Act
    thrown.expect(StringIndexOutOfBoundsException.class);
    MessageFormatter.isDoubleEscaped(messagePattern, delimeterStartIndex);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void isDoubleEscapedInputNotNullPositiveOutputTrue() {

    // Arrange
    final String messagePattern = "\\\\??";
    final int delimeterStartIndex = 3;

    // Act
    final boolean actual = MessageFormatter.isDoubleEscaped(messagePattern, delimeterStartIndex);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEscapedDelimeterInputNotNullPositiveOutputFalse() {

    // Arrange
    final String messagePattern = "a\'b\'c";
    final int delimeterStartIndex = 4;

    // Act
    final boolean actual = MessageFormatter.isEscapedDelimeter(messagePattern, delimeterStartIndex);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEscapedDelimeterInputNotNullPositiveOutputStringIndexOutOfBoundsException() {

    // Arrange
    final String messagePattern = "foo";
    final int delimeterStartIndex = 545_259_521;

    // Act
    thrown.expect(StringIndexOutOfBoundsException.class);
    MessageFormatter.isEscapedDelimeter(messagePattern, delimeterStartIndex);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEscapedDelimeterInputNotNullPositiveOutputTrue() {

    // Arrange
    final String messagePattern = "\\";
    final int delimeterStartIndex = 1;

    // Act
    final boolean actual = MessageFormatter.isEscapedDelimeter(messagePattern, delimeterStartIndex);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isEscapedDelimeterInputNotNullZeroOutputFalse() {

    // Arrange
    final String messagePattern = "\'";
    final int delimeterStartIndex = 0;

    // Act
    final boolean actual = MessageFormatter.isEscapedDelimeter(messagePattern, delimeterStartIndex);

    // Assert result
    Assert.assertFalse(actual);
  }
}
