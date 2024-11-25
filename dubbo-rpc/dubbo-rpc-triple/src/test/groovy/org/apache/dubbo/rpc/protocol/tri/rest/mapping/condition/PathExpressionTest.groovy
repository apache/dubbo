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

package org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition

import org.apache.dubbo.rpc.protocol.tri.rest.Messages
import org.apache.dubbo.rpc.protocol.tri.rest.PathParserException

import spock.lang.Specification

import static org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression.parse
import static org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type.LITERAL
import static org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type.PATTERN
import static org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type.PATTERN_MULTI
import static org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type.VARIABLE
import static org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type.WILDCARD
import static org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type.WILDCARD_TAIL

class PathExpressionTest extends Specification {

    def "Parse"() {
        given:
            def pss = PathParser.parse(path)
        expect:
            assertPath(pss, rs)
        where:
            path                       | rs
            null                       | [[LITERAL, '/']]
            ''                         | [[LITERAL, '/']]
            '/'                        | [[LITERAL, '/']]
            '/a?*b'                    | [[PATTERN, 'a[^/]*b']]
            '/a*?b'                    | [[PATTERN, 'a[^/]*b']]
            '/a**b'                    | [[PATTERN, 'a[^/]*b']]
            '/{param:o?e}'             | [[PATTERN, '(?<param>o?e)']]
            '/{param: \\d+ }/name'     | [[PATTERN, '(?<param>\\d+)', 'param'], [LITERAL, 'name']]
            '/{param:\\S\\W}'          | [[PATTERN_MULTI, '(?<param>\\S\\W)']]
            '/{param:a{2}}'            | [[PATTERN, '(?<param>a{2})']]
            '/{param:[^/]+}'           | [[PATTERN, '(?<param>[^/]+)', 'param']]
            '/{param:[/]+}'            | [[PATTERN_MULTI, '(?<param>[/]+)', 'param']]
            '/{one}{two:\\d+}'         | [[PATTERN, '(?<one>[^/]+)(?<two>\\d+)', 'one', 'two']]
            '/{one:\\d+}{}'            | [[PATTERN, '(?<one>\\d+)[^/]+', 'one']]
            '/{one:\\d+}**'            | [[PATTERN_MULTI, '(?<one>\\d+).*', 'one']]
            '/{one:\\d+}{*two}'        | [[PATTERN_MULTI, '(?<one>\\d+)(?<two>.*)', 'one', 'two']]
            '/{first}-{last}/b/{vv}/v' | [[PATTERN, '(?<first>[^/]+)-(?<last>[^/]+)', 'first', 'last'], 'b', [VARIABLE, 'vv'], 'v']
    }

    def "Parse failed"() {
        when:
            PathParser.parse(path)
        then:
            def e = thrown(PathParserException)
            e.errorCode == code.name()
        where:
            path                | code
            '/{param:[a}'       | Messages.REGEX_PATTERN_INVALID
            '/{param:}'         | Messages.MISSING_REGEX_CONSTRAINT
            '/{one}/{*/three'   | Messages.MISSING_CLOSE_CAPTURE
            '/{one}/{two/three' | Messages.MISSING_CLOSE_CAPTURE
            '/{o{ne}'           | Messages.ILLEGAL_NESTED_CAPTURE
            '/{one}a}'          | Messages.MISSING_OPEN_CAPTURE
            '/one/{*postfix}t'  | Messages.NO_MORE_DATA_ALLOWED
            '/{one}{two}'       | Messages.ILLEGAL_DOUBLE_CAPTURE
            '/{one}-{one}'      | Messages.DUPLICATE_CAPTURE_VARIABLE
            '/{one}/{one'       | Messages.MISSING_CLOSE_CAPTURE
    }

