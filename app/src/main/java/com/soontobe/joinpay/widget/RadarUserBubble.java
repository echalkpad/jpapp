package com.soontobe.joinpay.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.soontobe.joinpay.R;
import com.soontobe.joinpay.model.UserInfo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Observable;
import java.util.Observer;

/**
 * This is a custom widget for the radar screen.  It is used to represent
 * users on the radar.  It updates its appearance to reflect the state of
 * a user.  The Observer pattern is used to keep the bubble up-to-date
 * with the user.
 * <br>
 * CAUTION: This widget has the fixed size 108dp*108dp whether the side
 * buttons show or not.  The center part of this widget have the size
 * 96dp*96dp.
 * </br>
 * -->see layout file for detail.
 */
public class RadarUserBubble extends FrameLayout
        implements Observer, View.OnClickListener {

    /**
     * Used for tagging logs from this class.
     */
    private static final String TAG = "user_bubble";

    /**
     * The size that the user name will shrink to when a money
     * value is present.
     */
    private static final float TEXT_SIZE_SMALL = 14.0f;

    /**
     * The size that the user name will grow to when no amount
     * is present.
     */
    private static final float TEXT_SIZE_BIG = 18.0f;

    /**
     * The user represented by this bubble.
     */
    private UserInfo myUserInfo;

    /**
     * The circle that acts as a the option button panel.
     */
    private ImageView mOptionPanel;

    /**
     * The button that allows the user to be locked into their
     * transaction value.
     */
    private ImageButton mLockButton;

    /**
     * The button that allows the user's transaction amount
     * to be editted.
     */
    private ImageButton mEditButton;

    /**
     * The button that removes the user from the transaction.
     */
    private ImageButton mXButton;

    /**
     * The bubble is supposed to act like a button.  This allows
     * the user to be selected and added to the transaction.
     */
    private Button mCenterButton;

    /**
     * A text field for displaying the user's name.
     */
    private TextView mNameText;

    /**
     * A text field for displaying the user's contribution to the
     * transaction.
     */
    private TextView mMoneyText;

    /**
     * Used to store the initial parameters of the user name, so that
     * we can safely manipulate it in the code.
     */
    private ViewGroup.LayoutParams nameTextParams;

    /**
     * The listener that should handle when the lock button is pressed.
     */
    private OnLockListener lockListener;

    /**
     * The listener that should handle when the edit button is pressed.
     */
    private OnEditListener editListener;

    /**
     * The listener that should handle when the bubble is selected.
     */
    private OnSelectListener selectListener;

    /**
     * The listener that should handle when the deselect button is pressed.
     */
    private OnDeselectListener deselectListener;

    /**
     * @param context The context in which the bubble is inflated.
     * @see #RadarUserBubble(Context, AttributeSet, int)
     */
    public RadarUserBubble(final Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.radar_user_bubble, this);

        // Initialize the UI
        init();
    }

    /**
     * @param context The context in which the bubble is inflated.
     * @param attrs   Not used.
     * @see #RadarUserBubble(Context, AttributeSet, int)
     */
    public RadarUserBubble(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.radar_user_bubble, this);

        // Initialize the UI
        init();
    }

    /**
     * Constructs a new RadarUserBubble.  Only present so that a layouts with
     * bubbles in them can be inflated.
     *
     * @param context      The context in which the bubble is inflated.
     * @param attrs        Not used.
     * @param defStyleAttr Not used.
     */
    public RadarUserBubble(final Context context, final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.radar_user_bubble, this);

        // Initialize the UI
        init();
    }

    /**
     * Constructs a new RadarUserView.
     *
     * @param context The context in which the bubble is being created.
     * @param info    The user to associate with the bubble.
     */
    public RadarUserBubble(final Context context, final UserInfo info) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.radar_user_bubble, this);

        // Associate this bubble with the given user
        myUserInfo = info;
        info.addObserver(this);

        // Initialize the UI
        init();
    }

    /**
     * @param listener The new lock listener.
     * @see #lockListener
     */
    public final void setLockListener(final OnLockListener listener) {
        this.lockListener = listener;
    }

    /**
     * @param listener The new edit listener.
     * @see #editListener
     */
    public final void setEditListener(final OnEditListener listener) {
        this.editListener = listener;
    }

    /**
     * @param listener The new selection listener.
     * @see #selectListener
     */
    public final void setSelectListener(final OnSelectListener listener) {
        this.selectListener = listener;
    }

    /**
     * @param listener The new deselection listener.
     * @see #deselectListener
     */
    public final void setDeselectListener(final OnDeselectListener listener) {
        this.deselectListener = listener;
    }

    /**
     * Assigns the bubble to the given user.
     *
     * @param user The user to set the bubble to.
     */
    public final void setUserInfo(final UserInfo user) {

        // Unregister from current user
        if (myUserInfo != null) {
            Log.d(TAG, "Detaching bubble from user: "
                    + myUserInfo.getUserName());
            myUserInfo.deleteObserver(this);
            myUserInfo = null;
        }

        // Register with new user
        String name = null;
        if (user != null) {
            user.addObserver(this);
            name = user.getUserName();
        }

        Log.d(TAG, "Assigning bubble to user: " + name);
        myUserInfo = user;
        update(user, null);
    }

    /**
     * Initializes the UI.
     */
    private void init() {
        // Collect UI elements
        mLockButton = (ImageButton) findViewById(R.id.option_lock_button);
        mEditButton = (ImageButton) findViewById(R.id.option_edit_button);
        mXButton = (ImageButton) findViewById(R.id.option_deselect_button);
        mOptionPanel = (ImageView) findViewById(R.id.option_button_panel);
        mCenterButton = (Button) findViewById(R.id.user_bubble_button);
        mNameText = (TextView) findViewById(R.id.user_bubble_name);
        mMoneyText = (TextView) findViewById(R.id.user_bubble_money);

        // Option buttons shouldn't be visible at first.
        switchExpandPanel(false);

        // Save the initial layout of the name text so we
        // can safely manipulate in other methods
        nameTextParams = mNameText.getLayoutParams();

        // Configure buttons to send clicks to this fragment
        mCenterButton.setOnClickListener(this);
        mEditButton.setOnClickListener(this);
        mLockButton.setOnClickListener(this);
        mXButton.setOnClickListener(this);

        // Configure the bubble for the user.
        update(myUserInfo, null);
    }

    /**
     * Displays the given amount of money on the bubble.
     *
     * @param amountOfMoney The quantity to be displayed.
     */
    public final void setMoneyAmount(final BigDecimal amountOfMoney) {
        // A wrapper for the value, in case of null.
        BigDecimal toDisplay = BigDecimal.valueOf(0);
        if (amountOfMoney == null
                || amountOfMoney.compareTo(BigDecimal.valueOf(0)) == 0) {
            // Having no money means that a currency value should not
            // be displayed
            mMoneyText.setVisibility(View.GONE);

            // Username gets larger if value is not present.
            mNameText.setTextSize(TEXT_SIZE_BIG);

            // Reset the username's layout so it rewraps the text.
            FrameLayout.LayoutParams params =
                    new LayoutParams(mNameText.getLayoutParams());
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.setMargins(0, 0, 0, 0);
            mNameText.setLayoutParams(params);
        } else {
            toDisplay = amountOfMoney;

            //Restore default layout with money amount
            mMoneyText.setVisibility(View.VISIBLE);
            mNameText.setTextSize(TEXT_SIZE_SMALL);
            mNameText.setLayoutParams(nameTextParams);
        }

        // Always set the amount text, just in case.
        String pretty = NumberFormat.getCurrencyInstance().format(toDisplay);
        mMoneyText.setText(pretty);
    }

    /**
     * Toggles the visibility of the option button panel.
     *
     * @param visible True if panel should be displayed, false otherwise.
     */
    public final void switchExpandPanel(final boolean visible) {
        String v;
        int vis;
        if (visible) {
            v = "visible";
            vis = View.VISIBLE;
        } else {
            v = "invisible";
            vis = View.INVISIBLE;
        }
        Log.d(TAG, "Making option button panel " + v);

        // Display option panel
        mOptionPanel.setVisibility(vis);

        // Display buttons
        mLockButton.setVisibility(vis);
        mEditButton.setVisibility(vis);
        mXButton.setVisibility(vis);
    }

    /**
     * Toggles the lock buttons appearance to match the user's
     * state.
     *
     * @param isLocked True if the user is locked, false otherwise.
     */
    public final void changeLockState(final boolean isLocked) {
        if (isLocked) {
            mLockButton.setImageResource(R.drawable.locked_white);
        } else {
            mLockButton.setImageResource(R.drawable.unlocked_white);
        }

        String locked;
        if (isLocked) {
            locked = "locked";
        } else {
            locked = "unlocked";
        }
        Log.d(TAG, "Displaying " + locked + " option button");
    }

    /**
     * Toggles the appearance of the button based on whether the
     * user is selected.
     *
     * @param isSelected True if user is selected, false otherwise.
     */
    public final void setSelectState(final boolean isSelected) {
        String rendering;
        int resId;

        // Change bubble's appearance based on user selection.
        if (isSelected) {
            rendering = "selected";
            resId = R.drawable.shape_circle_darkgreen_w_border;
        } else {
            rendering = "unselected";
            resId = R.drawable.shape_circle_green_w_border;
        }

        Log.d(TAG, "Rendering bubble: " + rendering);
        mCenterButton.setBackgroundResource(resId);
    }

    @Override
    public final boolean isInEditMode() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public final void update(final Observable observable, final Object data) {
        if (!(observable instanceof UserInfo)) {
            Log.e(TAG, "Received update from non user: " + observable);
            return;
        }

        if (!((UserInfo) observable).equals(myUserInfo)) {
            Log.e(TAG, "Update from: " + observable
                    + " is not from my user: " + myUserInfo);
            return;
        }
        Log.d(TAG, "Updating bubble for user: " + myUserInfo.getUserName());

        // Update my selected state of the button
        setSelectState(myUserInfo.isSelected());

        // Make sure the option panel is not visible if user is
        // not selected.
        if (!myUserInfo.isSelected()) {
            switchExpandPanel(false);
        }

        // Update my locked state
        changeLockState(myUserInfo.isLocked());

        // Display the user's new information
        setMoneyAmount(myUserInfo.getAmountOfMoney());
        mNameText.setText(myUserInfo.getUserName());
    }

    /**
     * Handles when the user bubble is selected.  If the user has not
     * been selected, this calls the onSelectListener.  Otherwise,
     * it simply toggles the option panel.
     *
     * @param v The View that was clicked.
     */
    public final void onSelect(final View v) {
        Log.d(TAG, "Selected bubble for user: " + myUserInfo.getUserName());
        if (myUserInfo.isSelected()) {
            Log.d(TAG, "User already selected. Toggling option panel.");
            boolean toggle;
            if (mOptionPanel.getVisibility() == VISIBLE) {
                toggle = false;
            } else {
                toggle = true;
            }
            switchExpandPanel(toggle);
        } else {
            Log.d(TAG, "User has not yet been selected. Notifying listener.");
            if (selectListener != null) {
                selectListener.onSelect(myUserInfo);
            }
            // Also deactivate the option panel, if it is present.
            switchExpandPanel(false);
        }
    }

    /**
     * Handles when the deselect option button is pressed.
     *
     * @param v The View that was clicked.
     */
    public final void onDeselect(final View v) {
        Log.d(TAG, "Deselect button pressed for user: "
                + myUserInfo.getUserName());
        // Always close the option panel
        switchExpandPanel(false);

        if (deselectListener != null) {
            deselectListener.onDeselect(myUserInfo);
        }
    }

    /**
     * Handles when the edit button is pressed.
     *
     * @param v The View that was clicked.
     */
    public final void onEdit(final View v) {
        Log.d(TAG, "Edit button clicked for user: "
                + myUserInfo.getUserName());

        if (editListener != null) {
            editListener.onEdit(myUserInfo);
        }
    }

    /**
     * Handles when the lock button is pressed.
     *
     * @param v The View that was clicked.
     */
    public final void onLock(final View v) {
        Log.d(TAG, "Lock button clicked for user: " + myUserInfo.getUserName());

        if (lockListener != null) {
            lockListener.onLock(myUserInfo);
        }
    }

    @Override
    public final void onClick(final View v) {
        // Allows the view to route the click events from its
        // own buttons instead of passing them along to its
        // containing activity
        switch (v.getId()) {
            case R.id.user_bubble_button:
                onSelect(v);
                break;
            case R.id.option_lock_button:
                onLock(v);
                break;
            case R.id.option_deselect_button:
                onDeselect(v);
                break;
            case R.id.option_edit_button:
                onEdit(v);
                break;
            default:
                Log.e(TAG, "Unknown view received: " + v);
                break;
        }
    }

    /**
     * Should be implemented by portions of the UI that need to react
     * when the lock button is clicked.
     */
    public interface OnLockListener {

        /**
         * Locks the given user.
         *
         * @param user The user to be locked.
         */
        void onLock(UserInfo user);
    }

    /**
     * Should be implemented by portions of the UI that need to react
     * when the edit button is clicked.
     */
    public interface OnEditListener {

        /**
         * Allows the given user to be edited.
         *
         * @param user The user to be edited.
         */
        void onEdit(UserInfo user);
    }

    /**
     * Should be implemented by portions of the UI
     * that need to react when the user's bubble is selected.
     */
    public interface OnSelectListener {

        /**
         * Selects the given user.
         *
         * @param user The user to be selected.
         */
        void onSelect(UserInfo user);
    }

    /**
     * Should be implemented by portions of the UI
     * that need to react when the deselect button is clicked.
     */
    public interface OnDeselectListener {

        /**
         * Deselects the given user.
         *
         * @param user The user to be deselected.
         */
        void onDeselect(UserInfo user);
    }
}
