package com.o19s.payloads.attr;

import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.BytesRef;

public interface PayloadBufferAttribute extends Attribute {
    public BytesRef getPayload();
    public void setPayload(BytesRef payload);
}
