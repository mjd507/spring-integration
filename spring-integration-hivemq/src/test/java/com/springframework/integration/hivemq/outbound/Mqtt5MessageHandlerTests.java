/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.springframework.integration.hivemq.outbound;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.hivemq.client.internal.mqtt.datatypes.MqttUserPropertyImpl;
import com.hivemq.client.internal.mqtt.message.connect.MqttConnect;
import com.hivemq.client.internal.mqtt.message.disconnect.MqttDisconnect;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.springframework.integration.hivemq.HiveMQContainer;
import com.springframework.integration.hivemq.event.MqttMessageDeliveredEvent;
import com.springframework.integration.hivemq.event.MqttMessageDeliveryEvent;
import com.springframework.integration.hivemq.event.MqttMessageSentEvent;
import com.springframework.integration.hivemq.support.MqttHeaders;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class Mqtt5MessageHandlerTests implements HiveMQContainer {

	static CountDownLatch connectedLatch = new CountDownLatch(1);

	static final String CAR_DEVICE_DEFAULT_TOPIC = "mqtt-v5-outbound-car-default-topic";

	static CountDownLatch petDeviceMessageSentLatch = new CountDownLatch(1);

	static MqttMessageSentEvent petDeviceMessageSentEvent;

	static CountDownLatch petDeviceMessageDeliveredLatch = new CountDownLatch(1);

	static MqttMessageDeliveredEvent petDeviceMessageDeliveredEvent;

	@Autowired
	Mqtt5AsyncClient mqtt5AsyncClient;

	@Autowired
	MessageChannel carDeviceInputChannel;

	@Autowired
	MessageChannel petDeviceInputChannel;

	@BeforeEach
	void beforeEach() throws InterruptedException {
		// ensure connection established first
		Assertions.assertThat(connectedLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
	}

	@Test
	void testCarDeviceMessageHandler_DefaultTopic_BytePayload() throws InterruptedException {
		// Given
		CountDownLatch callbackLatch = new CountDownLatch(1);
		Consumer<Mqtt5Publish> consumer = mqtt5Publish -> {
			// Then
			Assertions.assertThat(mqtt5Publish).isNotNull()
					.satisfies(publish -> {
						Assertions.assertThat(publish.getTopic().toString()).isEqualTo(CAR_DEVICE_DEFAULT_TOPIC);
						Assertions.assertThat(publish.getPayloadAsBytes()).isEqualTo("simple-car-payload".getBytes());
						Assertions.assertThat(publish.getQos()).isEqualTo(MqttQos.AT_MOST_ONCE);
						Assertions.assertThat(publish.getContentType().get().toString()).isEqualTo("plain/text");
						Assertions.assertThat(publish.getCorrelationData().get()).isEqualTo(ByteBuffer.wrap("correlationData".getBytes()));
						Assertions.assertThat(publish.getMessageExpiryInterval().getAsLong()).isEqualTo(30L);
						Assertions.assertThat(publish.getResponseTopic().get().toString()).isEqualTo("response-topic");
						Assertions.assertThat(publish.getPayloadFormatIndicator().get()).isEqualTo(Mqtt5PayloadFormatIndicator.UTF_8);
						Assertions.assertThat(publish.getUserProperties().asList().get(0)).isEqualTo(MqttUserPropertyImpl.of("user_properties_key", "user_properties_value"));
					});
		};
		mqtt5AsyncClient.subscribeWith()
				.topicFilter(CAR_DEVICE_DEFAULT_TOPIC)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt3Publish -> {
					consumer.accept(mqtt3Publish);
					callbackLatch.countDown();
				})
				.send();
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload("simple-car-payload".getBytes())
				.setHeader(MqttHeaders.QOS, MqttQos.AT_MOST_ONCE)
				.setHeader(MqttHeaders.CONTENT_TYPE, "plain/text")
				.setHeader(MqttHeaders.CORRELATION_DATA, "correlationData".getBytes())
				.setHeader(MqttHeaders.MESSAGE_EXPIRY_INTERVAL, 30L)
				.setHeader(MqttHeaders.RESPONSE_TOPIC, "response-topic")
				.setHeader(MqttHeaders.PAYLOAD_FORMAT_INDICATOR, Mqtt5PayloadFormatIndicator.UTF_8)
				.setHeader(MqttHeaders.USER_PROPERTIES, Map.of("user_properties_key", "user_properties_value"))
				.build());
		// Await
		Assertions.assertThat(callbackLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();

	}

	@Test
	void testCarDeviceMessageHandler_HeaderTopic_StringPayload() throws InterruptedException {
		// Given
		String carTopic = "mqtt-v5-outbound-car-header-topic";
		CountDownLatch callbackLatch = new CountDownLatch(1);
		Consumer<Mqtt5Publish> consumer = mqtt5Publish -> {
			// Then
			Assertions.assertThat(mqtt5Publish)
					.isNotNull()
					.satisfies(publish -> {
						Assertions.assertThat(publish.getTopic().toString()).isEqualTo(carTopic);
						Assertions.assertThat(publish.getPayloadAsBytes()).isEqualTo("simple-car-payload".getBytes());
						Assertions.assertThat(publish.getQos()).isEqualTo(MqttQos.AT_LEAST_ONCE);
					});
		};
		mqtt5AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt3Publish -> {
					consumer.accept(mqtt3Publish);
					callbackLatch.countDown();
				})
				.send();
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload("simple-car-payload")
				.setHeader(MqttHeaders.TOPIC, carTopic)
				.build());
		// Await
		Assertions.assertThat(callbackLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
	}

	@Test
	void testCarDeviceMessageHandler_HeaderTopic_ObjectPayload() throws InterruptedException {
		// Given
		String carTopic = "mqtt-v5-outbound-car-header-topic-object-payload";
		record CarObject(String model, String serialNumber) implements Serializable {

		}

		CarObject carObject = new CarObject("Tesla", "1234");
		CountDownLatch callbackLatch = new CountDownLatch(1);
		Consumer<Mqtt5Publish> consumer = mqtt5Publish -> {
			// Then
			Assertions.assertThat(mqtt5Publish)
					.isNotNull()
					.satisfies(publish -> {
						Assertions.assertThat(publish.getTopic().toString()).isEqualTo(carTopic);
						Object deserializedPayload = new DeserializingConverter().convert(publish.getPayloadAsBytes());
						Assertions.assertThat(deserializedPayload)
								.isInstanceOf(CarObject.class)
								.isEqualTo(carObject);
					});
		};
		mqtt5AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt3Publish -> {
					consumer.accept(mqtt3Publish);
					callbackLatch.countDown();
				})
				.send();
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload(carObject)
				.setHeader(MqttHeaders.TOPIC, carTopic)
				.build());
		// Await
		Assertions.assertThat(callbackLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
	}

	@Test
	void testCarDeviceMessageHandler_HeaderTopic_Mqtt5PublishPayload() throws InterruptedException {
		// Given
		String carTopic = "mqtt-v5-outbound-car-header-topic-mqtt5publish-payload";
		Mqtt5Publish mqtt5Payload = Mqtt5Publish.builder()
				.topic(carTopic)
				.payload("simple-car-payload".getBytes())
				.build();
		CountDownLatch callbackLatch = new CountDownLatch(1);
		Consumer<Mqtt5Publish> consumer = mqtt3Publish -> {
			// Then
			Assertions.assertThat(mqtt3Publish)
					.isNotNull()
					.isEqualTo(mqtt5Payload);
		};
		mqtt5AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt3Publish -> {
					consumer.accept(mqtt3Publish);
					callbackLatch.countDown();
				})
				.send();
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload(mqtt5Payload)
				.setHeader(MqttHeaders.TOPIC, carTopic)
				.build());
		// Await
		Assertions.assertThat(callbackLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
	}

	@Test
	void testPetDeviceMessageHandler_AsyncEvent() throws InterruptedException {
		// Given
		String petTopic = "mqtt-v5-outbound-pet-header-topic";
		// When
		petDeviceInputChannel.send(MessageBuilder
				.withPayload("simple-pet-payload")
				.setHeader(MqttHeaders.TOPIC, petTopic)
				.setHeader(MqttHeaders.QOS, MqttQos.AT_LEAST_ONCE)
				.build());
		// Then
		Assertions.assertThat(petDeviceMessageSentLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		Assertions.assertThat(petDeviceMessageDeliveredLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		Assertions.assertThat(petDeviceMessageSentEvent).isNotNull()
				.extracting(MqttMessageSentEvent::getMessage)
				.extracting("payload")
				.isEqualTo("simple-pet-payload");
		Assertions.assertThat(petDeviceMessageDeliveredEvent).isNotNull()
				.extracting(MqttMessageDeliveryEvent::getMqttPublishResult)
				.isNotNull()
				.isInstanceOf(Mqtt5PublishResult.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt5AsyncClient mqtt5AsyncClient() {
			return Mqtt5Client.builder()
					.serverHost(HIVEMQ_CONTAINER.getHost())
					.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort())
					.addConnectedListener(connectedContext -> connectedLatch.countDown())
					.buildAsync();
		}

		@Bean
		DirectChannel carDeviceInputChannel() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "carDeviceInputChannel")
		Mqtt5MessageHandler carDeviceMessageHandler(Mqtt5AsyncClient mqtt5AsyncClient) {
			var messageHandler = new Mqtt5MessageHandler(mqtt5AsyncClient);
			messageHandler.setDefaultTopic(CAR_DEVICE_DEFAULT_TOPIC);
			messageHandler.setDefaultQos(MqttQos.AT_LEAST_ONCE);
			messageHandler.setDefaultRetained(false);
			// for coverage
			messageHandler.setTopicExpressionString("headers['mqtt_topic']");
			messageHandler.setQosExpressionString("headers['mqtt_qos']");
			messageHandler.setRetainedExpressionString("headers['mqtt_retained']");
			messageHandler.setContentTypeExpressionString("headers['mqtt_contentType']");
			messageHandler.setCorrelationDataExpressionString("headers['mqtt_correlationData']");
			messageHandler.setMessageExpiryIntervalExpressionString("headers['mqtt_messageExpiryInterval']");
			messageHandler.setResponseTopicExpressionString("headers['mqtt_responseTopic']");
			messageHandler.setPayloadFormatIndicatorExpressionString("headers['mqtt_payloadFormatIndicator']");
			messageHandler.setUserPropertiesExpressionString("headers['mqtt_userProperties']");
			messageHandler.setMessageConverter(new AbstractMessageConverter() {

				@Override
				protected boolean supports(Class<?> clazz) {
					return true;
				}

				@Override
				protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
					return new SerializingConverter().convert(message.getPayload());
				}
			});
			messageHandler.setSendTimeout(10000);
			return messageHandler;
		}

		@Bean
		DirectChannel petDeviceInputChannel() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "petDeviceInputChannel")
		Mqtt5MessageHandler petDeviceMessageHandler(Mqtt5AsyncClient mqtt5AsyncClient) {
			var messageHandler = new Mqtt5MessageHandler(mqtt5AsyncClient);
			messageHandler.setMqttConnect(MqttConnect.DEFAULT);
			messageHandler.setMqttDisconnect(MqttDisconnect.DEFAULT);
			// for coverage
			messageHandler.setTopicExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.TOPIC)));
			messageHandler.setQosExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.QOS)));
			messageHandler.setRetainedExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.RETAINED)));
			messageHandler.setContentTypeExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.CONTENT_TYPE)));
			messageHandler.setCorrelationDataExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.CORRELATION_DATA)));
			messageHandler.setResponseTopicExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.RESPONSE_TOPIC)));
			messageHandler.setMessageExpiryIntervalExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.MESSAGE_EXPIRY_INTERVAL)));
			messageHandler.setPayloadFormatIndicatorExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.PAYLOAD_FORMAT_INDICATOR)));
			messageHandler.setUserPropertiesExpression(new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.USER_PROPERTIES)));

			messageHandler.setAsync(true);
			messageHandler.setAsyncEvents(true);
			return messageHandler;
		}

		@EventListener
		void petDeviceMqttEvents(MqttMessageDeliveryEvent deliveryEvent) {
			if (deliveryEvent instanceof MqttMessageSentEvent messageSentEvent) {
				petDeviceMessageSentEvent = messageSentEvent;
				petDeviceMessageSentLatch.countDown();
			}
			else if (deliveryEvent instanceof MqttMessageDeliveredEvent messageDeliveredEvent) {
				petDeviceMessageDeliveredEvent = messageDeliveredEvent;
				petDeviceMessageDeliveredLatch.countDown();
			}
		}

	}

}
