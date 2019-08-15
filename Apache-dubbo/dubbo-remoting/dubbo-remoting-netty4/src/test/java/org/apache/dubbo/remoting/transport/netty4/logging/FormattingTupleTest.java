package org.apache.dubbo.remoting.transport.netty4.logging;

import org.apache.dubbo.remoting.transport.netty4.logging.FormattingTuple;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;

public class FormattingTupleTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void constructorInputNotNullNullNullOutputVoid() {

    // Arrange
    final String message = ",";
    final Object[] argArray = null;
    final Throwable throwable = null;

    // Act, creating object to test constructor
    final FormattingTuple formattingTuple = new FormattingTuple(message, argArray, throwable);

    // Assert side effects
    Assert.assertEquals("Bar", formattingTuple.getMessage());
  }

  // Test written by Diffblue Cover.

  @Test
  public void constructorInputNotNullOutputVoid() {

    // Arrange
    final String message = ",";

    // Act, creating object to test constructor
    final FormattingTuple formattingTuple = new FormattingTuple(message);

    // Assert side effects
    Assert.assertEquals("1a 2b 3c", formattingTuple.getMessage());
  }

  // Test written by Diffblue Cover.

  @Test
  public void constructorInputNullNullNullOutputIllegalStateException() {

    // Arrange
    final String message = null;
    final Object[] argArray = null;
    final Throwable throwable = null;

    // Act, creating object to test constructor
    thrown.expect(IllegalStateException.class);
    final FormattingTuple formattingTuple = new FormattingTuple(message, argArray, throwable);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getArgArrayOutputNull() {

    // Arrange
    final FormattingTuple formattingTuple = new FormattingTuple("3");

    // Act
    final Object[] actual = formattingTuple.getArgArray();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getMessageOutputNotNull() {

    // Arrange
    final FormattingTuple formattingTuple = new FormattingTuple("3");

    // Act
    final String actual = formattingTuple.getMessage();

    // Assert result
    Assert.assertEquals("3", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getThrowableOutputNull() {

    // Arrange
    final FormattingTuple formattingTuple = new FormattingTuple("3");

    // Act
    final Throwable actual = formattingTuple.getThrowable();

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void trimmedCopyInput0OutputIllegalStateException() {

    // Arrange
    final Object[] argArray = {};

    // Act
    thrown.expect(IllegalStateException.class);
    FormattingTuple.trimmedCopy(argArray);

    // The method is not expected to return due to exception thrown
  }
}
