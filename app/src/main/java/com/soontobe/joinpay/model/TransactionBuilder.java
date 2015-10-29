package com.soontobe.joinpay.model;

import android.support.annotation.NonNull;
import android.util.Log;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;

/**
 * TransactionBuilder assists with the creation and splitting of a
 * charge amongst multiple users.
 *
 * Created by davery on 10/26/2015.
 */
public class TransactionBuilder extends Observable implements Set<UserInfo> {

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
     * @param message The public note to be sent.
     */
    public final void setGeneralMessage(String message) {
        String cleaned = "";
        if(message != null) {
            cleaned = new String(message);
        }

        generalMessage = cleaned;

        for(UserInfo user: users) {
            if(user.isSelected()) {
                user.setPublicNote(cleaned);
            }
        }
    }

    public final boolean setAmount(UserInfo toSet, BigDecimal total) {

        if(toSet == null) {
            return false;
        }

        // Can't change amount on a user we don't have
        if(!users.contains(toSet)) {
            return false;
        }

        // Can't set the amount on an unselected or unlocked user
        if(!toSet.isSelected() || !toSet.isLocked()) {
            return false;
        }

        // Update the amount for the user
        toSet.setAmountOfMoney(total);
        setChanged();
        notifyObservers();
        return true;
    }

    /**
     * Selects or deselects the given user.
     * @param toSelect The user to be selected or deselected.
     * @param selected True if user is to be selected, false otherwise.
     * @return True if the operation was successful, false otherwise.
     */
    public final boolean selectUser(UserInfo toSelect, boolean selected) {

        if(toSelect == null) {
            return false;
        }

        // Can't select a user who is not in collection.
        if(!users.contains(toSelect)) {
            return false;
        }

        // Set the selection state of the user.
        toSelect.setSelected(selected);

        // Tasks specific to selecting or deselecting
        if(selected == false) {
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
     * @param user The user to be reset.
     */
    private void resetUser(UserInfo user) {
        if(user == null) return;
        user.setSelected(false);
        user.setAmountOfMoney(BigDecimal.valueOf(0, SCALE_PENNIES));
        user.setLocked(false);
        user.setPublicNote("");
    }

    /**
     * Locks or unlocks the given user.
     * @param toLock The user to be locked or unlocked.
     * @param locked True if user is to be locked, false if unlocked.
     * @return True if operation was successful, false otherwise.
     */
    public final boolean lockUser(UserInfo toLock, boolean locked) {
        // Cant lock/unlock someone we don't have
        if(!users.contains(toLock)) {
            return false;
        }

        // Can't lock/unlock an unselected user
        if(!toLock.isSelected()) {
            return false;
        }

        toLock.setLocked(locked);

        if(!locked) {
            // Unlocking user, need to resplit
            split(total());
        }

        setChanged();

        return true;
    }

    /**
     * Sets a new total for the transaction.
     * @param total The new total for the transaction, in pennies.
     * @return True if the total was changed successfully, false otherwise.
     */
    public final boolean setTotal(BigDecimal total) {

        // Can't set a transaction total without selecting users
        if(selectedUsers() <= 0) {
            return false;
        }

        // Make sure this value makes sense
        if(isValidTotal(total)) {
            // Split the transaction amongst unlocked users
            boolean success = split(total);
            if(success) {
                setChanged();
                notifyObservers();
            }
            return success;
        }

        return false;
    }

    /**
     * Splits the given amount amongst the selected users.
     * @param total The total to be split, in pennies.
     * @return True if the split was successful, false otherwise.
     */
    private final boolean split(BigDecimal total) {

        // Edge case: nothing to split if locked users have it covered
        if(lockedTotal() == total) {
            return true;
        }

        // Get the total remaining to be split amongst unlocked users
        // and how many ways it needs to be split
        BigDecimal totalToSplit = total.subtract(lockedTotal());
        BigDecimal ways = BigDecimal.valueOf(selectedUsers() - lockedUsers(), 0);

        Log.d("Split", "Splitting " + totalToSplit.toString() + " " + ways.toString() + " ways.");


        // How much each user will get, at least
        BigDecimal each = totalToSplit.divide(ways);

        // Remainder to be divided amongst users until it runs out
        BigDecimal rem = totalToSplit.remainder(ways);

        for(UserInfo user : users) {
            // Determine if user will share split charge.
            if(!user.isLocked() && user.isSelected()) {
                // User will cover the 'at least' amount.
                BigDecimal amt = BigDecimal.valueOf(each.unscaledValue().longValue(), each.scale());

                // Add portion of remainder, if any is available.
                if(rem.compareTo(BigDecimal.valueOf(0)) > 0) {
                    amt.add(BigDecimal.valueOf(1,2)); // add penny to amt
                    rem.subtract(BigDecimal.valueOf(1,2)); // sub penny from rem
                }
                user.setAmountOfMoney(amt);
            }
        }

        return true;
    }

    /**
     * Resets all users to their default state and clears any existing
     * transaction data.
     */
    public void resetTransaction() {
        for(UserInfo user: users) {
            user.setAmountOfMoney(BigDecimal.valueOf(0));
            user.setLocked(false);
            user.setSelected(false);
            user.setPublicNote("");
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Determines the validity of a given transaction total.
     * @param total The transaction total, in pennies.
     * @return True if the total is valid, false otherwise.
     */
    private boolean isValidTotal(BigDecimal total) {

        // Cannot have negative transactions
        if(total.compareTo(BigDecimal.valueOf(0)) < 0) {
            return false;
        }


        // Total cannot be less than locked total
        if(total.compareTo(lockedTotal()) < 0) {
            return false;
        }

        // Can't set a total if no users are selected
        if(selectedUsers() <= 0) {
            return false;
        }

        return true;
    }

    /**
     * Collects the total for users locked in a transaction.
     * @return The total locked transaction amount.
     */
    private BigDecimal lockedTotal() {

        BigDecimal total = BigDecimal.valueOf(0);
        for (UserInfo user : users) {
            if(user.isLocked()) {
                total.add(user.getAmountOfMoney());
            }
        }
        return total;
    }

    /**
     * Gets a count of the number of selected users.
     * @return The number of selected users.
     */
    public int selectedUsers() {
        int num = 0;
        for(UserInfo user : users) {
            if(user.isSelected()) {
                num++;
            }
        }
        return num;
    }

    /**
     * Gets a count of the number of locked users.
     * @return The number of locked users.
     */
    private int lockedUsers() {
        int num = 0;
        for(UserInfo user : users) {
            if(user.isLocked()) {
                num++;
            }
        }
        return num;
    }

    /**
     * Collects the total for the split transaction.
     * @return The transaction total across all users.
     */
    public BigDecimal total() {
        BigDecimal amt = BigDecimal.valueOf(0);
        for(UserInfo user: users) {
            if(user.isSelected()) {
                amt = amt.add(user.getAmountOfMoney());
            }
        }
        return amt;
    }

    @Override
    public boolean add(UserInfo object) {
        resetUser(object);
        return users.add(object);
    }

    @Override
    public boolean addAll(Collection<? extends UserInfo> collection) {
        for (UserInfo user: collection) {
            resetUser(user);
        }
        return users.addAll(collection);
    }

    @Override
    public void clear() {
        users.clear();
    }

    @Override
    public boolean contains(Object object) {
        return users.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return users.containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return users.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<UserInfo> iterator() {
        return users.iterator();
    }

    @Override
    public boolean remove(Object object) {
        return users.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return users.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return users.retainAll(collection);
    }

    @Override
    public int size() {
        return users.size();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return users.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(T[] array) {
        return users.toArray(array);
    }
}
