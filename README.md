# cqrs-framework

## Overview
Proof-of-concept of [CQRS](http://martinfowler.com/bliki/CQRS.html)/[ES](http://martinfowler.com/eaaDev/EventSourcing.html) 
framework/toolkit based on abstractions available in [Akka](http://akka.io) (mostly akka-persistence)  

The code was created during work on master thesis on AGH UST in cooperation with [Lufthansa Systems GmbH & Co. KG](http://www.lhsystems.com/) and as part of EU PaaSage FP7 project (grant 317715).

This code is available under [Mozilla Public License Version 2.0](https://www.mozilla.org/en-US/MPL/2.0/)

The code is available on Bintray, to use it add to your ```build.sbt```

```scala
resolvers += "cqrs-framework at bintray" at "http://dl.bintray.com/adebski/maven"

libraryDependencies += "cqrs-framework" %% "cqrs-framework" % "0.1.0"
```

## Dependencies

The most important dependencies are 

* [akka](http://akka.io) 2.4.0
* [Apache Cassandra](http://cassandra.apache.org/) 2.1.8
* [Apache kafka](http://kafka.apache.org/) 0.8.2.0

Some tests also assume that there is an instance of Kafka and Cassandra present, by default on local machine. Every endpoint address
can be configured through HOCON format that is used by Typesafe [config](https://github.com/typesafehub/config] library.

## Usage
Most important abstractions like *DomainView*, *AggregateRoot* and *AggregateRootActor* are documented through scala docs.
Also it is a good idea to check out the tests and [sample project](here-be-address) for examples of code that use those abstractions.

## Caveats and possible improvements

* The code was written before Akka 2.4.0 was released so it does not use new features 
like [persistence-query](http://doc.akka.io/docs/akka/snapshot/scala/persistence-query.html#persistence-query-scala)
so read-model/query side was implemented by hand using Akka actors. 
* Due to design choice Cassandra is used as long term storage and Kafka as event bus solution. Cassandra is used through
akka-persistence journal/snapshot API and [akka-persistence-cassandra plugin](https://github.com/krasserm/akka-persistence-cassandra/)
but sending events to Kafka is done in persist callback by sending a message to *ReplayablePubSub*. Currently this is a fire-and-forget process
without any ACKs and retires (apart from Kafka client settings) so there could be a situation where event is not propagated to Kafka properly 
and *AggregateRootActor* will never know about it.

## Building

Project is backed by SBT, the build configuration is pretty simple with no custom tasks. Only addition are [multi-jvm](http://doc.akka.io/docs/akka/2.4.0/dev/multi-jvm-testing.html)
tests that can be executed with ```multi-jvm:test``` command. 
