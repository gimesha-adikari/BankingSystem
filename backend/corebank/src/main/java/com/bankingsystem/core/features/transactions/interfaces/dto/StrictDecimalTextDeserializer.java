package com.bankingsystem.core.features.transactions.interfaces.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public final class StrictDecimalTextDeserializer extends JsonDeserializer<String> {
    @Override public String deserialize(JsonParser p, DeserializationContext c) throws IOException {
        if (p.currentToken() != JsonToken.VALUE_STRING) {
            return (String) c.handleUnexpectedToken(String.class, p);
        }
        return p.getText();
    }
}
