namespace java org.apache.dubbo.rpc.protocol.nativethrift
namespace go demo
/*Demo service define file,can be generated to inteface files*/
/*Here test the 7 kind of data type*/
service DemoService {
    string sayHello(1:required string name);

    bool hasName( 1:required bool hasName);

    string sayHelloTimes(1:required string name, 2:required i32 times);

    void timeOut(1:required i32 millis);

    string customException();

    string context(1:required string name);
}