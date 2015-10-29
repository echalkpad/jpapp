package com.soontobe.joinpay.model;

import android.support.annotation.NonNull;
import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;

/**
 * TransactionBuilder assists with the creation and splitting of a
 * charge amongst multiple users.
 * <p/>
 * Created by davery on 10/26/2015.
 */
public class TransactionBuilder extends Observable implements Set<UserInfo> {

    /**
     * For tagging logs from this class.
     */
    private static final String TAG = "keeper";

    /**
     * The set of users involved in this transaction.
     */
    private Set<UserInfo> users;

    /**
     * Scale used for BigDecimals representing pennies.
     */
    private static final int SCALE_PENNIES = 2;

    /**
     * Used to keep track of the general message that should be sent
     * to all users.
     */
    private String generalMessage;

    /**
     * Constructs a new TransactionBuilder.
     */
    public TransactionBuilder() {
        users = new HashSet<>();
        generalMessage = "";
    }

    /**
     * Sets the general message that is sent to all users involved in
     * the transaction.
     *
     * @param message The public note to be sent.
     */
    public final void setGeneralMessage(final String message) {
        // Protect users from null messages.
        String cleaned = "";
        if (message != null) {
            cleaned = new String(message);
        }
        generalMessage = cleaned;

        Log.d(TAG, "Updating general message: " + message);
        for (UserInfo user : users) {
            user.setPublicNote(cleaned);
        }
    }

    /**
     * @return The general message for the transaction.
     * @see #generalMessage
     */
    public final String getGeneralMessage() {
        return new String(generalMessage);
    }

    /**
     * Sets the given user's contribution to the split charge.  Only
     * user's who are registered in the keeper, have been selected,
     * and are locked can have their specific amount changed.
     *
     * @param toSet The user for whom the amount should be set.
     * @param total The amount to assign to the user.
     * @return True if the user's amount was set successfully, false
     * otherwise.
     */
    public final boolean setAmount(final UserInfo toSet,
                                   final BigDecimal total) {

        if (toSet == null) {
            Log.e(TAG, "Can't set amount on null user.");
            return false;
        }

        // Can't change amount on a user we don't have
        if (!users.contains(toSet)) {
            Log.e(TAG, toSet.getUserName() + " not registered");
            return false;
        }

        // Can't set the amount on an unselected or unlocked user
        if (!toSet.isSelected() || !toSet.isLocked()) {
            Log.e(TAG, toSet.getUserName() + " is not selected and locked");
            return false;
        }

        // Sanitize the scale of the total
        total.setScale(2, RoundingMode.FLOOR);

        // Update the amount for the user
        String debug = "Setting %s\'s amount to %s";
        Log.d(TAG, String.format(debug, toSet.getUserName(),
                total.toString()));
        toSet.setAmountOfMoney(total);
        setChanged();
        notifyObservers();
        return true;
    }

    /**
     * Selects or deselects the given user.
     *
     * @param toSelect The user to be selected or deselected.
     * @param selected True if user is to be selected, false otherwise.
     * @return True if the operation was successful, false otherwise.
     */
    public final boolean selectUser(final UserInfo toSelect,
                                    final boolean selected) {

        if (toSelect == null) {
            return false;
        }

        // Can't select a user who is not in collection.
        if (!users.contains(toSelect)) {
            return false;
        }

        // Set the selection state of the user.
        toSelect.setSelected(selected);

        // Tasks specific to selecting or deselecting
        if (!selected) {
            // If the user is being deselected, they should be reset
            resetUser(toSelect);
        } else {
            // User is being selected, update user
            toSelect.setPublicNote(generalMessage);
        }

        // Selecting or unselecting a user means we need to resplit
        split(total());

        // Update observers
        setChanged();
        notifyObservers();

        return true;
    }

