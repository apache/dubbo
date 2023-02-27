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
package org.apache.dubbo.monitor.dubbo;

public class StatisticsItem {

    private long success;
    private long failure;
    private long input;
    private long output;
    private long elapsed;
    private long concurrent;
    private long maxInput;
    private long maxOutput;
    private long maxElapsed;
    private long maxConcurrent;

    public StatisticsItem() {
    }

    public void setItems(long success, long failure, long input, long output, long elapsed, long concurrent) {
        this.setItems(success, failure, input, output, elapsed, concurrent, 0, 0, 0, 0);
    }

    public void setItems(long success, long failure, long input, long output, long elapsed, long concurrent,
                         long maxInput, long maxOutput, long maxElapsed, long maxConcurrent) {
        this.success = success;
        this.failure = failure;
        this.input = input;
        this.output = output;
        this.elapsed = elapsed;
        this.concurrent = concurrent;
        this.maxInput = maxInput;
        this.maxOutput = maxOutput;
        this.maxElapsed = maxElapsed;
        this.maxConcurrent = maxConcurrent;
    }

    public long getSuccess() {
        return success;
    }

    public void setSuccess(long success) {
        this.success = success;
    }

    public long getFailure() {
        return failure;
    }

    public void setFailure(long failure) {
        this.failure = failure;
    }

    public long getInput() {
        return input;
    }

    public void setInput(long input) {
        this.input = input;
    }

    public long getOutput() {
        return output;
    }

    public void setOutput(long output) {
        this.output = output;
    }

    public long getElapsed() {
        return elapsed;
    }

    public void setElapsed(long elapsed) {
        this.elapsed = elapsed;
    }

    public long getConcurrent() {
        return concurrent;
    }

    public void setConcurrent(long concurrent) {
        this.concurrent = concurrent;
    }

    public long getMaxInput() {
        return maxInput;
    }

    public void setMaxInput(long maxInput) {
        this.maxInput = maxInput;
    }

    public long getMaxOutput() {
        return maxOutput;
    }

    public void setMaxOutput(long maxOutput) {
        this.maxOutput = maxOutput;
    }

    public long getMaxElapsed() {
        return maxElapsed;
    }

    public void setMaxElapsed(long maxElapsed) {
        this.maxElapsed = maxElapsed;
    }

    public long getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(long maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }
}
