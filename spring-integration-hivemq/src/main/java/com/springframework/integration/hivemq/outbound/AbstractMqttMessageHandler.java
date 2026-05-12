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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.springframework.integration.hivemq.event.MqttMessageDeliveredEvent;
import com.springframework.integration.hivemq.event.MqttMessageNotDeliveredEvent;
import com.springframework.integration.hivemq.event.MqttMessageSentEvent;
import com.springframework.integration.hivemq.support.MqttHeaders;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.management.ManageableSmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT Outbound Adapters.
 *
 * @param <T> MQTT Client type
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public abstract class AbstractMqttMessageHandler<T extends MqttClient> extends AbstractMessageHandler
		implements ManageableSmartLifecycle, ApplicationEventPublisherAware {

	private static final long DEFAULT_SEND_TIMEOUT = 30_000L;

	private final AtomicBoolean running = new AtomicBoolean();

	@SuppressWarnings("NullAway.Init")
	protected ApplicationEventPublisher applicationEventPublisher;

	@SuppressWarnings("NullAway.Init")
	protected MessageConverter messageConverter;

	protected T mqttClient;

	protected MessageProcessor<String> topicProcessor = message
			-> message.getHeaders().get(MqttHeaders.TOPIC, String.class);

	protected @Nullable String defaultTopic;

	protected MessageProcessor<MqttQos> qosProcessor = message
			-> message.getHeaders().get(MqttHeaders.QOS, MqttQos.class);

	protected @Nullable MqttQos defaultQos;

	protected MessageProcessor<Boolean> retainedProcessor = message
			-> message.getHeaders().get(MqttHeaders.RETAINED, Boolean.class);

	protected @Nullable Boolean defaultRetained;

	protected boolean async;

	protected boolean asyncEvents;

	protected long sendTimeout = DEFAULT_SEND_TIMEOUT;

	protected AbstractMqttMessageHandler(T mqttClient) {
		this.mqttClient = mqttClient;
		Assert.notNull(mqttClient, "'mqttClient' cannot be null");

		if (mqttClient.getConfig().getAutomaticReconnect().isEmpty()) {
			logger.warn("it is recommended to enable 'automaticReconnect' when set this `mqttClient`. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.messageConverter == null) {
			String messageConverterBeanName = IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME;
			setMessageConverter(getBeanFactory().getBean(messageConverterBeanName, SmartMessageConverter.class));
		}
	}

	/**
	 * Set the message converter to use; if this is provided, the adapter qos and retained
	 * settings are ignored.
	 * @param messageConverter the messageConverter.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'converter' cannot be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * Set to true if you don't want to block when sending messages. Default false.
	 * When true, message sent/delivered events will be published for reception
	 * by a suitably configured 'ApplicationListener' or an event
	 * inbound-channel-adapter.
	 * @param async true for async.
	 * @see #setAsyncEvents(boolean)
	 */
	public void setAsync(boolean async) {
		this.async = async;
	}

	/**
	 * When {@link #setAsync(boolean)} is true, setting this to true enables
	 * publication of {@link MqttMessageSentEvent} and {@link MqttMessageDeliveredEvent}
	 * to be emitted. Default false.
	 * @param asyncEvents the asyncEvents.
	 */
	public void setAsyncEvents(boolean asyncEvents) {
		this.asyncEvents = asyncEvents;
	}

	/**
	 * Set the send timeout. used when {@link #async} is false.
	 * Default {@value #DEFAULT_SEND_TIMEOUT} milliseconds.
	 * @param sendTimeout The sendTimeout.
	 */
	public void setSendTimeout(long sendTimeout) {
		Assert.isTrue(sendTimeout > 0, "'sendTimeout' must be greater than 0");
		this.sendTimeout = sendTimeout;
	}

	/**
	 * Set the topic to which the message will be published if the
	 * {@link #setTopicExpression(Expression) topicExpression} evaluates to `null`.
	 * @param defaultTopic the default topic.
	 */
	public void setDefaultTopic(String defaultTopic) {
		Assert.hasText(defaultTopic, "'defaultTopic' must not be empty");
		this.defaultTopic = defaultTopic;
	}

	/**
	 * Set the topic expression; default "headers['mqtt_topic']".
	 * @param topicExpression the expression.
	 */
	public void setTopicExpression(Expression topicExpression) {
		Assert.notNull(topicExpression, "'topicExpression' cannot be null");
		this.topicProcessor = new ExpressionEvaluatingMessageProcessor<>(topicExpression);
	}

	/**
	 * Set the topic expression; default "headers['mqtt_topic']".
	 * @param topicExpression the expression.
	 */
	public void setTopicExpressionString(String topicExpression) {
		Assert.hasText(topicExpression, "'topicExpression' must not be null or empty");
		this.topicProcessor = new ExpressionEvaluatingMessageProcessor<>(topicExpression);
	}

	/**
	 * Set the qos for messages if the {@link #setQosExpression(Expression) qosExpression} evaluates to null.
	 * @param defaultQos the default qos.
	 */
	public void setDefaultQos(MqttQos defaultQos) {
		Assert.notNull(defaultQos, "'defaultQos' cannot be null");
		this.defaultQos = defaultQos;
	}

	/**
	 * Set the qos expression; default "headers['mqtt_qos']".
	 * @param qosExpression the expression.
	 */
	public void setQosExpression(Expression qosExpression) {
		Assert.notNull(qosExpression, "'qosExpression' cannot be null");
		this.qosProcessor = new ExpressionEvaluatingMessageProcessor<>(qosExpression);
	}

	/**
	 * Set the qos expression; default "headers['mqtt_qos']".
	 * @param qosExpression the expression.
	 */
	public void setQosExpressionString(String qosExpression) {
		Assert.hasText(qosExpression, "'qosExpression' must not be null or empty");
		this.qosProcessor = new ExpressionEvaluatingMessageProcessor<>(qosExpression);
	}

	/**
	 * Set the retained boolean for messages if the {@link #setRetainedExpression(Expression) retainedExpression}
	 * evaluates to null.
	 * @param defaultRetained the defaultRetained.
	 */
	public void setDefaultRetained(boolean defaultRetained) {
		this.defaultRetained = defaultRetained;
	}

	/**
	 * Set the retained expression; default "headers['mqtt_retained']".
	 * @param retainedExpression the expression.
	 */
	public void setRetainedExpression(Expression retainedExpression) {
		Assert.notNull(retainedExpression, "'qosExpression' cannot be null");
		this.retainedProcessor = new ExpressionEvaluatingMessageProcessor<>(retainedExpression);
	}

	/**
	 * Set the retained expression; default "headers['mqtt_retained']".
	 * @param retainedExpression the expression.
	 */
	public void setRetainedExpressionString(String retainedExpression) {
		Assert.hasText(retainedExpression, "'qosExpression' must not be null or empty");
		this.retainedProcessor = new ExpressionEvaluatingMessageProcessor<>(retainedExpression);
	}

	@Override
	public void start() {
		if (!this.running.getAndSet(true)) {
			doStart();
		}
	}

	protected abstract void doStart();

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object messageForPublish;
		if (shouldBuildMqttMessage(message.getPayload())) {
			String topic = this.topicProcessor.processMessage(message);
			if (topic == null) {
				topic = this.defaultTopic;
			}
			Assert.state(topic != null, "No topic could be determined from the message and no default topic defined");

			MqttQos qos = this.qosProcessor.processMessage(message);
			qos = qos == null ? this.defaultQos : qos;

			Boolean retained = this.retainedProcessor.processMessage(message);
			retained = retained == null ? this.defaultRetained : retained;

			messageForPublish = doBuildMqttMessage(topic, getPayloadAsBytes(message), qos, retained, message);
		}
		else {
			messageForPublish = message.getPayload();
		}

		publishMqttMessage(messageForPublish, message);
	}

	private byte @NonNull [] getPayloadAsBytes(Message<?> message) {
		Object payload = message.getPayload();
		byte[] body;
		if (payload instanceof byte[] p) {
			body = p;
		}
		else if (payload instanceof String p) {
			body = p.getBytes(StandardCharsets.UTF_8);
		}
		else {
			body = (byte[]) this.messageConverter.fromMessage(message, byte[].class);
			Assert.state(body != null,
					() -> "The MQTT payload cannot be null. The '" + this.messageConverter + "' returned null for: " + message);
		}
		return body;
	}

	protected abstract boolean shouldBuildMqttMessage(Object payload);

	protected abstract Object doBuildMqttMessage(String topic, byte[] body, @Nullable MqttQos qos,
			@Nullable Boolean retained, Message<?> message);

	protected abstract void publishMqttMessage(Object messageForPublish, Message<?> message);

	@Override
	public void stop() {
		if (this.running.getAndSet(false)) {
			doStop();
		}
	}

	protected abstract void doStop();

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	protected void messageSentEvent(Message<?> message, Object mqttPublish) {
		if (this.async && this.asyncEvents) {
			this.applicationEventPublisher.publishEvent(new MqttMessageSentEvent(this, message, mqttPublish));
		}
	}

	protected void sendDeliveryCompleteEvent(Object mqttPublishResult) {
		if (this.async && this.asyncEvents) {
			this.applicationEventPublisher.publishEvent(new MqttMessageDeliveredEvent(this, mqttPublishResult));
		}
	}

	protected void sendFailedDeliveryEvent(Object mqttPublishResult, Throwable exception) {
		if (this.async && this.asyncEvents) {
			this.applicationEventPublisher.publishEvent(
					new MqttMessageNotDeliveredEvent(this, mqttPublishResult, exception));
		}
	}

	@Override
	public String getComponentType() {
		return "mqtt:outbound-channel-adapter";
	}

}
