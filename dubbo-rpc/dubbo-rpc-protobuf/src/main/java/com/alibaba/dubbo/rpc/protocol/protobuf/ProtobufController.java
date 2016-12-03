/**
 * Copyright (c) 2015 Illuminate inc.
 *
 * * Licensed under the Apache License, Version 2.0 (the "License");
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
package com.alibaba.dubbo.rpc.protocol.protobuf;

import com.google.protobuf.RpcCallback;

/**
 * 类名称:ProtobufController 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月23日-下午4:29:51 修改备注:
 */
public class ProtobufController implements com.google.protobuf.RpcController, java.io.Serializable {

  // 提供默认实例，避免创建
  private static final ProtobufController defaultInstance = new ProtobufController();

  public static ProtobufController getDefaultInstance() {
    return defaultInstance;
  }

  private String reason;
  private boolean failed;
  private boolean canceled;

  @SuppressWarnings("unused")
  private RpcCallback<Object> callback;

  public String errorText() {
    return reason;
  }

  public boolean failed() {
    return failed;
  }

  public boolean isCanceled() {
    return canceled;
  }

  public void notifyOnCancel(RpcCallback<Object> callback) {
    this.callback = callback;
  }

  public void reset() {
    reason = null;
    failed = false;
    canceled = false;
    callback = null;
  }

  public void setFailed(String reason) {
    this.reason = reason;
    this.failed = true;
  }

  public void startCancel() {
    canceled = true;
  }

}
