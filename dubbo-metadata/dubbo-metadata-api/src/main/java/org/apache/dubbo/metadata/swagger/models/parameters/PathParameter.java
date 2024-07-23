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
package org.apache.dubbo.metadata.swagger.models.parameters;

import java.util.Objects;

/**
 * PathParameter
 */
public class PathParameter extends Parameter {
    private String in = "path";
    private Boolean required = true;

    /**
     * returns the in property from a PathParameter instance.
     *
     * @return String in
     **/
    @Override
    public String getIn() {
        return in;
    }

    @Override
    public void setIn(String in) {
        this.in = in;
    }

    @Override
    public PathParameter in(String in) {
        this.in = in;
        return this;
    }

    /**
     * returns the required property from a PathParameter instance.
     *
     * @return Boolean required
     **/
    @Override
    public Boolean getRequired() {
        return required;
    }

    @Override
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Override
    public PathParameter required(Boolean required) {
        this.required = required;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PathParameter pathParameter = (PathParameter) o;
        return Objects.equals(this.in, pathParameter.in)
                && Objects.equals(this.required, pathParameter.required)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(in, required, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PathParameter {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    in: ").append(toIndentedString(in)).append("\n");
        sb.append("    required: ").append(toIndentedString(required)).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
