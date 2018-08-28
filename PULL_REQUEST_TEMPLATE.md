# What is the purpose of the change
add the unit test of the HeaderExchangeChannel.class which is located in incubator-dubbo-master\dubbo-remoting\dubbo-remoting-api\src\main\java\org\apache\dubbo\remoting\exchange\support\header\HeaderExchangeChannel.java
# Brief changelog
### 1.Add HeaderExchangeChannelTest.class
add the unit tests for the methds as follows:

- HeaderExchangeChannel getOrAddChannel(Channel ch)
- void removeChannelIfDisconnected(Channel ch)
- void send(Object message
- void send(Object message, boolean sent)
- ResponseFuture request(Object request)
- ResponseFuture request(Object request, int timeout)
- boolean isClosed()
- void close()
- void close(int timeout)
- void startClose()
- InetSocketAddress getLocalAddress()
- InetSocketAddress getRemoteAddress()
- URL getUrl()
- boolean isConnected()
- ChannelHandler getChannelHandler()
- ExchangeHandler getExchangeHandler()
- Object getAttribute(String key)
- void setAttribute(String key, Object value)
- void removeAttribute(String key)
- boolean hasAttribute(String key)
- int hashCode()
### 2.Modify MockChannel.class
add the method:
 ```
public boolean isClosing(){return  closing;}
 ```

Follow this checklist to help us incorporate your contribution quickly and easily:

- [x] Make sure there is a [GITHUB_issue](https://github.com/apache/incubator-dubbo/issues) filed for the change (usually before you start working on it). Trivial changes like typos do not require a GITHUB issue. Your pull request should address just this issue, without pulling in other changes - one PR resolves one issue.
- [ ] Format the pull request title like `[Dubbo-XXX] Fix UnknownException when host config not exist #XXX`. Each commit in the pull request should have a meaningful subject line and body.
- [ ] Write a pull request description that is detailed enough to understand what the pull request does, how, and why.
- [ ] Write necessary unit-test to verify your logic correction, more mock a little better when cross module dependency exist. If the new feature or significant change is committed, please remember to add integration-test in [test module](https://github.com/apache/incubator-dubbo/tree/master/dubbo-test).
- [ ] Run `mvn clean install -DskipTests` & `mvn clean test-compile failsafe:integration-test` to make sure unit-test and integration-test pass.
- [ ] If this contribution is large, please follow the [Software Donation Guide](https://github.com/apache/incubator-dubbo/wiki/Software-donation-guide).
