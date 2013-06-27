
package com.google.gson;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonOptionalElement extends JsonElement {

    private final JsonElement mElem;

    public JsonOptionalElement() {
        this(null);
    }

    public JsonOptionalElement(JsonElement elem) {
        mElem = elem;
    }

    @Override
    public boolean isJsonArray() {
        if (!isNull())
            return mElem.isJsonArray();
        return false;
    }

    @Override
    public boolean isJsonObject() {
        if (!isNull())
            return mElem.isJsonObject();
        return false;
    }

    @Override
    public boolean isJsonPrimitive() {
        if (!isNull())
            return mElem.isJsonPrimitive();
        return false;
    }

    @Override
    public boolean isJsonNull() {
        if (!isNull())
            return mElem.isJsonNull();
        return false;
    }

    @Override
    public JsonObject getAsJsonObject() {
        if (!isNull())
            return mElem.getAsJsonObject();
        return null;
    }

    @Override
    public JsonArray getAsJsonArray() {
        if (!isNull())
            return mElem.getAsJsonArray();
        return null;
    }

    @Override
    public JsonPrimitive getAsJsonPrimitive() {
        if (!isNull())
            return mElem.getAsJsonPrimitive();
        return null;
    }

    @Override
    public JsonNull getAsJsonNull() {
        if (!isNull())
            return mElem.getAsJsonNull();
        return null;
    }

    @Override
    public boolean getAsBoolean() {
        if (!isNull())
            return mElem.getAsBoolean();
        return false;
    }

    @Override
    public Number getAsNumber() {
        if (!isNull())
            return mElem.getAsNumber();
        return null;
    }

    @Override
    public String getAsString() {
        if (!isNull())
            return mElem.getAsString();
        return null;
    }

    @Override
    public double getAsDouble() {
        if (!isNull())
            return mElem.getAsDouble();
        return 0;
    }

    @Override
    public float getAsFloat() {
        if (!isNull())
            return mElem.getAsFloat();
        return 0;
    }

    @Override
    public long getAsLong() {
        if (!isNull())
            return mElem.getAsLong();
        return 0;
    }

    @Override
    public int getAsInt() {
        if (!isNull())
            return mElem.getAsInt();
        return 0;
    }

    @Override
    public byte getAsByte() {
        if (!isNull())
            return mElem.getAsByte();
        return 0;
    }

    @Override
    public char getAsCharacter() {
        if (!isNull())
            return mElem.getAsCharacter();
        return 0;
    }

    @Override
    public BigDecimal getAsBigDecimal() {
        if (!isNull())
            return mElem.getAsBigDecimal();
        return null;
    }

    @Override
    public BigInteger getAsBigInteger() {
        if (!isNull())
            return mElem.getAsBigInteger();
        return null;
    }

    @Override
    public short getAsShort() {
        if (!isNull())
            return mElem.getAsShort();
        return 0;
    }

    private boolean isNull() {
        return mElem == null || mElem instanceof JsonNull;
    }

}
