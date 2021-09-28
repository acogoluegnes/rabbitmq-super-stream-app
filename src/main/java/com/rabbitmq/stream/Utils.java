package com.rabbitmq.stream;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

final class Utils {

  private Utils() {}

  static void declareSuperStreamTopology(Connection connection, String superStream, int partitions)
      throws Exception {
    declareSuperStreamTopology(
        connection,
        superStream,
        IntStream.range(0, partitions).mapToObj(String::valueOf).toArray(String[]::new));
  }

  static void declareSuperStreamTopology(Connection connection, String superStream, String... rks)
      throws Exception {
    try (Channel ch = connection.createChannel()) {
      ch.exchangeDeclare(superStream, BuiltinExchangeType.DIRECT, true);

      List<Tuple2<String, Integer>> bindings = new ArrayList<>(rks.length);
      for (int i = 0; i < rks.length; i++) {
        bindings.add(Tuple.of(rks[i], i));
      }
      // shuffle the order to make sure we get in the correct order from the server
      Collections.shuffle(bindings);

      for (Tuple2<String, Integer> binding : bindings) {
        String routingKey = binding._1();
        String partitionName = superStream + "-" + routingKey;
        ch.queueDeclare(
            partitionName, true, false, false, Collections.singletonMap("x-queue-type", "stream"));
        ch.queueBind(
            partitionName,
            superStream,
            routingKey,
            Collections.singletonMap("x-stream-partition-order", binding._2()));
      }
    }
  }
}
