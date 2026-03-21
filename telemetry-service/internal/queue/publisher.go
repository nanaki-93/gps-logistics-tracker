package queue

import (
	"context"
	"encoding/json"
	"fmt"

	"com.github.nanaki93/telemetry-service/internal/model"
	amqp "github.com/rabbitmq/amqp091-go"
	"go.opentelemetry.io/otel"
)

type Publisher struct {
	conn         *amqp.Connection
	channel      *amqp.Channel
	exchangeName string
	routingKey   string
}

type amqpHeaderCarrier amqp.Table

func (c amqpHeaderCarrier) Get(key string) string {
	v, ok := c[key]
	if !ok {
		return ""
	}
	s, _ := v.(string)
	return s
}

func (c amqpHeaderCarrier) Set(key, val string) {
	c[key] = val
}

func (c amqpHeaderCarrier) Keys() []string {
	keys := make([]string, 0, len(c))
	for k := range c {
		keys = append(keys, k)
	}
	return keys
}

func NewPublisher(url, exchangeName, routingKey string) (*Publisher, error) {
	conn, err := amqp.Dial(url)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to rabbitmq: %v", err)
	}

	channel, err := conn.Channel()
	if err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("failed to open channel: %v", err)
	}
	err = channel.ExchangeDeclare(exchangeName, "topic", true, false, false, false, nil)

	if err != nil {
		_ = channel.Close()
		_ = conn.Close()
		return nil, fmt.Errorf("failed to declare exchange: %v", err)
	}

	fmt.Printf("Connected to RabbitMQ, exchange: %s\n", exchangeName)

	return &Publisher{conn, channel, exchangeName, routingKey}, nil
}

func (p *Publisher) Publish(ctx context.Context, event model.GpsEvent) error {
	body, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal event: %w", err)
	}

	headers := amqp.Table{}
	otel.GetTextMapPropagator().Inject(ctx, amqpHeaderCarrier(headers))

	return p.channel.PublishWithContext(
		ctx,
		p.exchangeName,
		p.routingKey,
		false,
		false,
		amqp.Publishing{
			ContentType:  "application/json",
			DeliveryMode: amqp.Persistent,
			Headers:      headers,
			Body:         body,
		})
}

func (p *Publisher) Close() error {
	if err := p.channel.Close(); err != nil {
		return fmt.Errorf("failed to close channel: %v", err)
	}
	if err := p.conn.Close(); err != nil {
		return fmt.Errorf("failed to close connection: %v", err)
	}
	return nil
}
