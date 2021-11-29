## RabbitMQ Super Stream Application

### Producer Mode

The producer mode creates the super stream topology (using AMQP 0.9.1) and publishes 1,000 messages / second.

```shell
docker run -it --rm --network host pivotalrabbitmq/super-stream-app producer \
  --super-stream invoices --partitions 3 \
  --amqp-uri amqp://guest:guest@localhost:5672/%2f \
  --stream-uri rabbitmq-stream://guest:guest@localhost:5552/%2f
```

If you _don't_ want the producer mode to create the super stream topology, add the `--pre-declared` flag.
This makes the `--partitions` and `--amqp-uri` arguments not necessary:

```shell
docker run -it --rm --network host pivotalrabbitmq/super-stream-app producer \
  --super-stream invoices  \
  --pre-declared \
  --stream-uri rabbitmq-stream://guest:guest@localhost:5552/%2f
```

### Consumer Mode

The consumer mode starts a consumer on an individual stream.

```shell
docker run -it --rm --network host pivotalrabbitmq/super-stream-app consumer \
  --stream invoices-0 \
  --stream-uri rabbitmq-stream://guest:guest@localhost:5552/%2f
```