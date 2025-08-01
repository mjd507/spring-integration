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

package org.springframework.integration.store;

/**
 * A marker interface extension of the {@link MessageGroupStore.MessageGroupCallback}
 * for components which should be registered in the {@link MessageGroupStore} only once.
 * The {@link MessageGroupStore} implementation ensures that only once instance of this
 * class is present in the expire callbacks.
 *
 * @author Meherzad Lahewala
 * @author Artem Bilan
 *
 * @since 5.0.10
 */
@FunctionalInterface
public interface UniqueExpiryCallback extends MessageGroupStore.MessageGroupCallback {

}
