/*
 * Copyright 2011-present the original author or authors.
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

package org.springframework.integration.test.support;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * Defines a Spring Integration request response test scenario. All setter methods may
 * be chained.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 */
public class RequestResponseScenario {

	private final String inputChannelName;

	private final String outputChannelName;

	private @Nullable Object payload;

	private @Nullable Message<?> message;

	@SuppressWarnings("NullAway.Init")
	private AbstractResponseValidator<?> responseValidator;

	private @Nullable String name;

	protected Message<?> getMessage() {
		if (this.message == null) {
			return new GenericMessage<>(Objects.requireNonNull(this.payload));
		}
		else {
			return this.message;
		}
	}

	/**
	 * Create an instance
	 * @param inputChannelName the input channel name
	 * @param outputChannelName the output channel name
	 */
	public RequestResponseScenario(String inputChannelName, String outputChannelName) {
		this.inputChannelName = inputChannelName;
		this.outputChannelName = outputChannelName;
	}

	/**
	 *
	 * @return the input channel name
	 */
	public String getInputChannelName() {
		return this.inputChannelName;
	}

	/**
	 *
	 * @return the output channel name
	 */
	public String getOutputChannelName() {
		return this.outputChannelName;
	}

	/**
	 *
	 * @return the request message payload
	 */
	public @Nullable Object getPayload() {
		return this.payload;
	}

	/**
	 * set the payload of the request message
	 * @param payload The payload.
	 * @return this
	 */
	public RequestResponseScenario setPayload(Object payload) {
		this.payload = payload;
		return this;
	}

	/**
	 *
	 * @return the scenario name
	 */
	public @Nullable String getName() {
		return this.name;
	}

	/**
	 * Set the scenario name (optional)
	 * @param name the name
	 * @return this
	 */
	public RequestResponseScenario setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 *
	 * @return the response validator
	 * @see AbstractResponseValidator
	 */
	public AbstractResponseValidator<?> getResponseValidator() {
		return this.responseValidator;
	}

	/**
	 * Set the response validator
	 * @param responseValidator The response validator.
	 * @return this
	 * @see AbstractResponseValidator
	 */
	public RequestResponseScenario setResponseValidator(AbstractResponseValidator<?> responseValidator) {
		this.responseValidator = responseValidator;
		return this;
	}

	/**
	 * Set the request message (as an alternative to setPayload())
	 * @param message The message.
	 * @return this
	 */
	public RequestResponseScenario setMessage(Message<?> message) {
		this.message = message;
		return this;
	}

	protected void init() {
		Assert.state(this.message == null || this.payload == null, "cannot set both message and payload");
		Assert.state(this.responseValidator != null,
				"A 'responseValidator' must be provided for the 'SubscribableChannel' scenario.");

	}

}
