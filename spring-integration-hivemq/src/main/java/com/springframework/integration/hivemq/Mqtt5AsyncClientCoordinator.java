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

package com.springframework.integration.hivemq;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect;
import org.jspecify.annotations.Nullable;

/**
 * A {@link ClientCoordinator} implementation for {@link Mqtt5AsyncClient}.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
class Mqtt5AsyncClientCoordinator implements ClientCoordinator<Mqtt5AsyncClient, Mqtt5Connect, Mqtt5Disconnect> {

	Map<Mqtt5AsyncClient, CompletableFuture<Mqtt5ConnAck>> CONNECT_FUTURE_MAP = new ConcurrentHashMap<>();

	Map<Mqtt5AsyncClient, CompletableFuture<Void>> DISCONNECT_FUTURE_MAP = new ConcurrentHashMap<>();

	@Override
	public CompletableFuture<Mqtt5ConnAck> connect(Mqtt5AsyncClient mqttClient, Mqtt5Connect mqttConnect) {
		return this.CONNECT_FUTURE_MAP.computeIfAbsent(mqttClient, client -> {
			// Remove from disconnect map, in case dirty cache between lifecycle methods
			this.DISCONNECT_FUTURE_MAP.remove(client);

			if (mqttClient.getState().isConnected()) { // in case user provides a connected client
				return CompletableFuture.completedFuture(null);
			}
			return client.connect(mqttConnect);
		});
	}

	@Override
	public CompletableFuture<Void> disconnect(Mqtt5AsyncClient mqttClient, @Nullable Mqtt5Disconnect mqttDisconnect) {
		return this.DISCONNECT_FUTURE_MAP.computeIfAbsent(mqttClient, client -> {
			// Remove from connect map,  in case dirty cache between lifecycle methods
			this.CONNECT_FUTURE_MAP.remove(client);

			if (mqttClient.getState() == MqttClientState.DISCONNECTED) {
				return CompletableFuture.completedFuture(null);
			}
			return mqttDisconnect != null ? client.disconnect(mqttDisconnect) : client.disconnect();
		});
	}

}
