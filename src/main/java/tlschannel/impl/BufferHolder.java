package tlschannel.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tlschannel.BufferAllocator;

import java.nio.ByteBuffer;
import java.util.Optional;

public class BufferHolder {

    private static final Logger logger = LoggerFactory.getLogger(BufferHolder.class);
    private final static byte[] zeros = new byte[TlsChannelImpl.maxTlsPacketSize];

    public final String name;
    public final BufferAllocator allocator;
    public final boolean plainData;
    public final int maxSize;

    public ByteBuffer buffer;

    public BufferHolder(String name, Optional<ByteBuffer> buffer, BufferAllocator allocator, int initialSize, int maxSize, boolean plainData) {
        this.name = name;
        this.allocator = allocator;
        this.buffer = buffer.orElseGet(() -> allocator.allocate(initialSize));
        this.maxSize = maxSize;
        this.plainData = plainData;
    }

    public void dispose() {
        allocator.free(buffer);
    }

    public void resize(int newCapacity) {
        if (newCapacity > maxSize)
            throw new IllegalArgumentException(String.format("new capacity (%s) bigger than absolute max size (%s)", newCapacity, maxSize));
        logger.trace("resizing buffer {}, increasing from {} to {} (manual sizing)", name, buffer.capacity(), newCapacity);
        resizeImpl(newCapacity);
    }

    public void enlarge() {
        if (buffer.capacity() >= maxSize) {
            throw new IllegalStateException(
                    String.format("%s buffer insufficient despite having capacity of %d", name, buffer.capacity()));
        }
        int newCapacity = Math.min(buffer.capacity() * 2, maxSize);
        logger.trace("enlarging buffer {}, increasing from {} to {} (automatic enlarge)", name, buffer.capacity(), newCapacity);
        resizeImpl(newCapacity);
    }

    private void resizeImpl(int newCapacity) {
        ByteBuffer newBuffer = allocator.allocate(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        if (plainData) {
            zero();
        }
        allocator.free(buffer);
        buffer = newBuffer;
    }

    /**
     * Fill with zeros the remaining of the supplied buffer. This method does
     * not change the buffer position.
     *
     * Typically used for security reasons, with buffers that contains
     * now-unused plaintext.
     */
    public void zeroRemaining() {
        buffer.mark();
        buffer.put(zeros, 0, buffer.remaining());
        buffer.reset();
    }

    /**
     * Fill the buffer with zeros. This method does not change the buffer position.
     *
     * Typically used for security reasons, with buffers that contains
     * now-unused plaintext.
     */
    public void zero() {
        buffer.mark();
        buffer.position(0);
        buffer.put(zeros, 0, buffer.remaining());
        buffer.reset();
    }

}
