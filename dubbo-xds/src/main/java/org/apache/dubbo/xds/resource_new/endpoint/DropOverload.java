package org.apache.dubbo.xds.resource_new.endpoint;

public class DropOverload {

    private final String category;

    private final int dropsPerMillion;

    public DropOverload(
            String category, int dropsPerMillion) {
        if (category == null) {
            throw new NullPointerException("Null category");
        }
        this.category = category;
        this.dropsPerMillion = dropsPerMillion;
    }

    String category() {
        return category;
    }

    int dropsPerMillion() {
        return dropsPerMillion;
    }

    @Override
    public String toString() {
        return "DropOverload{" + "category=" + category + ", " + "dropsPerMillion=" + dropsPerMillion + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DropOverload) {
            DropOverload that = (DropOverload) o;
            return this.category.equals(that.category()) && this.dropsPerMillion == that.dropsPerMillion();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= category.hashCode();
        h$ *= 1000003;
        h$ ^= dropsPerMillion;
        return h$;
    }

}
