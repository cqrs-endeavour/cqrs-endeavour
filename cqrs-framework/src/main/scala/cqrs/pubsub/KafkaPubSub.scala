/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import java.util.{ Properties, UUID }

import akka.actor.ActorRef
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.StrictLogging
import cqrs.AkkaSerializer
import cqrs.query.EventEnvelope
import kafka.consumer.{ Consumer, ConsumerConfig, Whitelist }
import kafka.producer.{ KeyedMessage, Producer, ProducerConfig }
import kafka.serializer.DefaultDecoder
import scala.collection.JavaConverters._

/**
 * Kafka-based implementation of the [[ReplayablePubSub]]
 */
private[cqrs] class KafkaPubSub extends ReplayablePubSub with StrictLogging {
  import cqrs.pubsub.KafkaPubSub._

  private val config = context.system.settings.config
  private val serializer = new AkkaSerializer(context.system)

  private val producer = new Producer[AnyRef, AnyRef](producerConfig(config))
  private val consumer = Consumer.create(consumerConfig(config))

  override def publish(topic: String, message: EventEnvelope): Unit = {
    val serialized = serializer.serialize(message)
    logger.debug(s"event: $message published to Kafka")
    producer.send(new KeyedMessage(topic, serialized))
  }

  override def publishWithPartitioning(topic: String, message: EventEnvelope, key: UUID): Unit = {
    val serializedPayload = serializer.serialize(message)
    val serializedKey = serializer.serialize(key)
    producer.send(new KeyedMessage(topic, serializedKey, serializedPayload))
  }

  override def subscribeFromScratch(topic: String, subscriber: ActorRef): Unit = {
    context.dispatcher.execute(new Runnable {
      override def run(): Unit = {

        val filterSpec = new Whitelist(topic)
        val stream = consumer.createMessageStreamsByFilter(filterSpec, 1, new DefaultDecoder(), new DefaultDecoder()).head
        logger.info(s"Subscribed to $topic topic in Kafka")
        for (messageAndTopic <- stream) {
          val event = serializer.deserialize(messageAndTopic.message(), classOf[EventEnvelope])
          logger.debug(s"event: $event read from Kafka")
          subscriber ! event
        }
      }
    })
  }

  override def postStop(): Unit = {
    producer.close()
    consumer.shutdown()
  }
}

private[cqrs] object KafkaPubSub {
  private def configToProperties(config: Config, extra: Map[String, String] = Map.empty): Properties = {
    val properties = new Properties()

    config.entrySet.asScala.foreach { entry =>
      properties.put(entry.getKey, entry.getValue.unwrapped.toString)
    }

    extra.foreach {
      case (k, v) => properties.put(k, v)
    }

    properties
  }

  def consumerConfig(config: Config): ConsumerConfig = {
    val properties = configToProperties(config.getConfig("cqrs.kafka-pub-sub.consumer"))
    properties.put("group.id", config.getString("cqrs.kafka-pub-sub.node-name"))
    new ConsumerConfig(properties)
  }

  def producerConfig(config: Config): ProducerConfig = {
    val properties = configToProperties(config.getConfig("cqrs.kafka-pub-sub.producer"))
    properties.put("client.id", config.getString("cqrs.kafka-pub-sub.node-name"))

    new ProducerConfig(properties)
  }
}
