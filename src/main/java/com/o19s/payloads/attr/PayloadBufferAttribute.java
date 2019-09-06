package com.o19s.payloads.attr;

import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.BytesRef;

public interface PayloadBufferAttribute extends Attribute {
    public void forceClear();
    public BytesRef getPayload();
    public void setPayload(BytesRef payload);

    public int getEndOffset();
    public void setEndOffset(int endOffset);
}
