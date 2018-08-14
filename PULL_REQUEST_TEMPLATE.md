## What is the purpose of the change


A port conflict problem of dubbo provider when multiple services start concurrently.

If you use a dynamic port, Dubbo will detect it first, then listen. If multiple processes start concurrently, it will cause the same port to be used occasionally.

If the dubbo port configuration of both threads is -1, if two ports occupy a conflict, one of the processes will retry.

If the dubbo port configuration of one thread is -1, and another is set to be fixed (such as 20880), if the process whose port is configured as -1 preempts port 20880, the process with the port number of 20880 will be retried three time. If retries still fail after three times, just try out new ports incrementally.

If the dubbo port configuration of both threads is a fixed port (such as 20880), then when one process occupies 20880 ports, another process will retry three times and will be assigned an available port(not 20880). .
## Brief changelog

The simple fix is ​​to re-probe the dynamic port if it fails.

If the number of retries for a port is more than three, then the port of the dubbo service is set to -1, and the available port will be dynamically obtained.

## Verifying this change


Follow this checklist to help us incorporate your contribution quickly and easily:

- [x] Make sure there is a [GITHUB_issue](https://github.com/apache/incubator-dubbo/issues) filed for the change (usually before you start working on it). Trivial changes like typos do not require a GITHUB issue. Your pull request should address just this issue, without pulling in other changes - one PR resolves one issue.
- [x] Format the pull request title like `[Dubbo-XXX] Fix UnknownException when host config not exist #XXX`. Each commit in the pull request should have a meaningful subject line and body.
- [x] Write a pull request description that is detailed enough to understand what the pull request does, how, and why.
- [ ] Write necessary unit-test to verify your logic correction, more mock a little better when cross module dependency exist. If the new feature or significant change is committed, please remember to add integration-test in [test module](https://github.com/apache/incubator-dubbo/tree/master/dubbo-test).
- [x] Run `mvn clean install -DskipTests` & `mvn clean test-compile failsafe:integration-test` to make sure unit-test and integration-test pass.
- [x] If this contribution is large, please follow the [Software Donation Guide](https://github.com/apache/incubator-dubbo/wiki/Software-donation-guide).
