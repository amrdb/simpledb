package simpledb.tx.concurrency;

import java.util.*;

import simpledb.file.BlockId;

/**
 * The lock table, which provides methods to lock and unlock blocks.
 * It uses the wait-die deadlock prevention algorithm.
 * If a transaction requests a XLock that is held by another older transaction,
 * then the requesting transaction will be aborted.
 * Else (newer transaction or SLock), then that transaction is placed on a wait list.
 * There is only one wait list for all blocks.
 * When the last lock on a block is unlocked, then all transactions
 * are removed from the wait list and rescheduled.
 * If one of those transactions discovers that the lock it is waiting for
 * is still locked, it will place itself back on the wait list.
 * <p>
 * Note: txId is the same as txnum in other classes
 *
 * @author Edward Sciore
 */
class LockTable {
    private static final long MAX_TIME = 10000; // 10 seconds

    private Map<BlockId, List<Integer>> locks = new HashMap<>(); // xlock: -1, slock: txId

    /**
     * Grant an SLock on the specified block.
     * If an XLock exists when the method is called,
     * then the calling thread will be placed on a wait list
     * until the lock is released.
     * If the thread remains on the wait list for a certain
     * amount of time (currently 10 seconds),
     * then an exception is thrown.
     *
     * @param blk  a reference to the disk block
     * @param txId the ID of the transaction requesting the lock
     */
    public synchronized void sLock(BlockId blk, int txId) {
        try {
            while (hasXlock(blk)) wait(MAX_TIME);
            if (hasXlock(blk)) throw new LockAbortException();

            getLockTxIds(blk).add(txId);
        } catch (InterruptedException e) {
            throw new LockAbortException();
        }
    }

    /**
     * Grant an XLock on the specified block using the wait-die deadlock prevention algorithm..
     * If a lock granted by an older transaction,
     * then the calling thread will be aborted (die).
     * If an SLock exists when the method is called,
     * then the calling thread will be placed on a wait list
     * until the locks are released.
     * If the thread remains on the wait list for a certain
     * amount of time (currently 10 seconds),
     * then an exception is thrown.
     *
     * @param blk  a reference to the disk block
     * @param txId the ID of the transaction requesting the lock
     */
    synchronized void xLock(BlockId blk, int txId) {
        try {
            boolean shouldDie = getLockTxIds(blk).stream().anyMatch(id -> id < txId);
            if (shouldDie) throw new LockAbortException();

            while (hasOtherSLocks(blk)) wait(MAX_TIME);
            if (hasOtherSLocks(blk)) throw new LockAbortException();

            getLockTxIds(blk).add(-1);
        } catch (InterruptedException e) {
            throw new LockAbortException();
        }
    }

    /**
     * Release a lock on the specified block.
     * If this lock is the last lock on that block,
     * then the waiting transactions are notified.
     *
     * @param blk  a reference to the disk block
     * @param txId the ID of the transaction requesting the unlock
     */
    synchronized void unlock(BlockId blk, int txId) {
        // if there is xlock, remove it
        List<Integer> lockTxIds = getLockTxIds(blk);
        if (lockTxIds.contains(-1)) { // xlock
            locks.remove(blk);
            notifyAll();
        } else { // slock
            if (lockTxIds.size() == 1) {
                locks.remove(blk);
                notifyAll();
            } else {
                lockTxIds.remove((Integer) txId);
            }
        }
    }

    private boolean hasXlock(BlockId blk) {
        return getLockTxIds(blk).contains(-1);
    }

    private boolean hasOtherSLocks(BlockId blk) {
        return getLockTxIds(blk).size() > 1;
    }

    private List<Integer> getLockTxIds(BlockId blk) {
        if (!locks.containsKey(blk)) locks.put(blk, new LinkedList<>());
        return locks.get(blk);
    }
}
