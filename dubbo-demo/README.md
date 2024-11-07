# Dubbo Demo

This directory contains basic usages of Dubbo to help Dubbo developers for debugging and smoke test purpose. If you are looking for Dubbo samples for study purpose, you should look into here where you will find comprehensive usages for how to use Dubbo in different scenarios with the different features.

## Brief introduction

Dubbo provides three demos that illustrate different usage scenarios:

1. **`dubbo-demo-api`**
   This demo demonstrates the basic usage of Dubbo, serving as a fundamental example. It showcases how to define and implement a simple service using Dubbo, along with how to register and consume the service. This is a great starting point for understanding core concepts in Dubbo, such as interface definitions, service exposure, and service consumption.
2. **`dubbo-demo-springboot`**
   This demo illustrates the integration of Dubbo with Spring Boot, showing how Dubbo services can be utilized and configured within a Spring Boot application. It demonstrates how to configure and manage Dubbo services seamlessly through Spring Boot, making it ideal for developers leveraging the popular Spring Boot framework.
3. **`dubbo-demo-springboot-idl`**
   This demo focuses on showcasing how to use Dubbo with Spring Boot when IDL (Interface Definition Language) files such as Proto files are available. It illustrates how developers can work with Dubbo services defined through IDL, integrating them into a Spring Boot application.

## How to run

First, you need to set up Zookeeper as the registry center. Use the `cd` command to navigate to the specific module directory, then run `mvn clean compile` (particularly for the `dubbo-demo-springboot-idl` module, as code generated from the proto files is located in the target directory). Start the provider first, followed by the consumer, to experience the functionality of Dubbo.
