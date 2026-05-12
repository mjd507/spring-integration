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

package com.springframework.integration.hivemq.event;

import org.springframework.messaging.Message;

/**
 * An event emitted (when using async) when the client indicates that a message
 * has been sent.
 *
 * @author Gary Russell
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public class MqttMessageSentEvent extends MqttMessageDeliveryEvent {

	private final Message<?> message;

	public MqttMessageSentEvent(Object source, Message<?> message, Object mqttPublish) {
		super(source, mqttPublish);
		this.message = message;
	}

	public Message<?> getMessage() {
		return this.message;
	}

}
