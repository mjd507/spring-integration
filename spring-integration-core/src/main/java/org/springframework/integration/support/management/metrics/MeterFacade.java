/*
 * Copyright 2018-present the original author or authors.
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

package org.springframework.integration.support.management.metrics;

import org.jspecify.annotations.Nullable;

/**
 * Facade for Meters.
 *
 * @author Gary Russell
 * @since 5.1
 *
 */
public interface MeterFacade {

	/**
	 * Remove this meter facade.
	 * @param <T> the type of meter removed.
	 * @return the facade that was removed, or null.
	 */
	@Nullable
	default <T extends MeterFacade> T remove() {
		return null;
	}

}
