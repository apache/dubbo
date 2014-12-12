目的：
快速的使用 maven archetype 进行 dubbox rest 项目工程搭建。
本处默认从 dubbo-demo-lite 构建一个 rest 版本的 dubbox 框架结构。

命令参考：

1. 从一个已有的项目中构建一个 maven archetype，
$ mvn archetype:create-from-project -Darchetype.filteredExtentions=java,xml,jsp,properties,sql

2. 然后再修改模板中的文件，并执行
$ mvn clean install

3. 开始从一个已有的 maven archetype 中复制项目
$ mkdir tmp
$ cd tmp
$ mvn archetype:generate -DarchetypeCatalog=local

更多资料请参考
http://maven.apache.org/archetype/maven-archetype-plugin/examples/create-multi-module-project.html


使用示例列举：

<pre>
kangfoo@kangfoo-dk:~/work/hawkeye/tmp$ mvn archetype:generate -DarchetypeCatalog=local
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building Maven Stub Project (No POM) 1
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] >>> maven-archetype-plugin:2.2:generate (default-cli) @ standalone-pom >>>
[INFO]
[INFO] <<< maven-archetype-plugin:2.2:generate (default-cli) @ standalone-pom <<<
[INFO]
[INFO] --- maven-archetype-plugin:2.2:generate (default-cli) @ standalone-pom ---
[INFO] Generating project in Interactive mode
[INFO] No archetype defined. Using maven-archetype-quickstart (org.apache.maven.archetypes:maven-archetype-quickstart:1.0)
Choose archetype:
1: local -> com.alibaba:dubbo-demo-lite-archetype (The demo lite module of dubbo project)
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): : 1
Define value for property 'groupId': : com.tima
Define value for property 'artifactId': : test3
Define value for property 'version':  1.0-SNAPSHOT: :
Define value for property 'package':  com.tima: : com.tima.test3
Confirm properties configuration:
groupId: com.tima
artifactId: test3
version: 1.0-SNAPSHOT
package: com.tima.test3
 Y: : Y
[INFO] ----------------------------------------------------------------------------
[INFO] Using following parameters for creating project from Archetype: dubbo-demo-lite-archetype:2.8.3
[INFO] ----------------------------------------------------------------------------
[INFO] Parameter: groupId, Value: com.tima
[INFO] Parameter: artifactId, Value: test3
[INFO] Parameter: version, Value: 1.0-SNAPSHOT
[INFO] Parameter: package, Value: com.tima.test3
[INFO] Parameter: packageInPathFormat, Value: com/tima/test3
[INFO] Parameter: package, Value: com.tima.test3
[INFO] Parameter: version, Value: 1.0-SNAPSHOT
[INFO] Parameter: groupId, Value: com.tima
[INFO] Parameter: artifactId, Value: test3
[INFO] Parent element not overwritten in /home/kangfoo/work/hawkeye/tmp/test3/test3-api/pom.xml
[INFO] Parent element not overwritten in /home/kangfoo/work/hawkeye/tmp/test3/test3-provider/pom.xml
[INFO] Parent element not overwritten in /home/kangfoo/work/hawkeye/tmp/test3/test3-consumer/pom.xml
[INFO] project created from Archetype in dir: /home/kangfoo/work/hawkeye/tmp/test3
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 27.023s
[INFO] Finished at: Fri Dec 12 14:49:00 CST 2014
[INFO] Final Memory: 12M/105M
[INFO] ------------------------------------------------------------------------
kangfoo@kangfoo-dk:~/work/hawkeye/tmp$ cd test3/
kangfoo@kangfoo-dk:~/work/hawkeye/tmp/test3$ ll
总用量 24
drwxrwxr-x 5 kangfoo kangfoo 4096 12月 12 14:49 ./
drwxrwxr-x 4 kangfoo kangfoo 4096 12月 12 14:49 ../
-rw-rw-r-- 1 kangfoo kangfoo 1793 12月 12 14:49 pom.xml
drwxrwxr-x 3 kangfoo kangfoo 4096 12月 12 14:49 test3-api/
drwxrwxr-x 3 kangfoo kangfoo 4096 12月 12 14:49 test3-consumer/
drwxrwxr-x 3 kangfoo kangfoo 4096 12月 12 14:49 test3-provider/
<pre>


