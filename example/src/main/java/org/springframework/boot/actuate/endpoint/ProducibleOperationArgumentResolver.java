/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * An {@link OperationArgumentResolver} for {@link Producible producible enums}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class ProducibleOperationArgumentResolver implements OperationArgumentResolver {

	private final Supplier<List<String>> accepts;

	/**
	 * Create a new {@link ProducibleOperationArgumentResolver} instance.
	 * @param accepts supplier that returns accepted mime types
	 */
	public ProducibleOperationArgumentResolver(Supplier<List<String>> accepts) {
		this.accepts = accepts;
	}

	@Override
	public boolean canResolve(Class<?> type) {
		return Producible.class.isAssignableFrom(type) && Enum.class.isAssignableFrom(type);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T resolve(Class<T> type) {
		return (T) resolveProducible((Class<Enum<? extends Producible<?>>>) type);
	}

	private Enum<? extends Producible<?>> resolveProducible(Class<Enum<? extends Producible<?>>> type) {
		List<String> accepts = this.accepts.get();
		List<Enum<? extends Producible<?>>> values = Arrays.asList(type.getEnumConstants());

		Collections.reverse(values);

		System.out.println("[Producible Argument Resolver] Request Accepts: " + accepts );
		System.out.println("[Producible Argument Resolver] Resolve Type Values: " + values );

		if (CollectionUtils.isEmpty(accepts)) {
			return values.get(0);
		}
		Enum<? extends Producible<?>> result = null;
		for (String accept : accepts) {
			List<String> mimes = MimeTypeUtils.tokenize(accept);
			Collections.reverse(mimes); // TODO: should be ordered following `q` probability

			for (String mimeType : mimes) {
				result = mostRecent(result, forType(values, MimeTypeUtils.parseMimeType(mimeType)));
			}
		}

		System.out.println("[Producible Argument Resolver] Resolve Result: " + ((result!=null) ? result.name() : "null") );

		return result;
	}

	private static Enum<? extends Producible<?>> mostRecent(Enum<? extends Producible<?>> existing,
			Enum<? extends Producible<?>> candidate) {

		if ( ((existing!=null) ? existing.name(): null) == ((candidate!=null) ? candidate.name() : null ) || (existing==null && candidate!=null)
		|| (candidate==null && existing!=null)) {
			int existingOrdinal = (existing != null) ? existing.ordinal() : -1;
			int candidateOrdinal = (candidate != null) ? candidate.ordinal() : -1;

			System.out.println("[ProducibleOperationArgumentResolver] existing " + ((existing != null) ? existing.name() : null)
					+ " candidate " + ((candidate != null) ? candidate.name() : null)
					+ " mostRecent " + ((candidateOrdinal > existingOrdinal) ? ((candidate!=null) ? candidate.name():null) : ((existing!=null) ? existing.name():null)));

			return (candidateOrdinal > existingOrdinal) ? candidate : existing;
		} else {
			System.out.println("[ProducibleOperationArgumentResolver] existing " + ((existing != null) ? existing.name() : null)
							+ " candidate " + ((candidate != null) ? candidate.name() : null) +
							" mostRecent " + ((candidate != null) ? candidate.name() : null));

			return candidate;
		}
	}

	private static Enum<? extends Producible<?>> forType(List<Enum<? extends Producible<?>>> candidates,
			MimeType mimeType) {
		for (Enum<? extends Producible<?>> candidate : candidates) {
			if (mimeType.isCompatibleWith(((Producible<?>) candidate).getProducedMimeType())) {
				System.out.println("[ProducibleOperationArgumentResolver] for MimeType "
						+ mimeType.getType()
						+ " candidates [" + candidates.toString()
						+ "] candidate=" + candidate);
				return candidate;
			}
		}
		System.out.println("[ProducibleOperationArgumentResolver] for MimeType "
				+ mimeType.getType() + " null");
		return null;
	}

}
