namespace java org.apache.dubbo.rpc.protocol.nativethrift
namespace go demo

service UserService {
    string find(1:required i32 id);
}