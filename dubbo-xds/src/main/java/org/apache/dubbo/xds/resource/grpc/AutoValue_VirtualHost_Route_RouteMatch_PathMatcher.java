package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import com.google.re2j.Pattern;

final class AutoValue_VirtualHost_Route_RouteMatch_PathMatcher extends VirtualHost.Route.RouteMatch.PathMatcher {

  @Nullable
  private final String path;

  @Nullable
  private final String prefix;

  @Nullable
  private final Pattern regEx;

  private final boolean caseSensitive;

  AutoValue_VirtualHost_Route_RouteMatch_PathMatcher(
      @Nullable String path,
      @Nullable String prefix,
      @Nullable Pattern regEx,
      boolean caseSensitive) {
    this.path = path;
    this.prefix = prefix;
    this.regEx = regEx;
    this.caseSensitive = caseSensitive;
  }

  @Nullable
  @Override
  String path() {
    return path;
  }

  @Nullable
  @Override
  String prefix() {
    return prefix;
  }

  @Nullable
  @Override
  Pattern regEx() {
    return regEx;
  }

  @Override
  boolean caseSensitive() {
    return caseSensitive;
  }

  @Override
  public String toString() {
    return "PathMatcher{"
        + "path=" + path + ", "
        + "prefix=" + prefix + ", "
        + "regEx=" + regEx + ", "
        + "caseSensitive=" + caseSensitive
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof VirtualHost.Route.RouteMatch.PathMatcher) {
      VirtualHost.Route.RouteMatch.PathMatcher that = (VirtualHost.Route.RouteMatch.PathMatcher) o;
      return (this.path == null ? that.path() == null : this.path.equals(that.path()))
          && (this.prefix == null ? that.prefix() == null : this.prefix.equals(that.prefix()))
          && (this.regEx == null ? that.regEx() == null : this.regEx.equals(that.regEx()))
          && this.caseSensitive == that.caseSensitive();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (path == null) ? 0 : path.hashCode();
    h$ *= 1000003;
    h$ ^= (prefix == null) ? 0 : prefix.hashCode();
    h$ *= 1000003;
    h$ ^= (regEx == null) ? 0 : regEx.hashCode();
    h$ *= 1000003;
    h$ ^= caseSensitive ? 1231 : 1237;
    return h$;
  }

}
