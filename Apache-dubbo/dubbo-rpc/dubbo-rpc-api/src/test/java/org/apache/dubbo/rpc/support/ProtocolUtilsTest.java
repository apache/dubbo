package org.apache.dubbo.rpc.support;

import org.apache.dubbo.rpc.support.ProtocolUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class ProtocolUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void isBeanGenericSerializationInputNotNullOutputFalse() {

    // Arrange
    final String generic = "3";

    // Act
    final boolean actual = ProtocolUtils.isBeanGenericSerialization(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isBeanGenericSerializationInputNotNullOutputFalse2() {

    // Arrange
    final String generic = "nATIVEJAVa";

    // Act
    final boolean actual = ProtocolUtils.isBeanGenericSerialization(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isBeanGenericSerializationInputNotNullOutputTrue() {

    // Arrange
    final String generic = "bean";

    // Act
    final boolean actual = ProtocolUtils.isBeanGenericSerialization(generic);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isDefaultGenericSerializationInputNotNullOutputFalse() {

    // Arrange
    final String generic = "foo";

    // Act
    final boolean actual = ProtocolUtils.isDefaultGenericSerialization(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isDefaultGenericSerializationInputNotNullOutputFalse2() {

    // Arrange
    final String generic = "PROTOBUF-JSON";

    // Act
    final boolean actual = ProtocolUtils.isDefaultGenericSerialization(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isDefaultGenericSerializationInputNotNullOutputTrue() {

    // Arrange
    final String generic = "TRUe";

    // Act
    final boolean actual = ProtocolUtils.isDefaultGenericSerialization(generic);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isGenericInputNotNullOutputFalse() {

    // Arrange
    final String generic = "foo";

    // Act
    final boolean actual = ProtocolUtils.isGeneric(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isGenericInputNotNullOutputFalse2() {

    // Arrange
    final String generic = "";

    // Act
    final boolean actual = ProtocolUtils.isGeneric(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isGenericInputNotNullOutputTrue() {

    // Arrange
    final String generic = "PROToBuF-jSON";

    // Act
    final boolean actual = ProtocolUtils.isGeneric(generic);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isGenericInputNotNullOutputTrue2() {

    // Arrange
    final String generic = "TRUE";

    // Act
    final boolean actual = ProtocolUtils.isGeneric(generic);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isJavaGenericSerializationInputNotNullOutputFalse() {

    // Arrange
    final String generic = "/";

    // Act
    final boolean actual = ProtocolUtils.isJavaGenericSerialization(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isJavaGenericSerializationInputNotNullOutputFalse2() {

    // Arrange
    final String generic = "proTobuf-json";

    // Act
    final boolean actual = ProtocolUtils.isJavaGenericSerialization(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isJavaGenericSerializationInputNotNullOutputTrue() {

    // Arrange
    final String generic = "NATivejAVA";

    // Act
    final boolean actual = ProtocolUtils.isJavaGenericSerialization(generic);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isProtobufGenericSerializationInputNotNullOutputFalse() {

    // Arrange
    final String generic = "/";

    // Act
    final boolean actual = ProtocolUtils.isProtobufGenericSerialization(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isProtobufGenericSerializationInputNotNullOutputFalse2() {

    // Arrange
    final String generic = "NATivejava";

    // Act
    final boolean actual = ProtocolUtils.isProtobufGenericSerialization(generic);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void isProtobufGenericSerializationInputNotNullOutputTrue() {

    // Arrange
    final String generic = "protobuf-json";

    // Act
    final boolean actual = ProtocolUtils.isProtobufGenericSerialization(generic);

    // Assert result
    Assert.assertTrue(actual);
  }
}
