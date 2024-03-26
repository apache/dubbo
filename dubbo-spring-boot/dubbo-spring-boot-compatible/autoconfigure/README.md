# About spring-configuration-metadata

## Why

Configuration beans of dubbo are located in the `dubbo-common` module, and it's not suitable to add spring boot dependencies. 
However, spring configuration metadata generation relies on read javadoc from the source code and cannot use the @NestedConfigurationProperty annotation. 
This leads to missing comments and a lack of nested configuration options. Therefore, we use an independent module to copy the code and generate metadata.

## Principles

1. Copy classes under `org/apache/dubbo/config` from `dubbo-common` to the `generated-sources` directory.
2. Replace `@Nest` with `@NestedConfigurationProperty`.
3. Use an annotation-only option to compile and generate `spring-configuration-metadata.json`.

## How to add a new configuration option

- For standard configuration options, add javadoc to the corresponding configuration classes in `dubbo-common`.
- For non-standard configuration options, there are unnecessary to add nested classes. add them directly to `additional-spring-configuration-metadata.json`.

## Configuration Javadoc Guideline

1. For noun-type configuration options, use "The xxx" format for comments.
2. For boolean-type configuration options, use "Whether to xxx" format and add ", default value is &lt;code&gt;true&lt;/code&gt;" at the end.
3. For configuration options with longer comments, use multi-line comments, with a clear summary in the first line.
4. All comments should end with a period.
