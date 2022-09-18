package org.apache.dubbo.errorcode.reporter;

import java.util.List;
import java.util.Objects;

/**
 * Info here.
 */
public class ReportResult {
    private List<String> allErrorCodes;

    private List<String> linkNotReachableErrorCodes;

    // Generated methods, re-generate them if necessary.

    public List<String> getAllErrorCodes() {
        return allErrorCodes;
    }

    public void setAllErrorCodes(List<String> allErrorCodes) {
        this.allErrorCodes = allErrorCodes;
    }

    public List<String> getLinkNotReachableErrorCodes() {
        return linkNotReachableErrorCodes;
    }

    public void setLinkNotReachableErrorCodes(List<String> linkNotReachableErrorCodes) {
        this.linkNotReachableErrorCodes = linkNotReachableErrorCodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReportResult that = (ReportResult) o;

        if (!Objects.equals(allErrorCodes, that.allErrorCodes))
            return false;
        return Objects.equals(linkNotReachableErrorCodes, that.linkNotReachableErrorCodes);
    }

    @Override
    public int hashCode() {
        int result = allErrorCodes != null ? allErrorCodes.hashCode() : 0;
        result = 31 * result + (linkNotReachableErrorCodes != null ? linkNotReachableErrorCodes.hashCode() : 0);
        return result;
    }
}
