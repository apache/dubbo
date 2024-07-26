package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

final class AlwaysTrueMatcher implements Matcher {


    public static AlwaysTrueMatcher INSTANCE = new AlwaysTrueMatcher();

    @Override
    public boolean matches(Object args) {
        return true;
    }

    AlwaysTrueMatcher() {
  }

  @Override
  public String toString() {
    return "AlwaysTrueMatcher{"
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AlwaysTrueMatcher) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    return h$;
  }

}
