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
package org.apache.dubbo.rpc.listener;

import org.apache.dubbo.rpc.Exporter;

/**
 * An interface for listening to changes in the export state of an Exporter object.
 */
public interface ExporterChangeListener {

    /**
     * This method is called when an Exporter object is exported.
     *
     * @param exporter The Exporter object that has been exported.
     */
    void onExporterChangeExport(Exporter<?> exporter);

    /**
     * This method is called when an Exporter object is unexported.
     *
     * @param exporter The Exporter object that has been unexported.
     */
    void onExporterChangeUnExport(Exporter<?> exporter);
}
