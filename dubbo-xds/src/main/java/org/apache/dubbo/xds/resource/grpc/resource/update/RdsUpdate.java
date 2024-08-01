package org.apache.dubbo.xds.resource.grpc.resource.update;

import com.google.common.base.MoreObjects;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.dubbo.xds.resource.grpc.resource.VirtualHost;

import java.util.ArrayList;
import java.util.Objects;

import java.util.*;

public class RdsUpdate implements ResourceUpdate {
    // The list virtual hosts that make up the route table.
    final List<VirtualHost> virtualHosts;

    public RdsUpdate(List<VirtualHost> virtualHosts) {
      this.virtualHosts = Collections.unmodifiableList(
          new ArrayList<>(checkNotNull(virtualHosts, "virtualHosts")));
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("virtualHosts", virtualHosts)
          .toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(virtualHosts);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RdsUpdate that = (RdsUpdate) o;
      return Objects.equals(virtualHosts, that.virtualHosts);
    }
  }
