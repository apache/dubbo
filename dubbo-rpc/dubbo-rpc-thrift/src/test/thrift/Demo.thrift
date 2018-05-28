namespace dubbo_java com.alibaba.dubbo.rpc.gen.dubbo
namespace dubbo_cpp  com.alibaba.dubbo.rpc.gen.dubbo

namespace java com.alibaba.dubbo.rpc.gen.thrift
namespace go demo
/*Demo service define file,can be generated to inteface files*/
/*Here test the 7 kind of data type*/
service Demo {
    bool echoBool( 1:required bool arg );
    byte echoByte( 1:required byte arg );
    i16  echoI16 ( 1:required i16  arg );
    i32  echoI32 ( 1:required i32  arg );
    i64  echoI64 ( 1:required i64  arg );

    double echoDouble( 1:required double arg );
    string echoString( 1:required string arg );
}