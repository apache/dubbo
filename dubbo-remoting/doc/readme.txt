远程通讯模块，相当于 Dubbo 协议的实现，如果 RPC 用 RMI 协议则不需要使用此包。
transport 层和 exchange 层都放在 remoting 模块中，为 rpc 调用的通讯基础。