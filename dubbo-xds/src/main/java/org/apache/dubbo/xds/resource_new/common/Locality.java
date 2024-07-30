package org.apache.dubbo.xds.resource_new.common;

public class Locality {

    private String region;

    private String zone;

    private String subZone;

    public Locality(
            String region, String zone, String subZone) {
        if (region == null) {
            throw new NullPointerException("Null region");
        }
        this.region = region;
        if (zone == null) {
            throw new NullPointerException("Null zone");
        }
        this.zone = zone;
        if (subZone == null) {
            throw new NullPointerException("Null subZone");
        }
        this.subZone = subZone;
    }

    String region() {
        return region;
    }

    String zone() {
        return zone;
    }

    String subZone() {
        return subZone;
    }

    @Override
    public String toString() {
        return "Locality{" + "region=" + region + ", " + "zone=" + zone + ", " + "subZone=" + subZone + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Locality) {
            Locality that = (Locality) o;
            return this.region.equals(that.region()) && this.zone.equals(that.zone())
                    && this.subZone.equals(that.subZone());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= region.hashCode();
        h$ *= 1000003;
        h$ ^= zone.hashCode();
        h$ *= 1000003;
        h$ ^= subZone.hashCode();
        return h$;
    }

}
