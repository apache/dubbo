package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

final class PolicyMatcher implements Matcher {

  private final String name;

  private final OrMatcher permissions;

  private final OrMatcher principals;

    /** Constructs a matcher for one RBAC policy. */
    public static PolicyMatcher create(String name, OrMatcher permissions, OrMatcher principals) {
        return new PolicyMatcher(name, permissions, principals);
    }

    @Override
    public boolean matches(Object args) {
        return permissions().matches(args) && principals().matches(args);
    }


    PolicyMatcher(
      String name,
      OrMatcher permissions,
      OrMatcher principals) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (permissions == null) {
      throw new NullPointerException("Null permissions");
    }
    this.permissions = permissions;
    if (principals == null) {
      throw new NullPointerException("Null principals");
    }
    this.principals = principals;
  }

  public String name() {
    return name;
  }

  public OrMatcher permissions() {
    return permissions;
  }

  public OrMatcher principals() {
    return principals;
  }

  @Override
  public String toString() {
    return "PolicyMatcher{"
        + "name=" + name + ", "
        + "permissions=" + permissions + ", "
        + "principals=" + principals
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof PolicyMatcher) {
      PolicyMatcher that = (PolicyMatcher) o;
      return this.name.equals(that.name())
          && this.permissions.equals(that.permissions())
          && this.principals.equals(that.principals());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= permissions.hashCode();
    h$ *= 1000003;
    h$ ^= principals.hashCode();
    return h$;
  }

}
