package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import com.google.re2j.Pattern;

final class AutoValue_VirtualHost_Route_RouteAction_HashPolicy extends VirtualHost.Route.RouteAction.HashPolicy {

  private final VirtualHost.Route.RouteAction.HashPolicy.Type type;

  private final boolean isTerminal;

  @Nullable
  private final String headerName;

  @Nullable
  private final Pattern regEx;

  @Nullable
  private final String regExSubstitution;

  AutoValue_VirtualHost_Route_RouteAction_HashPolicy(
      VirtualHost.Route.RouteAction.HashPolicy.Type type,
      boolean isTerminal,
      @Nullable String headerName,
      @Nullable Pattern regEx,
      @Nullable String regExSubstitution) {
    if (type == null) {
      throw new NullPointerException("Null type");
    }
    this.type = type;
    this.isTerminal = isTerminal;
    this.headerName = headerName;
    this.regEx = regEx;
    this.regExSubstitution = regExSubstitution;
  }

  @Override
  VirtualHost.Route.RouteAction.HashPolicy.Type type() {
    return type;
  }

  @Override
  boolean isTerminal() {
    return isTerminal;
  }

  @Nullable
  @Override
  String headerName() {
    return headerName;
  }

  @Nullable
  @Override
  Pattern regEx() {
    return regEx;
  }

  @Nullable
  @Override
  String regExSubstitution() {
    return regExSubstitution;
  }

  @Override
  public String toString() {
    return "HashPolicy{"
        + "type=" + type + ", "
        + "isTerminal=" + isTerminal + ", "
        + "headerName=" + headerName + ", "
        + "regEx=" + regEx + ", "
        + "regExSubstitution=" + regExSubstitution
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof VirtualHost.Route.RouteAction.HashPolicy) {
      VirtualHost.Route.RouteAction.HashPolicy that = (VirtualHost.Route.RouteAction.HashPolicy) o;
      return this.type.equals(that.type())
          && this.isTerminal == that.isTerminal()
          && (this.headerName == null ? that.headerName() == null : this.headerName.equals(that.headerName()))
          && (this.regEx == null ? that.regEx() == null : this.regEx.equals(that.regEx()))
          && (this.regExSubstitution == null ? that.regExSubstitution() == null : this.regExSubstitution.equals(that.regExSubstitution()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= type.hashCode();
    h$ *= 1000003;
    h$ ^= isTerminal ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= (headerName == null) ? 0 : headerName.hashCode();
    h$ *= 1000003;
    h$ ^= (regEx == null) ? 0 : regEx.hashCode();
    h$ *= 1000003;
    h$ ^= (regExSubstitution == null) ? 0 : regExSubstitution.hashCode();
    return h$;
  }

}
