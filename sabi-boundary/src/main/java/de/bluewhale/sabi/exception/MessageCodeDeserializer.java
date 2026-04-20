/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for the {@link MessageCode} interface.
 *
 * The backend serializes MessageCode enum values as their name string (e.g. "FISH_CREATED").
 * Without this deserializer Jackson would fail because {@link MessageCode} is an abstract type.
 * This deserializer wraps the raw string into a {@link StringMessageCode} so the webclient
 * can process ResultTo responses without having the concrete server-side enum on its classpath.
 *
 * Register via {@code @JsonDeserialize(using = MessageCodeDeserializer.class)} on the field,
 * or globally via a Jackson Module / ObjectMapper configuration.
 *
 * @author schubert
 */
public class MessageCodeDeserializer extends StdDeserializer<MessageCode> {

    public MessageCodeDeserializer() {
        super(MessageCode.class);
    }

    @Override
    public MessageCode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        return new StringMessageCode(value);
    }
}
