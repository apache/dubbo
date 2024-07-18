package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.Matchers.HeaderMatcher;

import com.google.common.collect.ImmutableList;

final class AutoValue_VirtualHost_Route_RouteMatch extends VirtualHost.Route.RouteMatch {

  private final VirtualHost.Route.RouteMatch.PathMatcher pathMatcher;

  private final ImmutableList<HeaderMatcher> headerMatchers;

  @Nullable
  private final Matchers.FractionMatcher fractionMatcher;

  AutoValue_VirtualHost_Route_RouteMatch(
      VirtualHost.Route.RouteMatch.PathMatcher pathMatcher,
      ImmutableList<Matchers.HeaderMatcher> headerMatchers,
      @Nullable Matchers.FractionMatcher fractionMatcher) {
    if (pathMatcher == null) {
      throw new NullPointerException("Null pathMatcher");
    }
    this.pathMatcher = pathMatcher;
    if (headerMatchers == null) {
      throw new NullPointerException("Null headerMatchers");
    }
    this.headerMatchers = headerMatchers;
    this.fractionMatcher = fractionMatcher;
  }

  @Override
  VirtualHost.Route.RouteMatch.PathMatcher pathMatcher() {
    return pathMatcher;
  }

  @Override
  ImmutableList<Matchers.HeaderMatcher> headerMatchers() {
    return headerMatchers;
  }

  @Nullable
  @Override
  Matchers.FractionMatcher fractionMatcher() {
    return fractionMatcher;
  }

  @Override
  public String toString() {
    return "RouteMatch{"
        + "pathMatcher=" + pathMatcher + ", "
        + "headerMatchers=" + headerMatchers + ", "
        + "fractionMatcher=" + fractionMatcher
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof VirtualHost.Route.RouteMatch) {
      VirtualHost.Route.RouteMatch that = (VirtualHost.Route.RouteMatch) o;
      return this.pathMatcher.equals(that.pathMatcher())
          && this.headerMatchers.equals(that.headerMatchers())
          && (this.fractionMatcher == null ? that.fractionMatcher() == null : this.fractionMatcher.equals(that.fractionMatcher()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= pathMatcher.hashCode();
    h$ *= 1000003;
    h$ ^= headerMatchers.hashCode();
    h$ *= 1000003;
    h$ ^= (fractionMatcher == null) ? 0 : fractionMatcher.hashCode();
    return h$;
  }

}