    /**
     * <a href="https://docs.jboss.org/resteasy/docs/6.2.7.Final/userguide/html/ch04.html">resteasy @Path</a>
     */
    def "ParseJaxrs"() {
        given:
            def pss = PathParser.parse(path)
        expect:
            assertPath(pss, rs)
        where:
            path                             | rs
            '/library/books'                 | ['/library/books']
            '/library/{isbn}'                | ['library', [VARIABLE, 'isbn']]
            '/library/{isbn}/{type}'         | ['library', [VARIABLE, 'isbn'], [VARIABLE, 'type']]
            '{var:\\d+}/stuff'               | [[PATTERN, '(?<var>\\d+)', 'var'], [LITERAL, 'stuff']]
            '{var:.*}/stuff'                 | [[PATTERN_MULTI, '(?<var>.*)/stuff', 'var']]
            '/aaa{param}bbb'                 | [[PATTERN, 'aaa(?<param>[^/]+)bbb', 'param']]
            '/foo{name}-{zip}bar'            | [[PATTERN, 'foo(?<name>[^/]+)-(?<zip>[^/]+)bar', 'name', 'zip']]
            '/aaa{param:b+}/{many:.*}/stuff' | [[PATTERN, 'aaa(?<param>b+)', 'param'], [PATTERN_MULTI, '(?<many>.*)/stuff', 'many']]
    }

    /**
     * <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html#mvc-ann-requestmapping-uri-templates">Spring uri templates</a>
     */
    def "ParseSpring"() {
        given:
            def pss = PathParser.parse(path)
        expect:
            assertPath(pss, rs)
        where:
            path                                                | rs
            '/resources/ima?e.png'                              | ['resources', [PATTERN, 'ima[^/]e\\.png']]
            '/resources/*.png'                                  | ['resources', [PATTERN, '[^/]*\\.png']]
            '/resources/*/test.png'                             | ['resources', [VARIABLE, ''], 'test.png']
            '/resources/**/test.png'                            | ['resources', [PATTERN_MULTI, '.*\\Qtest.png\\E']]
            '/resources/**/*.png'                               | ['resources', [PATTERN_MULTI, '.*[^/]*\\.png']]
            '/resources/**'                                     | ['resources', [WILDCARD_TAIL, '']]
            '/resources/{**}'                                   | ['resources', [WILDCARD_TAIL, '*']]
            '/resources/{*path}'                                | ['resources', [WILDCARD_TAIL, 'path', 'path']]
            '/resources/*'                                      | ['resources', [VARIABLE, '']]
            '/projects/{project}/versions'                      | ['projects', [VARIABLE, 'project'], 'versions']
            '/projects/{project:[a-z]+}/versions'               | ['projects', [PATTERN, '(?<project>[a-z]+)', 'project'], 'versions']
            /{name:[a-z-]+}-{version:\d\.\d\.\d}{ext:\.[a-z]+}/ | [[PATTERN, /(?<name>[a-z-]+)-(?<version>\d\.\d\.\d)(?<ext>\.[a-z]+)/]]
    }

    def assertPath(PathSegment[] pss, List rs) {
        rs.eachWithIndex { s, i ->
            assert pss.length > i
            def ps = pss[i]
            if (s instanceof String) {
                assert ps.value == s
            } else if (s instanceof List) {
                assert ps.type == s[0]
                assert ps.value == s[1]
                for (j in 2..<s.size()) {
                    assert ps.variables[j - 2] == s[j]
                }
            }
        }
    }

    def "IsDirect"() {
        expect:
            parse(path).direct == result
        where:
            path              | result
            '/library/books'  | true
            '/library/{isbn}' | false
            '/library/**'     | false
    }

