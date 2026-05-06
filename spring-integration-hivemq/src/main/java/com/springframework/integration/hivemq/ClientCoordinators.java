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

import com.hivemq.client.internal.mqtt.message.connect.mqtt3.Mqtt3ConnectView;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect;

/**
 * The ClientCoordinators for getting the particular {@link ClientCoordinator}.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public final class ClientCoordinators {

	private static final Mqtt5AsyncClientCoordinator MQTT5_ASYNC_CLIENT_COORDINATOR = new Mqtt5AsyncClientCoordinator();

	private static final Mqtt3AsyncClientCoordinator MQTT3_ASYNC_CLIENT_COORDINATOR = new Mqtt3AsyncClientCoordinator();

	public static ClientCoordinator<Mqtt5AsyncClient, Mqtt5Connect, Mqtt5Disconnect> mqtt5AsyncClient() {
		return MQTT5_ASYNC_CLIENT_COORDINATOR;
	}

	public static ClientCoordinator<Mqtt3AsyncClient, Mqtt3ConnectView, Object> mqtt3AsyncClient() {
		return MQTT3_ASYNC_CLIENT_COORDINATOR;
	}

	private ClientCoordinators() {
	}

}
