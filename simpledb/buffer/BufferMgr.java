package simpledb.buffer;

import simpledb.file.*;
import simpledb.log.LogMgr;

import java.util.*;

/**
 * Manages the pinning and unpinning of buffers to blocks.
 *
 * @author Edward Sciore
 */
public class BufferMgr {
    private Queue<Buffer> unpinned; // LRU
    private Map<BlockId, Buffer> inMemory;
    private static final long MAX_TIME = 10000; // 10 seconds

    /**
     * Creates a buffer manager having the specified number
     * of buffer slots.
     * This constructor depends on a {@link FileMgr} and
     * {@link simpledb.log.LogMgr LogMgr} object.
     *
     * @param numbuffs the number of buffer slots to allocate
     */
    public BufferMgr(FileMgr fm, LogMgr lm, int numbuffs) {
        unpinned = new LinkedList<>();
        inMemory = new HashMap<>();
        for (int i = 0; i < numbuffs; i++) {
            unpinned.add(new Buffer(fm, lm, i));
        }
    }

    /**
     * Returns the number of available (i.e. unpinned) buffers.
     *
     * @return the number of available buffers
     */
    public synchronized int available() {
        return unpinned.size();
    }

    /**
     * Flushes the dirty buffers modified by the specified transaction.
     *
     * @param txnum the transaction's id number
     */
    public synchronized void flushAll(int txnum) {
        for (Buffer buff : unpinned)
            if (buff.modifyingTx() == txnum)
                buff.flush();
    }


    /**
     * Unpins the specified data buffer. If its pin count
     * goes to zero, then notify any waiting threads.
     *
     * @param buff the buffer to be unpinned
     */
    public synchronized void unpin(Buffer buff) {
        buff.unpin();
        if (!buff.isPinned()) {
            unpinned.add(buff);
            notifyAll();
        }
    }

    /**
     * Pins a buffer to the specified block, potentially
     * waiting until a buffer becomes available.
     * If no buffer becomes available within a fixed
     * time period, then a {@link BufferAbortException} is thrown.
     *
     * @param blk a reference to a disk block
     * @return the buffer pinned to that block
     */
    public synchronized Buffer pin(BlockId blk) {
        try {
            long timestamp = System.currentTimeMillis();
            Buffer buff = tryToPin(blk);
            while (buff == null && !waitingTooLong(timestamp)) {
                wait(MAX_TIME);
                buff = tryToPin(blk);
            }
            if (buff == null)
                throw new BufferAbortException();
            return buff;
        } catch (InterruptedException e) {
            throw new BufferAbortException();
        }
    }

    public void printStatus() {
        System.out.println("Buffers and their Contents:");
        for (var e : inMemory.entrySet()) {
            System.out.println("Buffer " + e.getValue().getId() + ": " + e.getKey().toString() + " " + (e.getValue().isPinned() ? "pinned" : "unpinned"));
        }

        System.out.print("Unpinned Buffers in LRU order: ");
        for (Buffer buff : unpinned) {
            System.out.print(buff.getId() + " ");
        }
        System.out.println();
    }

    private boolean waitingTooLong(long starttime) {
        return System.currentTimeMillis() - starttime > MAX_TIME;
    }

    /**
     * Tries to pin a buffer to the specified block.
     * If there is already a buffer assigned to that block
     * then that buffer is used;
     * otherwise, an unpinned buffer from the pool is chosen.
     * Returns a null value if there are no available buffers.
     *
     * @param blk a reference to a disk block
     * @return the pinned buffer
     */
    private Buffer tryToPin(BlockId blk) {
        Buffer buff = findExistingBuffer(blk);
        if (buff == null) {
            buff = chooseUnpinnedBuffer();
            if (buff == null)
                return null;
            inMemory.remove(buff.block());
            buff.assignToBlock(blk);
        }

        buff.pin();
        inMemory.put(blk, buff);
        return buff;
    }

    private Buffer findExistingBuffer(BlockId blk) {
        return inMemory.get(blk);
    }

    private Buffer chooseUnpinnedBuffer() {
        return unpinned.poll();
    }
}