    /**
     * Resets the given user to the default, unselected state.
     *
     * @param user The user to be reset.
     */
    private void resetUser(final UserInfo user) {
        if (user == null) {
            Log.e(TAG, "Can't reset null user!");
            return;
        }
        Log.d(TAG, "Resetting " + user.getUserName());
        user.setSelected(false);
        user.setAmountOfMoney(BigDecimal.valueOf(0));
        user.setLocked(false);
        user.setPublicNote("");
    }

    /**
     * Locks or unlocks the given user.
     *
     * @param toLock The user to be locked or unlocked.
     * @param locked True if user is to be locked, false if unlocked.
     * @return True if operation was successful, false otherwise.
     */
    public final boolean lockUser(final UserInfo toLock,
                                  final boolean locked) {
        if (toLock == null) {
            Log.e(TAG, "Can't lock null user");
            return false;
        }

        // Cant lock/unlock someone we don't have
        if (!users.contains(toLock)) {
            Log.e(TAG, toLock.getUserName() + " not registered");
            return false;
        }

        // Can't lock/unlock an unselected user
        if (!toLock.isSelected()) {
            Log.e(TAG, toLock.getUserName() + " can't be locked "
                    + "before being selected");
            return false;
        }

        String msg;
        if (locked) {
            msg = "Locking";
        } else {
            msg = "Unlocking";
        }
        Log.d(TAG, msg + " " + toLock.getUserName());
        toLock.setLocked(locked);

        // If the user is being unlocked, the total should be resplit.
        if (!locked) {
            Log.d(TAG, "Unlocking triggers check resplitting");
            split(total());
        }

        setChanged();
        notifyObservers();
        return true;
    }

    /**
     * Sets a new total for the transaction.
     *
     * @param total The new total for the transaction, in pennies.
     * @return True if the total was changed successfully, false otherwise.
     */
    public final boolean setTotal(final BigDecimal total) {

        // Can't set a transaction total without selecting users
        if (selectedUsers() <= 0) {
            return false;
        }

        // Make sure this value makes sense
        if (isValidTotal(total)) {
            // Split the transaction amongst unlocked users
            boolean success = split(total);
            if (success) {
                setChanged();
                notifyObservers();
            }
            return success;
        }

        return false;
    }

    /**
     * Splits the given amount amongst the selected users.
     *
     * @param total The total to be split, in pennies.
     * @return True if the split was successful, false otherwise.
     */
    private boolean split(final BigDecimal total) {

        // Validate the total to split
        if (!isValidTotal(total)) {
            return false;
        }

        // Can't split the total if there are no selected+unlocked users
        int splittable = selectedUsers() - lockedUsers();
        if (splittable == 0) {
            return false;
        }

        // Edge case: nothing to split if locked users have it covered
        if (lockedTotal() == total) {
            return true;
        }

        // Get the total remaining to be split amongst unlocked users
        // and how many ways it needs to be split
        BigDecimal totalToSplit = total.subtract(lockedTotal());
        BigDecimal ways = BigDecimal
                .valueOf(selectedUsers() - lockedUsers(), 0);

        Log.d("Split", "Splitting " + totalToSplit.toString()
                + " " + ways.toString() + " ways.");

        // TODO handling of the remainder is wrong.
        // How much each user will get, at least
        BigDecimal each = totalToSplit.divide(ways, 2, RoundingMode.FLOOR);

        // Remainder to be divided amongst users until it runs out
        BigDecimal rem = totalToSplit.remainder(ways);

        String debug = "Giving %s to each user, with a shared remainder of %s";
        Log.d("Split", String.format(debug, each.toString(), rem.toString()));

        for (UserInfo user : users) {
            // Determine if user will share split charge.
            if (!user.isLocked() && user.isSelected()) {
                // User will cover the 'at least' amount.
                BigDecimal amt = BigDecimal
                        .valueOf(each.unscaledValue().longValue(),
                                each.scale());

                // Add portion of remainder, if any is available.
                if (rem.compareTo(BigDecimal.valueOf(0)) > 0) {
                    // Add penny to amount
                    amt = amt.add(BigDecimal.valueOf(1, 2));
                    // Sub penny from remainder
                    rem = rem.subtract(BigDecimal.valueOf(1));
                }
                user.setAmountOfMoney(amt);
            }
        }

        // Sanity check: make sure total matches given
        if (!total.equals(total())) {
            debug = "Given total %s does not match final total %s";
            Log.e("Split", String.format(debug, total, total()));
        }

        return true;
    }

