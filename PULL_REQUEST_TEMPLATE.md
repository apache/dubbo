## What is the purpose of the change

XXXXX

## Brief changelog

XXXXX

## Verifying this change

XXXXX

Follow this checklist to help us incorporate your contribution quickly and easily:

- [x] Make sure there is a [GITHUB_issue](https://github.com/apache/dubbo/issues) field for the change (usually before you start working on it). Trivial changes like typos do not require a GITHUB issue. Your pull request should address just this issue, without pulling in other changes - one PR resolves one issue.
- [ ] Format the pull request title like `[Dubbo-XXX] Fix UnknownException when host config not exist #XXX`. Each commit in the pull request should have a meaningful subject line and body.
- [ ] Write a pull request description that is detailed enough to understand what the pull request does, how, and why.
- [ ] Write necessary unit-test to verify your logic correction, more mock a little better when cross module dependency exist. If the new feature or significant change is committed, please remember to add sample in [dubbo samples](https://github.com/apache/dubbo-samples) project.
- [ ] Run `mvn clean install -DskipTests=false` & `mvn clean test-compile failsafe:integration-test` to make sure unit-test and integration-test pass.
- [ ] If this contribution is large, please follow the [Software Donation Guide](https://github.com/apache/dubbo/wiki/Software-donation-guide).
