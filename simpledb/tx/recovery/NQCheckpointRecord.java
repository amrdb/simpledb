package simpledb.tx.recovery;

import simpledb.file.Page;
import simpledb.log.LogMgr;
import simpledb.tx.Transaction;

import java.util.Arrays;

/**
 * The NQCKPT log record.
 *
 * @author Amr
 */
public class NQCheckpointRecord implements LogRecord {
    private int[] activeTxns;

    /**
     * Create a new non-quiescent checkpoint log record.
     *
     * @param p the page containing the non-quiescent checkpoint log
     */
    public NQCheckpointRecord(Page p) {
        int txnCountPos = Integer.BYTES;
        int txnCount = p.getInt(txnCountPos);
        activeTxns = new int[txnCount];
        for (int i = 0; i < txnCount; i++) {
            txnCountPos += Integer.BYTES;
            activeTxns[i] = p.getInt(txnCountPos);
        }
    }

    public int op() {
        return NQCKPT;
    }

    /**
     * NQCKPT records have no associated transaction,
     * and so the method returns a "dummy", negative txid.
     */
    public int txNumber() {
        return -1; // dummy value
    }

    /**
     * Does nothing, because a checkpoint record
     * contains no undo information.
     */
    public void undo(Transaction tx) {
    }

    public String toString() {
        return "<NQCKPT " + activeTxns.length + " " + Arrays.toString(activeTxns) + " >";
    }

    /**
     * A static method to write a checkpoint record to the log.
     * This log record contains the NQCKPT operator, count of txns and the txns themselves,
     * e.g. <6, 2, 22, 24>
     *
     * @return the LSN of the last log value
     */
    public static int writeToLog(LogMgr lm, int[] activeTxns) {
        int cpos = Integer.BYTES;
        byte[] rec = new byte[2 * Integer.BYTES + activeTxns.length * Integer.BYTES];
        Page p = new Page(rec);
        p.setInt(0, NQCKPT);
        p.setInt(cpos, activeTxns.length);
        for (int i = 0; i < activeTxns.length; i++) {
            cpos += Integer.BYTES;
            p.setInt(cpos, activeTxns[i]);
        }
        return lm.append(rec);
    }

    /**
     * @return activeTxns
     */
    public int[] txList() {
        return activeTxns;
    }
}
