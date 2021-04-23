package com.example.client

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.Subscriber
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import scala.util.Using

object Client {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val credentialsFile = sys.env("GCP_CREADENTIALS_FILE")
  private val projectId = sys.env("GCP_PROJECT_ID")
  private val topicNameRequest = sys.env("GCP_CLOUD_PUBSUB_TOPIC_NAME_REQUEST")
  private val topicNameResponse = sys.env("GCP_CLOUD_PUBSUB_TOPIC_NAME_RESPONSE")
  private val subscriptionId = sys.env("GCP_CLOUD_PUBSUB_SUBSCRIPTION_NAME_RESPONSE")
  private val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);

  @volatile private var replyReceived: Boolean = false

  def main(args: Array[String]): Unit = {

    val credentials = Using.resource(Files.newInputStream(Paths.get(credentialsFile))) { in =>
      GoogleCredentials.fromStream(in)
    }

    val credentialsProvider = FixedCredentialsProvider.create(credentials)

    val receiver: MessageReceiver = (message: PubsubMessage, consumer: AckReplyConsumer) => {
      logger.info("Id: " + message.getMessageId());
      logger.info("Data: " + message.getData().toStringUtf8());
      consumer.ack();
      replyReceived = true
    }

    val subscriber: Subscriber =
      Subscriber.newBuilder(subscriptionName, receiver).setCredentialsProvider(credentialsProvider).build()

    val publisher = Publisher
      .newBuilder(ProjectTopicName.of(projectId, topicNameRequest))
      .setCredentialsProvider(credentialsProvider)
      .build()

    subscriber.startAsync().awaitRunning()

    logger.info(s"Started Subscriber")

    try {
      val now = ZonedDateTime.now()
      val message = s"Hello[timestamp=$now]"
      val dataAsBytes = ByteString.copyFromUtf8(message)
      val pubsubMessage = PubsubMessage.newBuilder().setData(dataAsBytes).build()

      val messageId = publisher.publish(pubsubMessage).get()
      logger.info(s"Published: messageId=$messageId, data=$message")
    } finally {
      publisher.shutdown()
    }

    while (!replyReceived) {
      logger.info("Waiting...")
      Thread.sleep(1000)
    }

    subscriber.stopAsync()
    subscriber.awaitTerminated()
  }

}
