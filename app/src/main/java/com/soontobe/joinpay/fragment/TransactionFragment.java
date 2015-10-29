package com.soontobe.joinpay.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.PositionHandler;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.Utility;
import com.soontobe.joinpay.model.TransactionBuilder;
import com.soontobe.joinpay.model.UserInfo;
import com.soontobe.joinpay.widget.BigBubblePopupWindow;
import com.soontobe.joinpay.widget.RadarUserBubble;
import com.soontobe.joinpay.widget.RadarUserBubble.OnDeselectListener;
import com.soontobe.joinpay.widget.RadarUserBubble.OnEditListener;
import com.soontobe.joinpay.widget.RadarUserBubble.OnLockListener;
import com.soontobe.joinpay.widget.RadarUserBubble.OnSelectListener;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

/**
 * TransactionFragment describes the process of selecting a list of users
 * and creating a shared transaction.
 */
public abstract class TransactionFragment extends Fragment
        implements LoaderCallbacks<Void>, Observer, OnClickListener {

    /**
     * Catches user interactions with fragment.
     */
    private OnFragmentInteractionListener mListener;

    /**
     * Used for tagging logs from this class.
     */
    private static final String TAG = "bubbles";

    /**
     * Controls the position of the popup window.
     */
    private static final int POPUP_Y = 200;

    /**
     * The Layout holding all the user bubbles.
     */
    private FrameLayout mBubbleFrameLayout;

    /**
     * A list of user bubbles.
     */
    private Map<UserInfo, RadarUserBubble> mUserBubbles;

    /**
     * The bubble corresponding to the current user.
     */
    private RadarUserBubble mSelfBubble;

    /**
     * The View in which the activity is displayed.
     */
    private View mCurrentView;

    /**
     * A popup window which allows the user to edit transaction details.
     */
    private BigBubblePopupWindow mBigBubble;

    /**
     * Displays the number of selected users.
     */
    private TextView mSelectCountText;

    /**
     * Resets the transaction.
     */
    private Button mResetButton;

    /**
     * Field for inputting a total amount for a transaction.
     */
    private EditText mTotalAmount;

    /**
     * The "next" button.  Selected once users have been selected
     * and the amounts are set.
     */
    private Button mSendMoneyButton;

    /**
     * Text field for the note to add to the transaction.
     */
    private EditText mGroupNote;

    /**
     * The UserInfo for the current user.
     */
    private UserInfo myUserInfo;

    /**
     * A flag for updating the total text box without
     * triggering an infinite loop of updates.
     */
    private boolean ignoreAmountChanges;

    /**
     * Keeps the transaction record keeping out of the UI.
     */
    private TransactionBuilder keeper;

    /**
     * Constructs a new TransactionFragment.
     */
    public TransactionFragment() {

    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public final void onStop() {
        super.onStop();
    }

    /**
     * The click handler for all the buttons in this fragment.
     *
     * @param v The view that was clicked.
     */
    public final void onClick(final View v) {
        switch (v.getId()) {
            case R.id.btn_radar_view_cross:
                // Reset buttons resets the transaction.
                Log.d(TAG, "Resetting keeper.");
                keeper.resetTransaction();
                break;
            default:
                Log.d(TAG, "Received click from unknown source");
                break;
        }
    }

    @Override
    public final View onCreateView(final LayoutInflater inflater,
                                   final ViewGroup container,
                                   final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (mCurrentView == null) {
            mCurrentView = inflater.inflate(R.layout.main_tab,
                    container, false);
            initUI();
        }

        // Create ViewGroup if the object does not exist,
        // otherwise use the current one.
        ViewGroup parent = (ViewGroup) mCurrentView.getParent();
        if (parent != null) {
            parent.removeView(mCurrentView);
        }
        Utility.setupKeyboardAutoHidden(mCurrentView, getActivity());

        return mCurrentView;
    }

    /**
     * Collects all the Views in the fragment and initializes the
     * transaction.
     */
    private void initUI() {
        // Initialize transaction tracking
        if (keeper == null) {
            keeper = new TransactionBuilder();
            keeper.addObserver(this);
        }

        // Acquire the layout that will hold all of our user bubbles.
        mBubbleFrameLayout = (FrameLayout) mCurrentView
                .findViewById(R.id.layout_send_frag_bubbles);

        // Clicking in the blank space on this layout should close the
        // options panel on each bubble
        mBubbleFrameLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                // Un-expand small bubbles
                for (UserInfo user : keeper) {
                    RadarUserBubble view = mUserBubbles.get(user);
                    if (view != null) {
                        view.switchExpandPanel(false);
                    } else {
                        Log.e(TAG, "No bubble for user: "
                                + user.getUserName());
                    }
                }

                // Catch the focus (from TotalAmount EditText)
                mBubbleFrameLayout.requestFocus();
            }
        });

        // Find the Views we need to make this UI work.
        mSelfBubble = (RadarUserBubble) mCurrentView
                .findViewById(R.id.user_bubble_myself);
        mSelectCountText = (TextView) mCurrentView
                .findViewById(R.id.send_num_of_people);
        mTotalAmount = (EditText) mCurrentView
                .findViewById(R.id.edit_text_total_amount);
        mSendMoneyButton = (Button) mCurrentView
                .findViewById(R.id.send_money_next);
        mGroupNote = (EditText) mCurrentView
                .findViewById(R.id.group_note);
        mResetButton = (Button) mCurrentView
                .findViewById(R.id.btn_radar_view_cross);

        // Listen for transaction resets
        mResetButton.setOnClickListener(this);

        // Nobody has their amount locked before a transaction is created.
        keeper.setTotal(BigDecimal.valueOf(0));

        // Create a UserInfo for the current user.
        myUserInfo = new UserInfo(Constants.userName, true);
        myUserInfo.setUserId(new Random().nextInt());
        keeper.add(myUserInfo);

        // Configure the self bubble for the current user
        BubbleListener listener = new BubbleListener();
        mSelfBubble.setUserInfo(myUserInfo);
        mSelfBubble.setEditListener(listener);
        mSelfBubble.setSelectListener(listener);
        mSelfBubble.setDeselectListener(listener);
        mSelfBubble.setLockListener(listener);

        // Self bubble should be treated just like any other bubble
        mUserBubbles = new HashMap<>();
        mUserBubbles.put(myUserInfo, mSelfBubble);

        // Listen for total amount inputs initially
        ignoreAmountChanges = false;
        TotalListener totalListener = new TotalListener();
        mTotalAmount.setOnFocusChangeListener(totalListener);
        mTotalAmount.setOnEditorActionListener(totalListener);

        // Configure the group message field
        NoteListener noteListener = new NoteListener();
        mGroupNote.setOnFocusChangeListener(noteListener);
        mGroupNote.setOnEditorActionListener(noteListener);
    }

    /**
     * Called when the status of the transaction keeper changes.
     *
     * @param o    The transaction keeper.
     * @param data Not used.
     */
    public final void update(final Observable o, final Object data) {
        if (o == null) {
            Log.e(TAG, "Received update from " + o);
            return;
        }

        // Make sure only the keeper is updating us.
        if (!(o instanceof TransactionBuilder)
                || !((TransactionBuilder) o).equals(keeper)) {
            Log.e(TAG, "Received update from wrong object: " + o);
            return;
        }

        String debug = String.format("Selected Users: %d "
                        + "| Total: %s", keeper.selectedUsers(),
                keeper.total().toString());
        Log.d(TAG, debug);
        for (UserInfo user : mUserBubbles.keySet()) {
            Log.d(TAG, user.getUserName() + " has "
                    + user.getAmountOfMoney());
        }

        // Amount should only be accessible if user's are selected
        if (keeper.selectedUsers() > 0) {
            mTotalAmount.setEnabled(true);
            Log.d(TAG, "Users selected, enabling total field");
        } else {
            mTotalAmount.setEnabled(false);
            Log.d(TAG, "No users selected, disabling and "
                    + "resetting total field");
        }

        // Update the number of selected users
        mSelectCountText.setText("" + keeper.selectedUsers());

        // Enable the next button if a valid transaction has been created
        String enabled;
        Log.d(TAG, "keeper total comparison: "
                + keeper.total().compareTo(BigDecimal.valueOf(0)));
        if (keeper.selectedUsers() > 0
                && keeper.total().compareTo(BigDecimal.valueOf(0)) > 0) {
            mSendMoneyButton.setEnabled(true);
            enabled = "enabled";
        } else {
            mSendMoneyButton.setEnabled(false);
            enabled = "disabled";
        }
        Log.d(TAG, "Next button " + enabled);

        // The total amount text field should match the keeper
        mTotalAmount.setText(keeper.total().toString());
        if (keeper.total().compareTo(BigDecimal.valueOf(0)) == 0) {
            mTotalAmount.setText("");
        }

        // The group not should match the keeper's
        mGroupNote.setText(keeper.getGeneralMessage());
    }

    /**
     * Removes a user's from the radar view.
     *
     * @param user The user to be removed.
     */
    public final void removeUserFromView(final UserInfo user) {
        // Kill the bubble visually, remove it from the list,
        // and finally remove the user from the transaction tracker.
        mBubbleFrameLayout.removeView(mUserBubbles.get(user));
        mUserBubbles.remove(user);
        keeper.remove(user);
    }

    /**
     * Adds a user to the radar.
     *
     * @param contactName The name of the user to be added.
     */
    public final void addContactToView(final String contactName) {
        UserInfo addedContact = generateBubbles(contactName);
        if (addedContact == null) {
            Log.e(TAG, "Failed to add " + contactName);
            return;
        }
    }

    /**
     * Creates and displays a bubble for the given user on the radar.
     *
     * @param username The user to create a bubble for.
     * @return The UserInfo associated with the bubble.
     */
    @SuppressLint("RtlHardcoded")
    private UserInfo generateBubbles(final String username) {
        Log.d(TAG, "starting generating bubble for: " + username);

        // There is a limit to the number of bubbles we can handle
        if (mUserBubbles.size() > PositionHandler.MAX_USER_SUPPORTED) {
            Log.e(TAG, "Maximum user quantity exceed!");
            return null;
        }

        // Calculate the size of the bubble
        int frameHeight = mBubbleFrameLayout.getHeight();
        int frameWidth = mBubbleFrameLayout.getWidth();
        int widgetWidth = mSelfBubble.getWidth();

        Random random = new Random();
        FrameLayout.LayoutParams params;

        // Generate the position of the bubble in radar.
        // Bubble positions have been preset based on the number of users.
        float[] pos = {PositionHandler
                .RAND_BUBBLE_CENTER_POS_X[mUserBubbles.size()],
                PositionHandler
                        .RAND_BUBBLE_CENTER_POS_Y[mUserBubbles.size()]};
        pos[0] = pos[0] * frameWidth - widgetWidth / 2;
        pos[1] = pos[1] * frameHeight - widgetWidth / 2;
        Log.d(TAG, "x=" + pos[0] + ", y=" + pos[1]);

        // Create layout parameters to place the bubble in the
        // generated position.
        params = new FrameLayout
                .LayoutParams(mSelfBubble.getLayoutParams());
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.setMargins((int) pos[0], (int) pos[1], 0, 0);

        // Generate this bubble's user information and associate
        // it with the user
        UserInfo info = new UserInfo(username, false);
        info.setUserId(random.nextInt());
        RadarUserBubble ruv = new RadarUserBubble(getActivity(), info);
        mBubbleFrameLayout.addView(ruv, params);

        // Add the user to the transaction keeper
        keeper.add(info);

        // Associate the bubble with the current user
        BubbleListener listener = new BubbleListener();
        ruv.setEditListener(listener);
        ruv.setLockListener(listener);
        ruv.setSelectListener(listener);
        ruv.setDeselectListener(listener);
        mUserBubbles.put(info, ruv);
        return info;
    }

    @Override
    public final void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public final void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Unused interaction interface for this fragment.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Is not used.
         *
         * @param uri Is not used.
         */
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public final Loader<Void> onCreateLoader(final int arg0,
                                             final Bundle arg1) {
        // Do nothing
        return null;
    }

    @Override
    public final void onLoadFinished(final Loader<Void> arg0, final Void arg1) {
        // Do nothing
    }

    @Override
    public final void onLoaderReset(final Loader<Void> arg0) {
        // Do nothing
    }

    @Override
    public final void onResume() {
        super.onResume();
    }

    /**
     * Displays the popup window which allows the user to edit transaction
     * details specifically for the given user.
     *
     * @param userInfo The user for this popup window.
     */
    private void showBigBubble(final UserInfo userInfo) {
        View popupView = getActivity().getLayoutInflater()
                .inflate(R.layout.big_bubble, null);
        mBigBubble = new BigBubblePopupWindow(popupView, userInfo);
        mBigBubble.setTouchable(true);
        mBigBubble.setOutsideTouchable(true);

        // Configure listeners to pull data out of the
        PopupListener listener = new PopupListener(userInfo);
        mBigBubble.setOnDismissListener(listener);
        mBigBubble.setFocusListener(listener);
        mBigBubble.setEditorListener(listener);

        // Display the popup window
        mBigBubble.showAtLocation(
                getActivity().findViewById(R.id.radar_view_title),
                Gravity.CENTER | Gravity.TOP, 0, POPUP_Y);
    }

    /**
     * Updates the total amount text field to match the given amount.
     *
     * @param total The total to be displayed.
     */
    private void matchTotal(final BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.valueOf(0)) == 0) {
            mTotalAmount.setText("");
            return;
        }

        mTotalAmount.setText(total.toString());
    }

    /**
     * Compiles a list of information necessary in order to send
     * this transaction to the JoinPay API.
     *
     * @return A List of Strings describing the transaction.
     */
    public abstract ArrayList<String[]> getPaymentInfo();

    /**
     * @return The group message text field.
     * @see #mGroupNote
     */
    protected final EditText getmGroupNote() {
        return mGroupNote;
    }

    /**
     * @return The transaction keeper.
     * @see #keeper
     */
    protected final TransactionBuilder getKeeper() {
        return keeper;
    }

    /**
     * @return The user bubbles for this fragment.
     * @see #mUserBubbles
     */
    protected final Map<UserInfo, RadarUserBubble> getmUserBubbles() {
        return mUserBubbles;
    }

    /**
     * @return The current user.
     * @see #myUserInfo
     */
    protected final UserInfo getMyUserInfo() {
        return myUserInfo;
    }

    /**
     * @return The total amount text field.
     * @see #mTotalAmount
     */
    protected final EditText getmTotalAmount() {
        return mTotalAmount;
    }

    /**
     * Helper class for dealing with the edit amount popups.
     */
    private class PopupListener implements OnFocusChangeListener,
            OnDismissListener, TextView.OnEditorActionListener {

        /**
         * For tagging logs from this class.
         */
        private static final String TAG = "popup_listener";

        /**
         * The user associated with the popup.
         */
        private UserInfo popupUser;

        /**
         * Constructs a new popup for the given user.
         *
         * @param user The user for the popup.
         */
        PopupListener(final UserInfo user) {
            popupUser = user;
        }

        @Override
        public void onDismiss() {
            Log.d(TAG, "Popup dismissed");
        }

        @Override
        public boolean onEditorAction(final TextView v,
                                      final int actionId,
                                      final KeyEvent event) {
            // Update the user's amount when user presses DONE
            if (actionId == EditorInfo.IME_ACTION_DONE) {

                // Create a DecimalFormat that fits your requirements
                DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                symbols.setGroupingSeparator(',');
                symbols.setDecimalSeparator('.');
                String pattern = "#,##0.0#";
                DecimalFormat decimalFormat = new DecimalFormat(pattern,
                        symbols);
                decimalFormat.setParseBigDecimal(true);

                // parse the string
                String total = v.getText().toString();
                try {
                    BigDecimal amount = (BigDecimal) decimalFormat.parse(total);
                    Log.d(TAG, "Parse total: " + amount.toString());

                    // Should attempt to change the amount for the user.
                    if (!keeper.setAmount(popupUser, amount)) {
                        Log.e(TAG, "Amount: " + total + " not accepted.");
                        Toast.makeText(mCurrentView.getContext(),
                                "Amount not valid.", Toast.LENGTH_SHORT).show();
                        v.setText("");
                    }
                } catch (ParseException e) {
                    String error = String.format("Could not parse total "
                            + "\"%s\":%s", total, e.getMessage());
                    Log.e(TAG, error);
                }
            }
            return false;
        }

        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            if (v == null || !(v instanceof EditText)) {
                Log.e(TAG, "Only support EditTexts");
                return;
            }

            // Capture the text field.
            EditText text = (EditText) v;
            if (hasFocus) {
                return;
            }

            // Set back to the user's amount
            Log.d(TAG, "Amount lost focus.  Setting to user total");
            text.setText(popupUser.getAmountOfMoney().toString());
        }
    }

    /**
     * Helper class for handling the input events that can come from
     * user radar bubbles.
     */
    private class BubbleListener implements OnEditListener,
            OnLockListener, OnSelectListener, OnDeselectListener {

        @Override
        public void onDeselect(final UserInfo user) {
            Log.d(TAG, "deselect bubble for user: " + user.getUserName());
            if (!keeper.selectUser(user, false)) {
                Log.e(TAG, "Failed to deselect user: " + user.getUserName());
            }
        }

        @Override
        public void onEdit(final UserInfo user) {
            if (user == null) {
                Log.e(TAG, "Cannot edit null user");
                return;
            }

            // Editing a user's value requires that they be locked.
            if (keeper.lockUser(user, true)) {
                // Display the transaction edit pop up bubble for this user.
                showBigBubble(user);
            } else {
                Log.e(TAG, "Failed to edit " + user.getUserName());
            }
        }

        @Override
        public void onSelect(final UserInfo user) {
            String selecting;
            boolean beingSelected;
            if (user.isSelected()) {
                selecting = "deselecting";
                beingSelected = false;
            } else {
                selecting = "selecting";
                beingSelected = true;
            }
            Log.d(TAG, selecting + " user: " + user.getUserName());
            if (!keeper.selectUser(user, beingSelected)) {
                Log.e(TAG, "Failed selection on user: "
                        + user.getUserName());
            }
        }

        @Override
        public void onLock(final UserInfo user) {
            // Lock the user.
            String locking;
            boolean beingLocked;
            if (user.isLocked()) {
                locking = "unlocking";
                beingLocked = false;
            } else {
                locking = "locking";
                beingLocked = true;
            }

            Log.d(TAG, locking + " user: " + user.getUserName());
            if (!keeper.lockUser(user, beingLocked)) {
                Log.e(TAG, "Failed to lock user: " + user.getUserName());
            }
        }
    }

    /**
     * Helper class for dealing with all the events in the transaction
     * total text field.
     */
    private class TotalListener implements OnFocusChangeListener,
            TextView.OnEditorActionListener {
        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            if (hasFocus) {
                return;
            }
            matchTotal(keeper.total());
        }

        @Override
        public boolean onEditorAction(final TextView v,
                                      final int actionId,
                                      final KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Create a DecimalFormat that fits your requirements
                DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                symbols.setGroupingSeparator(',');
                symbols.setDecimalSeparator('.');
                String pattern = "#,##0.0#";
                DecimalFormat decimalFormat = new DecimalFormat(pattern,
                        symbols);
                decimalFormat.setParseBigDecimal(true);

                // parse the string
                String total = mTotalAmount.getText().toString();
                try {
                    BigDecimal newTotal = (BigDecimal) decimalFormat
                            .parse(total);
                    Log.d(TAG, "Parse total: " + newTotal.toString());

                    // Should attempt to change the total for the
                    // transaction
                    if (!keeper.setTotal(newTotal)) {
                        Log.e(TAG, "Total: " + total + " not accepted.");
                        Toast.makeText(mCurrentView.getContext(),
                                "Total must be more than the locked total",
                                Toast.LENGTH_SHORT).show();
                        mTotalAmount.setText(keeper.total().toString());
                    }
                } catch (ParseException e) {
                    String error = String.format("Could not parse total "
                            + "\"%s\":%s", total, e.getMessage());
                    Log.e(TAG, error);
                }
            }
            return false;
        }
    }

    /**
     * Helper class for handling input events involving the group message
     * text field.
     */
    private class NoteListener implements OnFocusChangeListener,
            TextView.OnEditorActionListener {

        /**
         * For tagging logs from this class.
         */
        private static final String TAG = "note_listener";

        @Override
        public boolean onEditorAction(final TextView v,
                                      final int actionId,
                                      final KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                this.onFocusChange(v, false);
            }
            return false;
        }

        @Override
        public void onFocusChange(final View v,
                                  final boolean hasFocus) {
            if (hasFocus) {
                return;
            }

            // Update the group message.
            if (v != null && v instanceof EditText) {
                Log.d(TAG, "Updating group message.");
                String groupNote = ((EditText) v).getText().toString();
                keeper.setGeneralMessage(groupNote);
            }
        }
    }
}
