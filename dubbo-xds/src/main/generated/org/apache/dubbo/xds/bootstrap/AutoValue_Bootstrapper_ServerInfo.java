package org.apache.dubbo.xds.bootstrap;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_Bootstrapper_ServerInfo extends Bootstrapper.ServerInfo {

  private final String target;

  private final Object implSpecificConfig;

  private final boolean ignoreResourceDeletion;

  AutoValue_Bootstrapper_ServerInfo(
      String target,
      Object implSpecificConfig,
      boolean ignoreResourceDeletion) {
    if (target == null) {
      throw new NullPointerException("Null target");
    }
    this.target = target;
    if (implSpecificConfig == null) {
      throw new NullPointerException("Null implSpecificConfig");
    }
    this.implSpecificConfig = implSpecificConfig;
    this.ignoreResourceDeletion = ignoreResourceDeletion;
  }

  @Override
  public String target() {
    return target;
  }

  @Override
  public Object implSpecificConfig() {
    return implSpecificConfig;
  }

  @Override
  public boolean ignoreResourceDeletion() {
    return ignoreResourceDeletion;
  }

  @Override
  public String toString() {
    return "ServerInfo{"
        + "target=" + target + ", "
        + "implSpecificConfig=" + implSpecificConfig + ", "
        + "ignoreResourceDeletion=" + ignoreResourceDeletion
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Bootstrapper.ServerInfo) {
      Bootstrapper.ServerInfo that = (Bootstrapper.ServerInfo) o;
      return this.target.equals(that.target())
          && this.implSpecificConfig.equals(that.implSpecificConfig())
          && this.ignoreResourceDeletion == that.ignoreResourceDeletion();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= target.hashCode();
    h$ *= 1000003;
    h$ ^= implSpecificConfig.hashCode();
    h$ *= 1000003;
    h$ ^= ignoreResourceDeletion ? 1231 : 1237;
    return h$;
  }

}
