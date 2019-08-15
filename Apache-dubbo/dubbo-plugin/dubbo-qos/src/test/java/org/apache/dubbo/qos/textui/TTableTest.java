package org.apache.dubbo.qos.textui;

import org.apache.dubbo.qos.textui.TTable.ColumnDefine;
import org.apache.dubbo.qos.textui.TTable;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class TTableTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getRowCountOutputZero() {

    // Arrange
    final ColumnDefine tTableColumnDefine = new ColumnDefine();

    // Act
    final int actual = tTableColumnDefine.getRowCount();

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getWidthOutputZero() {

    // Arrange
    final ColumnDefine tTableColumnDefine = new ColumnDefine();

    // Act
    final int actual = tTableColumnDefine.getWidth();

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void wrapInputNotNullNegativeOutputNotNull() {

    // Arrange
    final String string = "\n";
    final int width = -2_147_483_648;

    // Act
    final String actual = TTable.wrap(string, width);

    // Assert result
    Assert.assertEquals("\n", actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void wrapInputNotNullPositiveOutputNotNull() {

    // Arrange
    final String string = "1";
    final int width = 1;

    // Act
    final String actual = TTable.wrap(string, width);

    // Assert result
    Assert.assertEquals("1", actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void wrapInputNotNullZeroOutputNotNull() {

    // Arrange
    final String string = "1";
    final int width = 0;

    // Act
    final String actual = TTable.wrap(string, width);

    // Assert result
    Assert.assertEquals("\n1", actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void wrapInputNotNullZeroOutputNotNull2() {

    // Arrange
    final String string = "\n";
    final int width = 0;

    // Act
    final String actual = TTable.wrap(string, width);

    // Assert result
    Assert.assertEquals("\n", actual);
  }
}
