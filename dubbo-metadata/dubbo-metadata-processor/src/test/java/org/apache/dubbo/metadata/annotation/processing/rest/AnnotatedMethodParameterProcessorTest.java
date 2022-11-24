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
package org.apache.dubbo.metadata.annotation.processing.rest;

import org.apache.dubbo.metadata.annotation.processing.AbstractAnnotationProcessingTest;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The abstract class for {@link AnnotatedMethodParameterProcessor}'s test cases
 *
 * @since 2.7.6
 */
public abstract class AnnotatedMethodParameterProcessorTest extends AbstractAnnotationProcessingTest {

    protected AnnotatedMethodParameterProcessor processor;

    protected RestMethodMetadata restMethodMetadata;

    protected abstract AnnotatedMethodParameterProcessor createTestInstance();

    @BeforeEach
    public final void prepare() {
        this.processor = createTestInstance();
        this.restMethodMetadata = createRestMethodMetadata();
    }

    protected RestMethodMetadata createRestMethodMetadata() {
        return new RestMethodMetadata();
    }

    protected abstract String getExpectedAnnotationType();

    @Test
    void testGetAnnotationType() {
        String expectedAnnotationType = getExpectedAnnotationType();
        assertNull(processor.getAnnotationType());
        assertEquals(expectedAnnotationType, processor.getAnnotationType());
    }
}
