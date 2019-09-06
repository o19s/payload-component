package com.o19s.payloads.filter;

import com.o19s.payloads.attr.PayloadBufferAttribute;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

/**
 * PayloadBufferFilter
 *
 * A filter to preserve payload compatibility when a WDF is used in the analyzer chain.
 *
 * At the time of writing, some filters (namely the WordDelimiterFilter) will strip payloads
 * if the incoming token is split at all by the filter.  This Filter will copy the payload to a buffer
 * and re-apply it to the first token in the tokenstream.
 *
 * This may be a bug to file in Solr but it's currently undecided what the best behavior is.  This
 * filter is one option but cleaning data of punctuation during indexing is another.
 */
public class PayloadBufferFilter extends TokenFilter {
    private int lastStartOffset = 0;

    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);
    private final PayloadBufferAttribute bufferAtt = addAttribute(PayloadBufferAttribute.class);

    public PayloadBufferFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if(!input.incrementToken()) {
            return false;
        }

        // Force clear the buffer when offsets shift
        if (offsetAtt.startOffset() != lastStartOffset) {
            bufferAtt.forceClear();
            lastStartOffset = offsetAtt.startOffset();
        }

        // If the buffer has data apply it to the payloadAtt if it is empty
        if (bufferAtt.getPayload() != null && payloadAtt.getPayload() == null) {
            payloadAtt.setPayload(BytesRef.deepCopyOf(bufferAtt.getPayload()));
        // Otherwise check if theres a payload, if so, copy it to the buffer
        } else if (payloadAtt.getPayload() != null) {
            bufferAtt.setPayload(BytesRef.deepCopyOf(payloadAtt.getPayload()));
        }

        return true;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        lastStartOffset = 0;
    }
}
