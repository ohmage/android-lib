
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
        if (mElem != null)
            return mElem.isJsonArray();
        return false;
    }

    @Override
    public boolean isJsonObject() {
        if (mElem != null)
            return mElem.isJsonObject();
        return false;
    }

    @Override
    public boolean isJsonPrimitive() {
        if (mElem != null)
            return mElem.isJsonPrimitive();
        return false;
    }

    @Override
    public boolean isJsonNull() {
        if (mElem != null)
            return mElem.isJsonNull();
        return false;
    }

    @Override
    public JsonObject getAsJsonObject() {
        if (mElem != null)
            return mElem.getAsJsonObject();
        return null;
    }

    @Override
    public JsonArray getAsJsonArray() {
        if (mElem != null)
            return mElem.getAsJsonArray();
        return null;
    }

    @Override
    public JsonPrimitive getAsJsonPrimitive() {
        if (mElem != null)
            return mElem.getAsJsonPrimitive();
        return null;
    }

    @Override
    public JsonNull getAsJsonNull() {
        if (mElem != null)
            return mElem.getAsJsonNull();
        return null;
    }

    @Override
    public boolean getAsBoolean() {
        if (mElem != null)
            return mElem.getAsBoolean();
        return false;
    }

    @Override
    public Number getAsNumber() {
        if (mElem != null)
            return mElem.getAsNumber();
        return null;
    }

    @Override
    public String getAsString() {
        if (mElem != null)
            return mElem.getAsString();
        return null;
    }

    @Override
    public double getAsDouble() {
        if (mElem != null)
            return mElem.getAsDouble();
        return 0;
    }

    @Override
    public float getAsFloat() {
        if (mElem != null)
            return mElem.getAsFloat();
        return 0;
    }

    @Override
    public long getAsLong() {
        if (mElem != null)
            return mElem.getAsLong();
        return 0;
    }

    @Override
    public int getAsInt() {
        if (mElem != null)
            return mElem.getAsInt();
        return 0;
    }

    @Override
    public byte getAsByte() {
        if (mElem != null)
            return mElem.getAsByte();
        return 0;
    }

    @Override
    public char getAsCharacter() {
        if (mElem != null)
            return mElem.getAsCharacter();
        return 0;
    }

    @Override
    public BigDecimal getAsBigDecimal() {
        if (mElem != null)
            return mElem.getAsBigDecimal();
        return null;
    }

    @Override
    public BigInteger getAsBigInteger() {
        if (mElem != null)
            return mElem.getAsBigInteger();
        return null;
    }

    @Override
    public short getAsShort() {
        if (mElem != null)
            return mElem.getAsShort();
        return 0;
    }

}
