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
namespace java org.apache.dubbo.rpc.protocol.nativethrift
namespace go demo
/*Demo service define file,can be generated to interface files*/
/*Here test the 7 kind of data type*/
service DemoService {
    string sayHello(1:required string name);

    bool hasName( 1:required bool hasName);

    string sayHelloTimes(1:required string name, 2:required i32 times);

    void timeOut(1:required i32 millis);

    string customException();

    string context(1:required string name);
}