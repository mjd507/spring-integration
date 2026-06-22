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

package org.springframework.integration.config;

import java.beans.Introspector;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.MessageHandler;

/**
 * @author Jiandong Ma
 */
class IntegrationAnnotationsRegistrarTests {

	@Test
	void testEnableIntegrationAnnotations() {
		try (var context = new AnnotationConfigApplicationContext(Config1.class)) {

			assertContainsBean(context, IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME);
			assertContainsBean(context, Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName()));

			assertContainsBean(context, "inputChannel");
			assertContainsBean(context, "customMessageHandler");
			assertContainsBean(context, "customMessageHandler.serviceActivator");
			assertContainsBean(context, "customMessageHandler.serviceActivator.handler");
		}
	}

	@Test
	void testNoEnableIntegrationAnnotations() {
		try (var context = new AnnotationConfigApplicationContext(Config2.class)) {

			assertDoesNotContainsBean(context, IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME);
			assertDoesNotContainsBean(context, Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName()));

			assertDoesNotContainsBean(context, "inputChannel");
			assertContainsBean(context, "customMessageHandler");
			assertDoesNotContainsBean(context, "customMessageHandler.serviceActivator");
			assertDoesNotContainsBean(context, "customMessageHandler.serviceActivator.handler");
		}
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationAnnotations
	static class Config1 {

		@Bean
		@ServiceActivator(inputChannel = "inputChannel")
		MessageHandler customMessageHandler() {
			return message -> {

			};
		}

	}

	@Configuration
	@EnableIntegration
	static class Config2 {

		@Bean
		@ServiceActivator(inputChannel = "inputChannel")
		MessageHandler customMessageHandler() {
			return message -> {

			};
		}

	}

	static void assertContainsBean(ApplicationContext context, String beanName) {
		Assertions.assertThat(context.containsBean(beanName)).isTrue();
	}

	static void assertDoesNotContainsBean(ApplicationContext context, String beanName) {
		Assertions.assertThat(context.containsBean(beanName)).isFalse();
	}

}
