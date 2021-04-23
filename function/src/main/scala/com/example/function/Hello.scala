package com.example.function

import com.google.cloud.functions.{BackgroundFunction, Context}
import com.google.cloud.pubsub.v1.Publisher
import com.google.events.cloud.pubsub.v1.Message
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{ProjectTopicName, PubsubMessage}
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.util.Base64
import com.google.pubsub.v1.TopicName

class Hello extends BackgroundFunction[Message] {

  private val logger = LoggerFactory.getLogger(classOf[Hello])

  private val projectId = sys.env("GCP_PROJECT_ID")

  private val topicName = sys.env("GCP_CLOUD_PUBSUB_TOPIC_NAME_RESPONSE")

  private val publisher = Publisher.newBuilder(ProjectTopicName.of(projectId, topicName)).build();

  override def accept(message: Message, context: Context): Unit = {
    val messageBody = for {
      m <- Option(message)
      d <- Option(m.getData)
    } yield new String(Base64.getDecoder.decode(d.getBytes(StandardCharsets.UTF_8)))

    val defaultMessage = "Hello, World!"

    val messageString = messageBody.getOrElse(defaultMessage)

    logger.info(s"Received: ${messageString}")

    val dataAsBytes = ByteString.copyFromUtf8(messageString)
    val pubsubMessage = PubsubMessage.newBuilder().setData(dataAsBytes).build()

    publisher.publish(pubsubMessage).get()
  }

}
