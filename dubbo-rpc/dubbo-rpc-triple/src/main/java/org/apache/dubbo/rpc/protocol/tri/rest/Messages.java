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
package org.apache.dubbo.rpc.protocol.tri.rest;

import org.apache.dubbo.common.utils.ArrayUtils;

import java.text.MessageFormat;

public enum Messages {
    MISSING_CLOSE_CAPTURE("Expected close capture character after variable name '}' for path ''{0}'' at index {1}"),
    MISSING_OPEN_CAPTURE("Missing preceding open capture character before variable name '{' for path ''{0}''"),
    ILLEGAL_NESTED_CAPTURE("Not allowed to nest variable captures for path ''{0}'' at index {1}"),
    ILLEGAL_DOUBLE_CAPTURE("Not allowed to capture ''{0}'' twice in the same pattern"),
    DUPLICATE_CAPTURE_VARIABLE("Duplicate capture variable ''{0}''"),
    MISSING_REGEX_CONSTRAINT("Missing regex constraint on capture for path ''{0}'' at index {1}"),
    REGEX_PATTERN_INVALID("Invalid regex pattern ''{0}'' for path ''{0}''"),
    NO_MORE_DATA_ALLOWED("No more data allowed after '{*...}' or '**' pattern segment for path ''{0}'' at index {1}"),
    CANNOT_COMBINE_PATHS("Cannot combine paths: ''{0}'' vs ''{1}''"),
    DUPLICATE_MAPPING(
            "Duplicate mapping for ''{0}'': mapping={1}, method={2}, exists={3}", "Duplicate mapping for ''{0}''"),
    AMBIGUOUS_MAPPING("Ambiguous mapping for ''{0}'': [{1}, {2}]", "Ambiguous mapping for ''{0}''"),
    EXTENSION_INIT_FAILED(
            "Rest extension: ''{0}'' initialization failed for invoker: ''{1}''",
            "Rest extension initialization failed"),
    ARGUMENT_NAME_MISSING("Name for argument of type [{0}] not specified, and parameter name information not "
            + "available via reflection. Ensure that the compiler uses the '-parameters' flag"),
    ARGUMENT_VALUE_MISSING("Missing argument ''{0}'' for method parameter of type [{1}]", 400),
    ARGUMENT_CONVERT_ERROR("Could not convert argument ''{0}'' value ''{1}'' from type [{2}] to type [{3}]", 400),
    ARGUMENT_COULD_NOT_RESOLVED("Could not resolve ''{0}'', no suitable resolver", 400),
    ARGUMENT_BIND_ERROR("Bind argument ''{0}'' of type [{1}] error", 400),
    BAD_REQUEST("Rest Bad Request", 400),
    INTERNAL_ERROR("Rest Internal Error");

    private final String message;
    private final String displayMessage;
    private final int statusCode;

    Messages(String message) {
        this.message = message;
        displayMessage = null;
        statusCode = 500;
    }

    Messages(String message, String displayMessage) {
        this.message = message;
        this.displayMessage = displayMessage;
        statusCode = 500;
    }

    Messages(String message, int statusCode) {
        this.message = message;
        displayMessage = null;
        this.statusCode = statusCode;
    }

    Messages(String message, String displayMessage, int statusCode) {
        this.message = message;
        this.displayMessage = displayMessage;
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    public String format(Object... args) {
        return ArrayUtils.isEmpty(args) ? message : MessageFormat.format(message, args);
    }

    public String formatDisplay(Object... args) {
        return displayMessage == null || ArrayUtils.isEmpty(args)
                ? displayMessage
                : MessageFormat.format(displayMessage, args);
    }
}
