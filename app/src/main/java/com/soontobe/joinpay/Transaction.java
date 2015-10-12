package com.soontobe.joinpay;

import org.json.JSONObject;

import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

/**
 * This class translates the JSONObjects that get returned by the JoinPay APIs into objects.
 * It just helps clean up code by having all the processing done here instead of every file that
 * uses JSONs.
 * Created by Dale Avery on 10/8/2015.
 */
public class Transaction {

    /* Structure of transaction JSON from JoinPay API
	 * [][0]: type
	 *
	 *  type: normal/normal_pn
	 *  	[1] personal note
	 *  	[2] payer
	 *  	[3] payee
	 *  	[4] amount
	 *
	 *  type: summary
	 *  	[1] date (and maybe time)
	 *  	[2] # of ppl
	 *  	[3] total amount
	 *
	 *  type: group_note
	 */

    // Tags for pulling above information out of a JSON
    private static final String KEY_ID = "_id";
    private static final String KEY_REV = "_rev";
    private static final String KEY_TO_USER = "toUser";
    private static final String KEY_TO_ACCOUNT = "toAccount";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_FROM_USER = "fromUser";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_CREATED = "created";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TYPE = "type";
    private static final String KEY_DATE = "created";

    // For checking the status of a transaction
    private static final String DENIED = "DENIED";
    private static final String APPROVED = "APPROVED";
    private static final String PENDING = "PENDING";
    public enum STATUS {
        APPROVED,
        DENIED,
        PENDING,
        UNKNOWN
    }

    // For classifying the type of transaction
    private static final String TYPE_SENDING = "sending";
    public enum TYPE {
        SENDING,
        UNKNOWN
    }

    // All the fields that can be in the transaction JSON
    private final String id;
    private final String rev;
    private final String toUser;
    private final String toAccount;
    private final String description;
    private final String fromUser;
    private final String amount;
    private final long created;
    private final STATUS status;
    private final TYPE type;

    /**
     * Constructs a new Transaction Object from the given transaction JSONObject.
     * @param obj The transaction to process into an object.
     */
    public Transaction(JSONObject obj) {

        // Extract transaction details from JSON
        String fallback = "Not found";  // In case the tag is not found in the json
        id = obj.optString(KEY_ID, fallback).trim();
        rev = obj.optString(KEY_REV, fallback).trim();
        toUser = obj.optString(KEY_TO_USER, fallback).trim();
        toAccount = obj.optString(KEY_TO_ACCOUNT, fallback).trim();
        description = obj.optString(KEY_DESCRIPTION, fallback).trim();
        fromUser = obj.optString(KEY_FROM_USER, fallback).trim();
        amount = obj.optString(KEY_AMOUNT, fallback).trim();
        created = obj.optLong(KEY_CREATED, 0);

        String stat = obj.optString(KEY_STATUS, fallback).trim();
        if (stat.equalsIgnoreCase(DENIED))
            status = STATUS.DENIED;
        else if (stat.equalsIgnoreCase(APPROVED))
            status = STATUS.APPROVED;
        else if (stat.equalsIgnoreCase(PENDING))
            status = STATUS.PENDING;
        else
            status = STATUS.UNKNOWN;

        String t = obj.optString(KEY_TYPE, fallback);
        if (t.equalsIgnoreCase(TYPE_SENDING))
            type = TYPE.SENDING;
        else
            type = TYPE.UNKNOWN;
    }

    /**
     * Constructs a new Transaction object with bogus data.  You shouldn't be using this.
     */
    public Transaction() {
        String nothing = "no json";
        id = nothing;
        rev = nothing;
        toUser = nothing;
        toAccount = nothing;
        description = nothing;
        fromUser = nothing;
        amount = nothing;
        created = 0;
        status = STATUS.UNKNOWN;
        type = TYPE.UNKNOWN;
    }

    @Override
    public String toString() {
        // Just to make Transactions easier to log in other classes.
        String ret = String.format("Transaction: id: %s | rev: %s | toUser: %s | toAcc: %s | desc: %s | " +
                        "fromUser: %s | amount: %s | created: %s | date: %s | status: %s | type: %s",
                id, rev, toUser, toAccount, description, fromUser, amount,
                created, prettyDate(), status, type);
        return ret;
    }

    /**
     * Parses the UTC of the transaction into a Date object.
     * @return A Date representing when the transaction was created.
     */
    public Date prettyDate() {
        return new Date(created);
    }

    /**
     * Creates a Comparator which can be used to sort Transactions by their date.
     * @param ascending True if the Transactions should be sorted in ascending order, false otherwise.
     * @return A Comparator for sorting transactions.
     */
    public static Comparator dateComparator(boolean ascending) {
        if (ascending) return new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                if(lhs instanceof Transaction && rhs instanceof Transaction) {
                    Transaction left = (Transaction) lhs;
                    Transaction right = (Transaction) rhs;
                    return left.prettyDate().compareTo(right.prettyDate());
                } else
                    throw new IllegalArgumentException("Comparator only works on Transactions");
            }
        };
        else return new Comparator() {
                @Override
                public int compare(Object lhs, Object rhs) {
                    if(lhs instanceof Transaction || rhs instanceof Transaction) {
                        Transaction left = (Transaction) lhs;
                        Transaction right = (Transaction) rhs;
                        return right.prettyDate().compareTo(left.prettyDate());
                    } else
                        throw new IllegalArgumentException("Comparator only works on Transactions");
                }
            };
    }

    /**
     * Creates a Comparator that keeps transactions that require user attention at the top
     * of the list, while other transactions are sorted by their date.
     * @param ascending True is dates should be sorted in ascending order, false otherwise
     * @return A Comparator for sorting Transactions.
     */
    public static Comparator finalComparator(final boolean ascending) {
        return new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                // Cast transactions so we can compare them
                if(!(lhs instanceof Transaction) || !(rhs instanceof Transaction))
                    throw new IllegalArgumentException("Comparator only works on Transactions");
                Transaction left = (Transaction) lhs;
                Transaction right = (Transaction) rhs;
                if(isFirst(left) && isFirst(right))
                    return dateComparator(ascending).compare(lhs, rhs);
                else if(isFirst(left))
                    return -1;
                else if(isFirst(right))
                    return 1;
                else
                    return dateComparator(ascending).compare(lhs, rhs);
            }

            // Sending, pending transactions go first
            private boolean isFirst(Transaction t) {
                return (t.getType().equals(TYPE.SENDING) && t.getStatus().equals(STATUS.PENDING));
            }
        };
    }

    /*********************************
     * Getters
     *********************************/

    public String getId() {
        return id;
    }

    public String getRev() {
        return rev;
    }

    public String getToUser() {
        return toUser;
    }

    public String getToAccount() {
        return toAccount;
    }

    public String getDescription() {
        return description;
    }

    public String getFromUser() {
        return fromUser;
    }

    public String getAmount() {
        return amount;
    }

    public long getCreated() {
        return created;
    }

    public STATUS getStatus() {
        return status;
    }

    public TYPE getType() {
        return type;
    }
}

