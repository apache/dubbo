# Readme

## When to modify

### When a new module is introduced

Need to add one new `<artifactSet> -> <includes> -> <include>` in maven-shad-plugin's configuration:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <executions>
        <execution>
            <configuration>
                <artifactSet>
                    <includes>
                        <include>com.alibaba:new-module-name</include>
                    </includes>
                </artifactSet>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### When a new extension type is introduced

Need to add one new `<transformers> -> <transformer>` in maven-shade-plugin's configuration:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <executions>
        <execution>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                        <resource>META-INF/dubbo/internal/com.alibaba.dubbo.NewTypeExtension</resource>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

All extension types in dubbo project could be listed by the following command:

```sh
$ find . -wholename */META-INF/dubbo/* -type f | grep -vF /test/ | awk -F/ '{print $NF}' | sort -u
```
