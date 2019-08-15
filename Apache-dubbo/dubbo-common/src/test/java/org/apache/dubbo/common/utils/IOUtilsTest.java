package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.utils.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;

public class IOUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void appendLinesInputNullNullOutputIOException() throws IOException {

    // Arrange
    final File file = null;
    final String[] lines = null;

    // Act
    thrown.expect(IOException.class);
    IOUtils.appendLines(file, lines);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void readInputNullOutputNullPointerException() throws IOException {

    // Arrange
    final Reader reader = null;

    // Act
    thrown.expect(NullPointerException.class);
    IOUtils.read(reader);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.

  @Test
  public void readLinesInputNullOutput0() throws IOException {

    // Arrange
    final File file = null;

    // Act
    final String[] actual = IOUtils.readLines(file);

    // Assert result
    Assert.assertArrayEquals(new String[] {}, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void writeInputNullNotNullOutputNullPointerException() throws IOException {

    // Arrange
    final Writer writer = null;
    final String string = "\ufffc";

    // Act
    thrown.expect(NullPointerException.class);
    IOUtils.write(writer, string);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void writeInputNullNotNullOutputZero() throws IOException {

    // Arrange
    final Writer writer = null;
    final String string = "";

    // Act
    final long actual = IOUtils.write(writer, string);

    // Assert result
    Assert.assertEquals(0L, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void writeInputNullNullOutputNullPointerException() throws IOException {

    // Arrange
    final Writer writer = null;
    final String string = null;

    // Act
    thrown.expect(NullPointerException.class);
    IOUtils.write(writer, string);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void writeLinesInputNullNullOutputIOException() throws IOException {

    // Arrange
    final File file = null;
    final String[] lines = null;

    // Act
    thrown.expect(IOException.class);
    IOUtils.writeLines(file, lines);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void writeLinesInputNullNullOutputNullPointerException() throws IOException {

    // Arrange
    final OutputStream os = null;
    final String[] lines = null;

    // Act
    thrown.expect(NullPointerException.class);
    IOUtils.writeLines(os, lines);

    // The method is not expected to return due to exception thrown
  }
}
