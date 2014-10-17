/**
 * Copyright 1999-2014 dangdang.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.benchmark;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("echo")
@Consumes({MediaType.APPLICATION_JSON})
//@Consumes({MediaType.TEXT_XML})
@Produces({MediaType.APPLICATION_JSON})
//@Produces({MediaType.TEXT_XML})
public interface EchoService {

    @POST
    @Path("bid")
//    @GZIP
    BidRequest bid(/*@GZIP */BidRequest request);

    @POST
    @Path("text")
//    @GZIP
    Text text(/*@GZIP */Text text);
}