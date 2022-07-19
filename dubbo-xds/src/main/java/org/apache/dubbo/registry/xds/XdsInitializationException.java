package org.apache.dubbo.registry.xds;

public final class XdsInitializationException extends Exception {

  public XdsInitializationException(String message) {
    super(message);
  }

  public XdsInitializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
