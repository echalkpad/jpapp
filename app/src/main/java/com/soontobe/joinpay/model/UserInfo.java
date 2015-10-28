package com.soontobe.joinpay.model;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 * User information model for information exchange between RadarView and
 * BigBubble.
 *
 */
public class UserInfo {

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
	 * The personal note that should be sent to this user.
	 */
	private String personalNote;

	/**
	 * The amount of money that has been assigned to the user for the
	 * current transaction.
	 */
	private BigDecimal amountOfMoney;

	/**
	 * True if the user is the currently logged in user, false otherwise.
	 */
	private boolean isMyself; // Is this user just myself

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
		userName = "";
		publicNote = "";
		personalNote = "";
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
	 * @see #isMyself
	 * @param myself The new value for myself.
	 */
	public final void setMyself(final boolean myself) {
		this.isMyself = myself;
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
	}

	/**
	 * @see #personalNote
	 * @return The value of personalNote.
	 */
	public final String getPersonalNote() {
		return personalNote;
	}

	/**
	 * @see #personalNote
	 * @param note The new value of note.
	 */
	public final void setPersonalNote(final String note) {
		this.personalNote = note;
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
	}

	/**
	 * @see #userName
	 * @return The value of userName.
	 */
	public final String getUserName() {
		return userName;
	}

	/**
	 * @see #userName
	 * @param name The new value of name.
	 */
	public final void setUserName(final String name) {
		this.userName = name;
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
