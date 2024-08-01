package org.apache.dubbo.xds.resource.grpc;

import io.grpc.ChannelCredentials;

final class AutoValue_Bootstrapper_ServerInfo extends Bootstrapper.ServerInfo {

  private final String target;

  private final ChannelCredentials channelCredentials;

  private final boolean ignoreResourceDeletion;

  AutoValue_Bootstrapper_ServerInfo(
      String target,
      ChannelCredentials channelCredentials,
      boolean ignoreResourceDeletion) {
    if (target == null) {
      throw new NullPointerException("Null target");
    }
    this.target = target;
    if (channelCredentials == null) {
      throw new NullPointerException("Null channelCredentials");
    }
    this.channelCredentials = channelCredentials;
    this.ignoreResourceDeletion = ignoreResourceDeletion;
  }

  @Override
  String target() {
    return target;
  }

  @Override
  ChannelCredentials channelCredentials() {
    return channelCredentials;
  }

  @Override
  boolean ignoreResourceDeletion() {
    return ignoreResourceDeletion;
  }

  @Override
  public String toString() {
    return "ServerInfo{"
        + "target=" + target + ", "
        + "channelCredentials=" + channelCredentials + ", "
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
          && this.channelCredentials.equals(that.channelCredentials())
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
    h$ ^= channelCredentials.hashCode();
    h$ *= 1000003;
    h$ ^= ignoreResourceDeletion ? 1231 : 1237;
    return h$;
  }

}
