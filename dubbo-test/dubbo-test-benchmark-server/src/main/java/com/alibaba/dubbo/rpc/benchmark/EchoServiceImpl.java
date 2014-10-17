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

public class EchoServiceImpl implements EchoService {

//    private final BidResponse response = new BidResponse();

    public EchoServiceImpl() {
//        response.setId("abc");
//
//        SeatBid seatBid = new SeatBid();
//        seatBid.setGroup("group");
//        seatBid.setSeat("seat");
//        List<SeatBid> seatBids = new ArrayList<SeatBid>(1);
//        seatBids.add(seatBid);
//
//        response.setSeatBids(seatBids);
    }

    public BidRequest bid(BidRequest request) {
        return request;
    }

    public Text text(Text text) {
        return text;
    }
}