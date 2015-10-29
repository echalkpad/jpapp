package com.soontobe.joinpay.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Observable;

/**
 * User information model for information exchange between RadarView and
 * BigBubble.
 *
 */
public class UserInfo extends Observable {

	/**
	 * The user's ID.
	 */
	private int userId;

	/**
	 * The user's name.
	 */
	private String userName;

	/**
	 * The public note that should be sent to this user.
	 */
	private String publicNote;

	/**
	 * The amount of money that has been assigned to the user for the
	 * current transaction.
	 */
	private BigDecimal amountOfMoney;

	/**
	 * True if the user is the currently logged in user, false otherwise.
	 */
	private boolean isMyself;

	/**
	 * True if the user is a contact of the currently logged in user,
	 * false otherwise.
	 */
	private boolean isContact;

	/**
	 * True if the user's transaction value has been locked, false
	 * otherwise.
	 */
	private boolean isLocked;

	/**
	 * True if the user has been selected on the radar screen, false
	 * otherwise.
	 */
	private boolean isSelected; // Is this user selected

	/**
	 * Constructs a new UserInfo.
	 */
	public UserInfo() {
		isContact = false;
		isMyself = false;
		amountOfMoney = BigDecimal.valueOf(0);
		userName = "NoName";
		publicNote = "";
	}

	/**
	 * Constructs a new UserInfo.
	 * @param name The name of the user.
	 * @param isSelf True if the user is the currently logged in user,
	 *               false otherwise.
	 */
	public UserInfo(String name, boolean isSelf) {
		userName = name;
		isMyself = isSelf;
		isContact = false;
		publicNote = "";
		amountOfMoney = BigDecimal.valueOf(0);
	}

	/////////////////////////
	// Getters and Setters //
	/////////////////////////

	/**
	 * @see #isMyself
	 * @return The value of isMyself.
	 */
	public final boolean isMyself() {
		return isMyself;
	}

	/**
	 * @see #isSelected
	 * @return The value of isSelected.
	 */
	public final boolean isSelected() {
		return isSelected;
	}

	/**
	 * @see #isSelected
	 * @param selected The new value of selected.
	 */
	public final void setSelected(final boolean selected) {
		this.isSelected = selected;
		setChanged();
		notifyObservers();
	}

	/**
	 * @see #isContact
	 * @return The value of isContact.
	 */
	public final boolean isContact() {
		return isContact;
	}

	/**
	 * @see #isContact
	 * @param contactState The new value of contactState.
	 */
	public final void setContactState(final boolean contactState) {
		this.isContact = contactState;
		setChanged();
		notifyObservers();
	}

	/**
	 * @see #publicNote
	 * @return The value of publicNote.
	 */
	public final String getPublicNote() {
		return publicNote;
	}

	/**
	 * @see #publicNote
	 * @param note The new value of note.
	 */
	public final void setPublicNote(final String note) {
		this.publicNote = note;
		setChanged();
		notifyObservers();
	}

	/**
	 * @see #isLocked
	 * @return The value of isLocked.
	 */
	public final boolean isLocked() {
		return isLocked;
	}

	/**
	 * @see #isLocked
	 * @param locked The new value of locked.
	 */
	public final void setLocked(final boolean locked) {
		this.isLocked = locked;
		setChanged();
		notifyObservers();
	}

	/**
	 * @see #userId
	 * @return The value of userId.
	 */
	public final int getUserId() {
		return userId;
	}

	/**
	 * @see #userId
	 * @param id The new value of id.
	 */
	public final void setUserId(final int id) {
		this.userId = id;
		setChanged();
		notifyObservers();
	}

	/**
	 * @see #userName
	 * @return The value of userName.
	 */
	public final String getUserName() {
		return userName;
	}

	/**
	 * @see #amountOfMoney
	 * @return The value of amountOfMoney.
	 */
	public final BigDecimal getAmountOfMoney() {
		return amountOfMoney;
	}

	/**
	 * @see #amountOfMoney
	 * @param amount The new value of amount.
	 */
	public final void setAmountOfMoney(final BigDecimal amount) {
		this.amountOfMoney = amount;
		setChanged();
		notifyObservers();
	}

	/**
	 * Generates a formatted version of the amount that matches the locale.
	 * @return A beautified string representing the amount.
	 */
	public final String getPrettyAmount() {
		NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
		String money = numberFormat.format(amountOfMoney);
		return money;
	}

	@Override
	public final String toString() {
		String str = "UserInfo:";
		str += "Name = " + userName;
		return str;
	}
}
