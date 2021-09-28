package com.rabbitmq.stream;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "super-stream-app",
    mixinStandardHelpOptions = true,
    description = "A RabbitMQ super stream application.")
public class SuperStreamApp implements Callable<Integer> {

  @Parameters(
      index = "0",
      description = "Mode of the application, producer or consumer.",
      defaultValue = "consumer")
  private String mode;

  @Option(
      names = {"--stream-uri"},
      description = "URI for stream plugin.",
      defaultValue = "rabbitmq-stream://localhost:5552")
  private String streamUri;

  @Option(
      names = {"--amqp-uri"},
      description = "URI for AMQP.",
      defaultValue = "amqp://localhost:5672")
  private String amqpUri;

  @Option(
      names = {"--super-stream"},
      description = "Name of the super stream to publish to.",
      defaultValue = "super-stream")
  private String superStream;

  @Option(
      names = {"--partitions"},
      description = "Number of partitions of the super stream.",
      defaultValue = "3")
  private int partitions;

  @Option(
      names = {"--stream"},
      description = "Name of the stream to consume from.",
      defaultValue = "")
  private String stream;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new SuperStreamApp()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ScheduledExecutorService scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor();
    if ("producer".equalsIgnoreCase(this.mode)) {
      ConnectionFactory cf = new ConnectionFactory();
      cf.setUri(this.amqpUri);
      try (Connection connection = cf.newConnection()) {
        Utils.declareSuperStreamTopology(connection, this.superStream, this.partitions);
      }
      try (Environment environment = Environment.builder().uri(this.streamUri).build()) {
        Producer producer =
            environment.producerBuilder().stream(this.superStream)
                .routing(message -> message.getProperties().getMessageIdAsString())
                .producerBuilder()
                .build();

        AtomicLong messageCount = new AtomicLong();
        Runtime.getRuntime()
            .addShutdownHook(new Thread(() -> scheduledExecutorService.shutdownNow()));
        scheduledExecutorService.scheduleAtFixedRate(
            () -> {
              System.out.println(
                  String.format(Locale.US, "Messages published: %,d", messageCount.get()));
            },
            1,
            1,
            TimeUnit.SECONDS);
        System.out.println("Starting to publish to super stream " + this.superStream + "...");
        while (true) {
          producer.send(
              producer
                  .messageBuilder()
                  .addData("hello".getBytes(StandardCharsets.UTF_8))
                  .properties()
                  .messageId(UUID.randomUUID().toString())
                  .messageBuilder()
                  .build(),
              confirmationStatus -> messageCount.incrementAndGet());
          Thread.sleep(1);
        }
      }
    } else {
      try (Environment environment = Environment.builder().uri(this.streamUri).build()) {
        System.out.println("Starting to consume from " + this.stream + "...");
        AtomicLong messageCount = new AtomicLong();
        environment.consumerBuilder().stream(this.stream)
            .messageHandler(
                (context, message) -> {
                  messageCount.incrementAndGet();
                })
            .build();

        scheduledExecutorService.scheduleAtFixedRate(
            () -> {
              if (messageCount.get() == 0) {
                System.out.println("No messages received.");
              } else {
                System.out.println(
                    String.format(Locale.US, "Messages received: %,d", messageCount.get()));
              }
            },
            1,
            1,
            TimeUnit.SECONDS);

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> latch.countDown()));
        latch.await();
      } finally {
        scheduledExecutorService.shutdownNow();
      }
    }

    return 0;
  }
}
