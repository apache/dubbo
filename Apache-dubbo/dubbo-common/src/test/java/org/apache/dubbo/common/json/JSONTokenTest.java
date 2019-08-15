package org.apache.dubbo.common.json;

import org.apache.dubbo.common.json.JSONToken;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class JSONTokenTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull() {

    // Arrange
    final int t = 2;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("{", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull2() {

    // Arrange
    final int t = 1;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("IDENT", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull3() {

    // Arrange
    final int t = 20;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("STRING VALUE", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull4() {

    // Arrange
    final int t = 10;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("ANY", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull5() {

    // Arrange
    final int t = 3;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("[", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull6() {

    // Arrange
    final int t = 19;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("FLOAT VALUE", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull7() {

    // Arrange
    final int t = 18;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("INT VALUE", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull8() {

    // Arrange
    final int t = 17;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("BOOL VALUE", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull9() {

    // Arrange
    final int t = 16;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("NULL", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull10() {

    // Arrange
    final int t = 7;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals(":", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull11() {

    // Arrange
    final int t = 4;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("}", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull12() {

    // Arrange
    final int t = 5;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals("]", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void token2stringInputPositiveOutputNotNull13() {

    // Arrange
    final int t = 6;

    // Act
    final String actual = JSONToken.token2string(t);

    // Assert result
    Assert.assertEquals(",", actual);
  }
}
