/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.streams.state.internals;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.streams.kstream.internals.WrappingNullableDeserializer;
import org.apache.kafka.streams.processor.internals.SerdeGetter;
import org.apache.kafka.streams.state.VersionedRecord;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import static org.apache.kafka.streams.kstream.internals.WrappingNullableUtils.initNullableDeserializer;

class VersionedRecordDeserializer<V> implements WrappingNullableDeserializer<VersionedRecord<V>, Void, V> {
    private final Deserializer<V> valueDeserializer;
    private final Deserializer<Long> timestampDeserializer;

    VersionedRecordDeserializer(final Deserializer<V> valueDeserializer) {
        Objects.requireNonNull(valueDeserializer);
        this.valueDeserializer = valueDeserializer;
        timestampDeserializer = new LongDeserializer();
    }

    @Override
    public void configure(final Map<String, ?> configs,
                          final boolean isKey) {
        valueDeserializer.configure(configs, isKey);
        timestampDeserializer.configure(configs, isKey);
    }

    @Override
    public VersionedRecord<V> deserialize(final String topic,
                                          final byte[] versionedRecord) {
        if (versionedRecord == null) {
            return null;
        }

        final long validTo = timestampDeserializer.deserialize(topic, rawValidTo(versionedRecord));
        final long timestamp = timestampDeserializer.deserialize(topic, rawTimestamp(versionedRecord));
        final V value = valueDeserializer.deserialize(topic, rawValue(versionedRecord));
        return makeVersionedRecord(value, timestamp, validTo);
    }

    static <V> VersionedRecord<V> makeVersionedRecord(final V value,
                                                      final long timestamp,
                                                      final long validTo) {
        if (value == null) {
            return null;
        } else if (validTo == Long.MAX_VALUE) {
            return new VersionedRecord<>(value, timestamp);
        }
        return new VersionedRecord<>(value, timestamp, validTo);
    }

    @Override
    public void close() {
        valueDeserializer.close();
        timestampDeserializer.close();
    }

    static byte[] rawValue(final byte[] rawValueAndTimestamp) {
        if (rawValueAndTimestamp == null) {
            return null;
        }

        final int rawValueLength = rawValueAndTimestamp.length - 16;
        return ByteBuffer
            .allocate(rawValueLength)
            .put(rawValueAndTimestamp, 16, rawValueLength)
            .array();
    }

    private static byte[] rawTimestamp(final byte[] rawVersionedRecord) {
        return ByteBuffer
            .allocate(8)
            .put(rawVersionedRecord, 8, 8)
            .array();
    }

    private static byte[] rawValidTo(final byte[] rawVersionedRecord) {
        return ByteBuffer
                .allocate(8)
                .put(rawVersionedRecord, 0, 8)
                .array();
    }

    @Override
    public void setIfUnset(final SerdeGetter getter) {
        // ValueAndTimestampDeserializer never wraps a null deserializer (or configure would throw),
        // but it may wrap a deserializer that itself wraps a null deserializer.
        initNullableDeserializer(valueDeserializer, getter);
    }
}
