package org.apache.dubbo.spring.security.oauth2;

import org.apache.dubbo.spring.security.oauth2.UnmodifiableCollectionMixin.UnmodifiableCollectionConverter;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

@JsonDeserialize(converter = UnmodifiableCollectionConverter.class)
abstract class UnmodifiableCollectionMixin {

    public static class UnmodifiableCollectionConverter extends StdConverter<Collection<Object>, Collection<Object>> {

        @Override
        public Collection<Object> convert(Collection<Object> value) {
            return Collections.unmodifiableCollection(value);
        }
    }
}