    /**
     * Resets all users to their default state and clears any existing
     * transaction data.
     */
    public final void resetTransaction() {
        for (UserInfo user : users) {
            user.setAmountOfMoney(BigDecimal.valueOf(0));
            user.setLocked(false);
            user.setSelected(false);
            user.setPublicNote("");
        }
        generalMessage = "";
        setChanged();
        notifyObservers();
    }

    /**
     * Determines the validity of a given transaction total.
     *
     * @param total The transaction total, in pennies.
     * @return True if the total is valid, false otherwise.
     */
    private boolean isValidTotal(final BigDecimal total) {

        // Cannot have negative transactions
        if (total.compareTo(BigDecimal.valueOf(0)) < 0) {
            return false;
        }

        // Total cannot be less than locked total
        if (total.compareTo(lockedTotal()) < 0) {
            return false;
        }

        // Can't set a total if no users are selected
        if (selectedUsers() <= 0) {
            return false;
        }

        //

        return true;
    }

    /**
     * Collects the total for users locked in a transaction.
     *
     * @return The total locked transaction amount.
     */
    private BigDecimal lockedTotal() {

        BigDecimal total = BigDecimal.valueOf(0);
        for (UserInfo user : users) {
            if (user.isLocked()) {
                total = total.add(user.getAmountOfMoney());
            }
        }
        return total;
    }

    /**
     * Gets a count of the number of selected users.
     *
     * @return The number of selected users.
     */
    public final int selectedUsers() {
        int num = 0;
        for (UserInfo user : users) {
            if (user.isSelected()) {
                num++;
            }
        }
        return num;
    }

    /**
     * Gets a count of the number of locked users.
     *
     * @return The number of locked users.
     */
    private int lockedUsers() {
        int num = 0;
        for (UserInfo user : users) {
            if (user.isLocked()) {
                num++;
            }
        }
        return num;
    }

    /**
     * Collects the total for the split transaction.
     *
     * @return The transaction total across all users.
     */
    public final BigDecimal total() {
        BigDecimal amt = BigDecimal.valueOf(0);
        for (UserInfo user : users) {
            if (user.isSelected()) {
                amt = amt.add(user.getAmountOfMoney());
            }
        }
        return amt;
    }

    @Override
    public final boolean add(final UserInfo object) {
        resetUser(object);
        return users.add(object);
    }

    @Override
    public final boolean addAll(
            final Collection<? extends UserInfo> collection) {
        for (UserInfo user : collection) {
            resetUser(user);
        }
        return users.addAll(collection);
    }

    @Override
    public final void clear() {
        users.clear();
    }

    @Override
    public final boolean contains(final Object object) {
        return users.contains(object);
    }

    @Override
    public final boolean containsAll(final Collection<?> collection) {
        return users.containsAll(collection);
    }

    @Override
    public final boolean isEmpty() {
        return users.isEmpty();
    }

    @NonNull
    @Override
    public final Iterator<UserInfo> iterator() {
        return users.iterator();
    }

    @Override
    public final boolean remove(final Object object) {
        return users.remove(object);
    }

    @Override
    public final boolean removeAll(final Collection<?> collection) {
        return users.removeAll(collection);
    }

    @Override
    public final boolean retainAll(final Collection<?> collection) {
        return users.retainAll(collection);
    }

    @Override
    public final int size() {
        return users.size();
    }

    @NonNull
    @Override
    public final Object[] toArray() {
        return users.toArray();
    }

    @NonNull
    @Override
    public final <T> T[] toArray(final T[] array) {
        return users.toArray(array);
    }
}