    def "Match"() {
        given:
            def map = parse(path).match(value)
        expect:
            if (result instanceof Boolean) {
                assert map != null == result
            } else if (result instanceof Map) {
                assert map == result
            }
        where:
            path                             | value                         | result
            '/library/books'                 | '/library/books'              | true
            '/library/{isbn}'                | '/library/1-84356-028-3'      | [isbn: '1-84356-028-3']
            '/library/{isbn}/{type}'         | '/library/1-84356-028-3/math' | [isbn: '1-84356-028-3', type: 'math']
            '/{var:.*}/stuff'                | '/123/stuff'                  | [var: '123']
            '/{var:.*}/stu'                  | '/123/stuff'                  | false
            '/{var:.*}/stuff'                | '/123/abc/stuff'              | [var: '123/abc']
            '/aaa{param}bbb'                 | '/aaac1cbbb'                  | [param: 'c1c']
            '/foo{name}-{zip}bar'            | '/fooabc-123bar'              | [name: 'abc', zip: '123']
            '/aaa{param:b+}/{many:.*}/stuff' | '/aaabbb/ccc/stuff'           | [param: 'bbb', many: 'ccc']
            '/resources/*.png'               | '/resources/cat.png'          | true
            '/resources/**/test.png'         | '/resources/abc/def/test.png' | true
            '/resources/**'                  | '/resources/a/b/c'            | true
            '/resources/{*path}'             | '/resources/a/b/c'            | [path: 'a/b/c']
            '/resources/*'                   | '/resources/a'                | true
            '/resources/{*}'                 | '/resources/a/b/c'            | true
            '/{id:\\d+}'                     | '/123'                        | [id: '123']
            '/{id:\\d+}'                     | '/one'                        | false
            '/a?cd/ef'                       | '/abcd/ef'                    | true
            '/a?cd/ef'                       | '/aaccd/ef'                   | false
            '/a?cd/ef'                       | '/a/cd/ef'                    | false
            '/a*cd/ef'                       | '/aaccd/ef'                   | true
            '/a*cd/ef'                       | '/acd/ef'                     | true
            '/a*cd/ef'                       | '/a/ccd/ef'                   | false
            '/one/*/three'                   | '/one/two/three'              | true
            '/one/*/three'                   | '/one/three'                  | false
            '/one/**/three'                  | '/one/three'                  | true
            '/one/**/*/three'                | '/one/three'                  | false
            '/{one:\\d+}{*two}'              | '/123abc/def'                 | [one: '123', two: 'abc/def']
            '/{one:\\d+}{two:.*}'            | '/123abc/def'                 | [one: '123', two: 'abc/def']
    }

    def "MatchTest"() {
        expect:
            parse(path).match(value) != null
        where:
            path             | value
            '/resources/{*}' | '/resources/a/b/c'
    }

    def "CompareTo"() {
        expect:
            parse(path) <=> parse(other) == result
        where:
            path                    | other             | result
            '/one'                  | '/one'            | 0
            '/one'                  | '/two'            | 0
            '/one/{two}/three'      | '/one/{t}/three'  | 0
            '/one/two'              | '/one/{two}'      | -1
            '/one/{two}/three'      | '/one/*/three'    | -1
            '/one/*/three'          | '/one/**/three'   | -1
            '/one/two'              | '/one'            | -1
            '/one/{two}'            | '/{one}/two'      | -1
            '/one/{two}'            | '/one/{two:\\d+}' | -1
            '/one/*'                | '/one/{two:\\d+}' | -1
            '/one/{two:\\d+}/three' | '/one/**/three'   | -1
    }

    def "CompareTo with lookup path"() {
        expect:
            parse(path).compareTo(parse(other), lookupPath) == result
        where:
            path   | other  | lookupPath | result
            '/one' | '/two' | '/one'     | -1
            '/one' | '/two' | '/two'     | 1
            '/one' | '/two' | '/three'   | 0
    }

    def "Equals"() {
        expect:
            Objects.equals(path, other) == result
            if (result) {
                path.hashCode() == other.hashCode()
            }
        where:
            path          | other         | result
            parse('/one') | parse('/one') | true
            parse('/one') | parse('/two') | false
            parse('/two') | null          | false
            parse('/two') | '/two'        | false
    }

    @SuppressWarnings('ChangeToOperator')
    def "Misc"() {
        given:
            def path = '/library/{isbn}/{type}'
            def expr = parse(path)
            def seg = expr.segments[1]
        expect:
            PathExpression.match(path, "/library/1-84356-028-3/math")
            expr.path == path
            expr.equals(expr)
            expr.segments.length == 3
            expr.toString() == path
            seg.hashCode()
            seg.equals(seg)
            !seg.equals(null)
            !seg.equals(expr.segments[2])
            !new PathSegment(WILDCARD, '').match('', 0, 0, null)
            seg.toString() == '{type=VARIABLE, value=isbn, variables=[isbn]}'
    }
}
