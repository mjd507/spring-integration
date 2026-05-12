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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.hivemq.client.internal.mqtt.message.connect.mqtt3.Mqtt3ConnectView;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.springframework.integration.hivemq.MqttClientConnectionCoordinators;
import com.springframework.integration.hivemq.event.MqttConnectionFailedEvent;
import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMqttMessageHandler} implementation for MQTT v3.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt3MessageHandler extends AbstractMqttMessageHandler<Mqtt3AsyncClient> {

	private Mqtt3ConnectView mqtt3ConnectView = Mqtt3ConnectView.DEFAULT;

	public Mqtt3MessageHandler(Mqtt3AsyncClient mqttClient) {
		super(mqttClient);
	}

	/**
	 * Set the Connect message.
	 * @param mqtt3ConnectView the mqtt3ConnectView
	 */
	public void setMqttConnectView(Mqtt3ConnectView mqtt3ConnectView) {
		Assert.notNull(mqtt3ConnectView, "'mqtt3ConnectView' must not be null.");
		this.mqtt3ConnectView = mqtt3ConnectView;
	}

	@Override
	protected void doStart() {
		MqttClientConnectionCoordinators.mqtt3AsyncClient().connect(mqttClient, this.mqtt3ConnectView)
				.whenComplete((connAck, throwable) -> {
					if (throwable != null) {
						MqttConnectionFailedEvent event = new MqttConnectionFailedEvent(this, throwable);
						applicationEventPublisher.publishEvent(event);
						logger.error(throwable, "MQTT client failed to connect. " + throwable.getMessage());
					}
				});
	}

	@Override
	protected void doStop() {
		MqttClientConnectionCoordinators.mqtt3AsyncClient().disconnect(mqttClient, null)
				.whenComplete((unused, throwable) -> {
					if (throwable != null) {
						logger.error(throwable, "MQTT client failed to disconnect. " + throwable.getMessage());
					}
				});
	}

	@Override
	protected boolean shouldBuildMqttMessage(Object payload) {
		return !(payload instanceof Mqtt3Publish);
	}

	@Override
	protected Object doBuildMqttMessage(String topic, byte[] body, @Nullable MqttQos qos, @Nullable Boolean retained, Message<?> message) {

		var builder = Mqtt3Publish.builder()
				.topic(topic)
				.payload(body);

		if (qos != null) {
			builder = builder.qos(qos);
		}

		if (retained != null) {
			builder = builder.retain(retained);
		}

		return builder.build();
	}

	@Override
	protected void publishMqttMessage(Object messageForPublish, Message<?> message) {
		Mqtt3Publish mqtt3Publish = (Mqtt3Publish) messageForPublish;
		var publishFuture = mqttClient.publish(mqtt3Publish);
		if (async) {
			messageSentEvent(message, mqtt3Publish);
			publishFuture.whenComplete((mqtt3PublishResult, throwable) -> {
				if (throwable != null) {
					sendFailedDeliveryEvent(mqtt3PublishResult, throwable);
				}
				else {
					sendDeliveryCompleteEvent(mqtt3PublishResult);
				}
			});
		}
		else {
			try {
				publishFuture.get(sendTimeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException | ExecutionException | TimeoutException ex) {
				throw new MessageHandlingException(message, "Failed to publish to MQTT in the [" + this + ']', ex);
			}
		}
	}

}
