package com.o19s.payloads.attr;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;
import org.apache.lucene.util.BytesRef;

public class PayloadBufferAttributeImpl extends AttributeImpl implements PayloadBufferAttribute, Cloneable {
    private BytesRef payload;
    private int endOffset;

    public PayloadBufferAttributeImpl() {}

    public PayloadBufferAttributeImpl(BytesRef payload) {
        this.payload = payload;
    }

    @Override
    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    @Override
    public BytesRef getPayload() {
        return this.payload;
    }

    @Override
    public void setPayload(BytesRef payload) {
        this.payload = payload;
    }

    @Override
    public void clear() {
        // No-op to maintain data even if other filters call clear
    }

    public void forceClear() {
        // PayloadBufferFilter will call force clear when offsets move
        this.payload = null;
        this.endOffset = 0;
    }

    @Override
    public PayloadBufferAttributeImpl clone() {
        PayloadBufferAttributeImpl clone = (PayloadBufferAttributeImpl) super.clone();
        if (payload != null) {
            clone.payload = BytesRef.deepCopyOf(payload);
            clone.endOffset = endOffset;
        }

        return clone;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other instanceof PayloadBufferAttribute) {
            PayloadBufferAttributeImpl o = (PayloadBufferAttributeImpl) other;
            if (o.payload == null || payload == null) {
                return o.payload == null && payload == null;
            }

            return o.payload.equals(payload);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (payload == null) ? 0 : payload.hashCode() + endOffset;
    }

    @Override
    public void copyTo(AttributeImpl target) {
        PayloadBufferAttribute t = (PayloadBufferAttribute) target;
        t.setPayload((payload == null) ? null : BytesRef.deepCopyOf(payload));
        t.setEndOffset(endOffset);
    }

    @Override
    public void reflectWith(AttributeReflector reflector) {
        reflector.reflect(PayloadBufferAttribute.class, "payload", payload);
        reflector.reflect(PayloadBufferAttribute.class, "endOffset", endOffset);
    }
}
