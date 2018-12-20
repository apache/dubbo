# Release Notes

## 2.6.2

1. Hessian-lite serialization: revert locale serialization for compatibility, #1413
2. Asset transfer to ASF, includeing pom, license, DISCLAIMER and so on, #1491
3. Introduce of new dispatcher policy: EagerThreadpool, #1568
4. Separate monitor data with group and version, #1407
5. Spring Boot Enhancement, #1611
6. Graceful shutdown enhancement
   - Remove exporter destroy logic in AnnotationBean.
   - Waiting for registry notification on consumer side by checking channel state.
7. Simplify consumer/provider side check in RpcContext, #1444.

Issues and Pull Requests, check [milestone-2.6.2](https://github.com/apache/incubator-dubbo/milestone/15).
