package com.o19s.payloads;

import org.apache.lucene.analysis.payloads.AbstractEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.util.BytesRef;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Base64;

/**
 * This encoder will accept Base64 data but store it out as a decoded UTF8 string.  This saves space
 * and makes it simpler to use at query time.
 *
 * This may be handy if you run into issues getting your payload data thru your analysis chain.  Whitespace
 * and formatting can sometimes chunk up your payload so wrapping it inside of Base64 is one possible work
 * around.
 */
public class Base64Encoder  extends AbstractEncoder implements PayloadEncoder {
    private Charset charset = StandardCharsets.UTF_8;

    public Base64Encoder() {
    }

    public Base64Encoder(Charset charset) {
        this.charset = charset;
    }

    @Override
    public BytesRef encode(char[] buffer, int offset, int length) {
        final ByteBuffer bb = charset.encode(CharBuffer.wrap(buffer, offset, length));

        BytesRef encoded = new BytesRef(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());

        try {
            String decoded = new String(Base64.getMimeDecoder().decode(encoded.bytes), charset);
            return new BytesRef(decoded.getBytes());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unable to decode Base64 payload.  This can occur if the payload is malformed or if the content included the delimiter by mistake.");
        }
    }
}
