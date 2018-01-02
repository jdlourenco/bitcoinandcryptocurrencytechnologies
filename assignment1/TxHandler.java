import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        int txInIdx = 0;
        for(Transaction.Input txIn : tx.getInputs()) {
            if(!inputInPoll(txIn) || !txInSignatureValid(tx, txIn, txInIdx)) {
                return false;
            }
            txInIdx++;
        }

        if(utxoMultiClaim(tx)) {
            return false;
        }

        for(Transaction.Output txOut : tx.getOutputs()) {
            if(!txOutNonNegative(txOut)) {
                return false;
            }
        }

        if(!inputsGTEOutputs(tx)) {
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {        
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();

        for(Transaction tx : possibleTxs) {
            if(isValidTx(tx)) {
                acceptedTxs.add(tx);

                for(Transaction.Input txIn : tx.getInputs()) {
                    UTXO txInUTXO = getTxInUTXO(txIn);
                    this.utxoPool.removeUTXO(txInUTXO);
                }

                int outIdx = 0;
                for(Transaction.Output txOut : tx.getOutputs()) {
                    UTXO txOutUTXO = new UTXO(tx.getHash(), outIdx);
                    this.utxoPool.addUTXO(txOutUTXO, txOut);
                    outIdx++;
                }
            }
        }

        Transaction[] acceptedTxsArray = new Transaction[acceptedTxs.size()];
        int i = 0;
        for(Transaction tx : acceptedTxs) {
            acceptedTxsArray[i] = tx;
            i++;
        }
        return acceptedTxsArray;
    }

    private boolean inputInPoll(Transaction.Input txIn) {
        if(getTxInUTXO(txIn) == null) {
            return false;
        } else {
            return true;
        }
    }

    private UTXO getTxInUTXO(Transaction.Input txIn) {
        for(UTXO utxo : this.utxoPool.getAllUTXO()) {
            if(Arrays.equals(utxo.getTxHash(), txIn.prevTxHash) && utxo.getIndex() == txIn.outputIndex) {
                return utxo;
            }
        }

        return null;
    }

    private boolean utxoMultiClaim(Transaction tx) {
        HashSet<UTXO> utxoSet = new HashSet<UTXO>();

        for(Transaction.Input txIn : tx.getInputs()) {
            UTXO txInUTXO = getTxInUTXO(txIn);
            if(utxoSet.contains(txInUTXO)) {
                return true;
            } else {
                utxoSet.add(txInUTXO);
            }
        }

        return false;
    }

    private boolean txInSignatureValid(Transaction tx, Transaction.Input txIn, int txInIdx) {
        UTXO txInUTXO = getTxInUTXO(txIn);
        if(txInUTXO == null) {
            return false;
        }

        Transaction.Output txOut = this.utxoPool.getTxOutput(txInUTXO);
        if(txOut == null) {
            return false;
        }

        return Crypto.verifySignature(txOut.address, tx.getRawDataToSign(txInIdx), txIn.signature);
    }

    private boolean txOutNonNegative(Transaction.Output txOut) {
        return txOut.value >= 0;
    }

    private boolean inputsGTEOutputs(Transaction tx) {
        double inputSum  = getTxInSum(tx);
        double outputSum = getTxOutSum(tx);
        
        return inputSum >= outputSum;
    }

    private double getTxInSum(Transaction tx) {
        double inputSum  = 0.0;

        for(Transaction.Input txIn : tx.getInputs()) {
            UTXO txInUTXO = getTxInUTXO(txIn);
            Transaction.Output txOut = this.utxoPool.getTxOutput(txInUTXO);
            inputSum += txOut.value;
        }

        return inputSum;
    }

    private double getTxOutSum(Transaction tx) {
        double outputSum  = 0.0;

        for(Transaction.Output txOut : tx.getOutputs()) {
            outputSum += txOut.value;
        }

        return outputSum;
    }
}
