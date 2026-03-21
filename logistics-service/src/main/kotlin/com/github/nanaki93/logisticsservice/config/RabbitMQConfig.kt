package com.github.nanaki93.logisticsservice.config

import io.micrometer.observation.ObservationRegistry
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@ConfigurationProperties(prefix = "rabbitmq")
data class RabbitMQProperties(
    val exchange: String,
    val queue: String,
    val routingKey: String,
    val dlq: String,
)

@Configuration
@EnableConfigurationProperties(RabbitMQProperties::class)
class RabbitMQConfig(
    private val rabbitProps: RabbitMQProperties,
) {
    @Bean
    fun rabbitListenerContainerFactory(
        configurer: SimpleRabbitListenerContainerFactoryConfigurer,
        connectionFactory: ConnectionFactory,
        observationRegistry: ObservationRegistry // Injected by Spring Boot 4
    ): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        configurer.configure(factory, connectionFactory)

        // CRITICAL: This enables the extraction of tracing headers (traceparent)
        // from the RabbitMQ message and starts a new Span linked to it.
        factory.setContainerCustomizer { container ->
            container.setObservationEnabled(true)
        }

        return factory
    }



    @Bean
    fun deadLetterQueue(): Queue = QueueBuilder.durable(rabbitProps.dlq).build()

    @Bean
    fun telemetryEventQueue(): Queue =
        QueueBuilder
            .durable(rabbitProps.queue)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", rabbitProps.routingKey)
            .build()

    @Bean
    fun telemetryEventExchange(): TopicExchange = TopicExchange(rabbitProps.exchange)

    @Bean
    fun telemetryEventBinding(
        telemetryEventQueue: Queue,
        telemetryEventExchange: TopicExchange,
    ): Binding =
        BindingBuilder
            .bind(telemetryEventQueue)
            .to(telemetryEventExchange)
            .with(rabbitProps.routingKey)

    @Bean
    fun messageConverter(objectMapper: JsonMapper): MessageConverter = JacksonJsonMessageConverter(objectMapper)

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: MessageConverter,
    ): RabbitTemplate =
        RabbitTemplate(connectionFactory).apply {
            this.messageConverter = messageConverter
        }
}
