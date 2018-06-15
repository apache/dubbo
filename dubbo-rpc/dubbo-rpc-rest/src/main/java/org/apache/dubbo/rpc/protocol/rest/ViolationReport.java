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
package org.apache.dubbo.rpc.protocol.rest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name="violationReport")
@XmlAccessorType(XmlAccessType.FIELD)
public class ViolationReport implements Serializable {

    private static final long serialVersionUID = -130498234L;

    private List<RestConstraintViolation> constraintViolations;

    public List<RestConstraintViolation> getConstraintViolations() {
        return constraintViolations;
    }

    public void setConstraintViolations(List<RestConstraintViolation> constraintViolations) {
        this.constraintViolations = constraintViolations;
    }

    public void addConstraintViolation(RestConstraintViolation constraintViolation) {
        if (constraintViolations == null) {
            constraintViolations = new LinkedList<RestConstraintViolation>();
        }
        constraintViolations.add(constraintViolation);
    }
}
