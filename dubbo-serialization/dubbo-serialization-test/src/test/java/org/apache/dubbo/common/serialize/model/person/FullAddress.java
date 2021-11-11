/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.serialize.model.person;

import java.io.Serializable;

public class FullAddress implements Serializable {

    private static final long serialVersionUID = 5163979984269419831L;

    private String countryId;

    private String countryName;

    private String provinceName;

    private String cityId;

    private String cityName;

    private String streetAddress;

    private String zipCode;

    public FullAddress() {
    }

    public FullAddress(String countryId, String provinceName, String cityId, String streetAddress,
                       String zipCode) {
        this.countryId = countryId;
        this.countryName = countryId;
        this.provinceName = provinceName;
        this.cityId = cityId;
        this.cityName = cityId;
        this.streetAddress = streetAddress;
        this.zipCode = zipCode;
    }

    public FullAddress(String countryId, String countryName, String provinceName, String cityId,
                       String cityName, String streetAddress, String zipCode) {
        this.countryId = countryId;
        this.countryName = countryName;
        this.provinceName = provinceName;
        this.cityId = cityId;
        this.cityName = cityName;
        this.streetAddress = streetAddress;
        this.zipCode = zipCode;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cityId == null) ? 0 : cityId.hashCode());
        result = prime * result + ((cityName == null) ? 0 : cityName.hashCode());
        result = prime * result + ((countryId == null) ? 0 : countryId.hashCode());
        result = prime * result + ((countryName == null) ? 0 : countryName.hashCode());
        result = prime * result + ((provinceName == null) ? 0 : provinceName.hashCode());
        result = prime * result + ((streetAddress == null) ? 0 : streetAddress.hashCode());
        result = prime * result + ((zipCode == null) ? 0 : zipCode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FullAddress other = (FullAddress) obj;
        if (cityId == null) {
            if (other.cityId != null)
                return false;
        } else if (!cityId.equals(other.cityId))
            return false;
        if (cityName == null) {
            if (other.cityName != null)
                return false;
        } else if (!cityName.equals(other.cityName))
            return false;
        if (countryId == null) {
            if (other.countryId != null)
                return false;
        } else if (!countryId.equals(other.countryId))
            return false;
        if (countryName == null) {
            if (other.countryName != null)
                return false;
        } else if (!countryName.equals(other.countryName))
            return false;
        if (provinceName == null) {
            if (other.provinceName != null)
                return false;
        } else if (!provinceName.equals(other.provinceName))
            return false;
        if (streetAddress == null) {
            if (other.streetAddress != null)
                return false;
        } else if (!streetAddress.equals(other.streetAddress))
            return false;
        if (zipCode == null) {
            if (other.zipCode != null)
                return false;
        } else if (!zipCode.equals(other.zipCode))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (countryName != null && countryName.length() > 0) {
            sb.append(countryName);
        }
        if (provinceName != null && provinceName.length() > 0) {
            sb.append(" ");
            sb.append(provinceName);
        }
        if (cityName != null && cityName.length() > 0) {
            sb.append(" ");
            sb.append(cityName);
        }
        if (streetAddress != null && streetAddress.length() > 0) {
            sb.append(" ");
            sb.append(streetAddress);
        }
        return sb.toString();
    }

}