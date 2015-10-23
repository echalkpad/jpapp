package com.soontobe.joinpay.model;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * This class translates the JSONObjects that get returned by the JoinPay
 * APIs into objects. It just helps clean up code by having all the
 * processing done here instead of every file that uses JSONs.
 * <p/>
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

    /**
     * Key for the transaction ID.
     */
    private static final String KEY_ID = "_id";

    /**
     * Key for the revision number of the cloudant document storing
     * the transaction.
     */
    private static final String KEY_REV = "_rev";

    /**
     * Key for the user to whom the transaction is sent.
     */
    private static final String KEY_TO_USER = "toUser";

    /**
     * Key for the account number on which the transaction is applied.
     */
    private static final String KEY_TO_ACCOUNT = "toAccount";

    /**
     * Key for the description of the transaction.
     */
    private static final String KEY_DESCRIPTION = "description";

    /**
     * Key for the user who sent the transaction.
     */
    private static final String KEY_FROM_USER = "fromUser";

    /**
     * Key for the amount of the transaction.
     */
    private static final String KEY_AMOUNT = "amount";

    /**
     * Key for the creation date of the transaction.
     */
    private static final String KEY_CREATED = "created";

    /**
     * Key for the status of the transaction.
     */
    private static final String KEY_STATUS = "status";

    /**
     * Key for the type of transaction.
     */
    private static final String KEY_TYPE = "type";

    /**
     * String used to signify denied transactions.
     */
    private static final String DENIED = "DENIED";

    /**
     * String used to signify approved transactions.
     */
    private static final String APPROVED = "APPROVED";

    /**
     * String used to signify pending transactions.
     */
    private static final String PENDING = "PENDING";

    /**
     * The list of statuses that a transaction can have.  Useful
     * for checking transaction statuses in other classes.
     */
    public enum STATUS {
        /**
         * Has been approved and paid.
         */
        APPROVED,

        /**
         * Has been denied and not paid.
         */
        DENIED,

        /**
         * Is awaiting approval and has not been paid.
         */
        PENDING,

        /**
         * Otherwise.
         */
        UNKNOWN
    }

    /**
     * String used to signify outgoing transactions.
     */
    private static final String TYPE_SENDING = "sending";

    /**
     * The list of transaction types.
     */
    public enum TYPE {
        /**
         * The transaction is outgoing.
         */
        SENDING,

        /**
         * Otherwise.
         */
        UNKNOWN
    }

    /**
     * Transaction ID.
     */
    private final String id;

    /**
     * Transaction revision number.
     */
    private final String rev;

    /**
     * The paid user.
     */
    private final String toUser;

    /**
     * The paid account.
     */
    private final String toAccount;

    /**
     * Transaction description.
     */
    private final String description;

    /**
     * The paying user.
     */
    private final String fromUser;

    /**
     * Transaction amount.
     */
    private final String amount;

    /**
     * Transaction UTC.
     */
    private final long created;

    /**
     * Transaction status.
     */
    private final STATUS status;

    /**
     * Transaction type.
     */
    private final TYPE type;

    /**
     * Constructs a new Transaction Object from the given transaction
     * JSONObject.
     *
     * @param obj The transaction to process into an object.
     */
    public Transaction(final JSONObject obj) {

        // Extract transaction details from JSON
        String fallback = "Not found";
        id = obj.optString(KEY_ID, fallback).trim();
        rev = obj.optString(KEY_REV, fallback).trim();
        toUser = obj.optString(KEY_TO_USER, fallback).trim();
        toAccount = obj.optString(KEY_TO_ACCOUNT, fallback).trim();
        description = obj.optString(KEY_DESCRIPTION, fallback).trim();
        fromUser = obj.optString(KEY_FROM_USER, fallback).trim();
        amount = obj.optString(KEY_AMOUNT, fallback).trim();
        created = obj.optLong(KEY_CREATED, 0);

        String stat = obj.optString(KEY_STATUS, fallback).trim();
        if (stat.equalsIgnoreCase(DENIED)) {
            status = STATUS.DENIED;
        } else if (stat.equalsIgnoreCase(APPROVED)) {
            status = STATUS.APPROVED;
        } else if (stat.equalsIgnoreCase(PENDING)) {
            status = STATUS.PENDING;
        } else {
            status = STATUS.UNKNOWN;
        }

        String t = obj.optString(KEY_TYPE, fallback);
        if (t.equalsIgnoreCase(TYPE_SENDING)) {
            type = TYPE.SENDING;
        } else {
            type = TYPE.UNKNOWN;
        }
    }

    /**
     * Constructs a new Transaction object with bogus data.
     * You shouldn't be using this.
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
    public final String toString() {
        // Just to make Transactions easier to log in other classes.
        String ret = String.format("Transaction: id: %s | rev: %s | "
                        + "toUser: %s | toAcc: %s | desc: %s | "
                        + "fromUser: %s | amount: %s | created: %s | "
                        + "date: %s | status: %s | type: %s",
                id, rev, toUser, toAccount, description, fromUser, amount,
                created, prettyDate(), status, type);
        return ret;
    }

    /**
     * Parses the UTC of the transaction into a Date object.
     *
     * @return A Date representing when the transaction was created.
     */
    public final Date prettyDate() {
        return new Date(created);
    }

    /**
     * Creates a Comparator which can be used to sort Transactions by their
     * date.
     *
     * @param ascending True if the Transactions should be sorted in
     *                  ascending order, false otherwise.
     * @return A Comparator for sorting transactions.
     */
    public static Comparator dateComparator(final boolean ascending) {
        if (ascending) {
            return new Comparator() {
                @Override
                public int compare(final Object lhs, final Object rhs) {
                    if (lhs instanceof Transaction
                            && rhs instanceof Transaction) {
                        Transaction left = (Transaction) lhs;
                        Transaction right = (Transaction) rhs;
                        return left.prettyDate().compareTo(right.prettyDate());
                    } else {
                        throw new IllegalArgumentException("Comparator "
                                + "only works on Transactions");
                    }
                }
            };
        } else {
            return new Comparator() {
                @Override
                public int compare(final Object lhs, final Object rhs) {
                    if (lhs instanceof Transaction
                            || rhs instanceof Transaction) {
                        Transaction left = (Transaction) lhs;
                        Transaction right = (Transaction) rhs;
                        return right.prettyDate().compareTo(left.prettyDate());
                    } else {
                        throw new IllegalArgumentException("Comparator "
                                + "only works on Transactions");
                    }
                }
            };
        }
    }

    /**
     * Creates a Comparator that keeps transactions that require
     * user attention at the top of the list, while other transactions
     * are sorted by their date.
     *
     * @param ascending True is dates should be sorted in ascending
     *                  order, false otherwise
     * @return A Comparator for sorting Transactions.
     */
    public static Comparator finalComparator(final boolean ascending) {
        return new Comparator() {
            @Override
            public int compare(final Object lhs, final Object rhs) {
                // Cast transactions so we can compare them
                if (!(lhs instanceof Transaction)
                        || !(rhs instanceof Transaction)) {
                    throw new IllegalArgumentException(
                            "Comparator only works on Transactions");
                }
                Transaction left = (Transaction) lhs;
                Transaction right = (Transaction) rhs;
                if (isFirst(left) && isFirst(right)) {
                    return dateComparator(ascending).compare(lhs, rhs);
                } else if (isFirst(left)) {
                    return -1;
                } else if (isFirst(right)) {
                    return 1;
                } else {
                    return dateComparator(ascending).compare(lhs, rhs);
                }
            }

            // Sending, pending transactions go first
            private boolean isFirst(final Transaction t) {
                return (t.getType().equals(TYPE.SENDING)
                        && t.getStatus().equals(STATUS.PENDING));
            }
        };
    }

    /**
     * Gets the ID of the transaction.
     *
     * @return The transaction's ID.
     */
    public final String getId() {
        return id;
    }

    /**
     * Gets the paid user.
     *
     * @return The user's name.
     */
    public final String getToUser() {
        return toUser;
    }

    /**
     * Gets the paid user's account.
     *
     * @return The user's account.
     */
    public final String getToAccount() {
        return toAccount;
    }

    /**
     * Gets the transaction description.
     *
     * @return The transaction description.
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Gets the paying user.
     *
     * @return The paying user.
     */
    public final String getFromUser() {
        return fromUser;
    }

    /**
     * Parses the transaction amount into a pretty String that matches
     * the local currency.
     *
     * @return The amount of the transaction.
     */
    public final String getPrettyAmount() {
        double amt = Double.parseDouble(amount);
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        String money = formatter.format(amt);
        return money;
    }

    /**
     * Gets the status of the transaction.
     *
     * @return The transaction status.
     */
    public final STATUS getStatus() {
        return status;
    }

    /**
     * Gets the type of the transaction.
     *
     * @return The transaction type.
     */
    public final TYPE getType() {
        return type;
    }
}

