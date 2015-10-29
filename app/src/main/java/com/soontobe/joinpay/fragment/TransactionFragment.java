package com.soontobe.joinpay.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

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
     * The Layout holding all the user bubbles.
     */
    private FrameLayout mBubbleFrameLayout;

    /**
     * A list of user bubbles.
     */
    protected static Map<UserInfo, RadarUserBubble> mUserBubbles;

    /**
     * The bubble corresponding to the current user.
     */
    protected static RadarUserBubble mSelfBubble;

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
    protected static TextView mSelectCountText;

    /**
     * Resets the transaction.
     */
    protected Button mResetButton;

    /**
     * Field for inputting a total amount for a transaction.
     */
    public static EditText mTotalAmount;

    /**
     * The "next" button.  Selected once users have been selected
     * and the amounts are set.
     */
    public static Button mSendMoneyButton;

    /**
     * Text field for the note to add to the transaction.
     */
    protected static EditText mGroupNote;

    /**
     * The UserInfo for the current user.
     */
    public static UserInfo myUserInfo;

    /**
     * Keeps the transaction record keeping out of the UI.
     */
    protected TransactionBuilder keeper;

    /**
     * Constructs a new TransactionFragment.
     */
    public TransactionFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public final void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_radar_view_cross:
                keeper.resetTransaction();
                mTotalAmount.setText("");
                break;
            default:
                Log.d(TAG, "Received click from unknown source");
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (mCurrentView == null) {
            mCurrentView = inflater.inflate(R.layout.main_tab, container, false);
            initUI();
        }

        // Create ViewGroup if the object does not exist, otherwise use the current one.
        ViewGroup parent = (ViewGroup) mCurrentView.getParent();
        if (parent != null) {
            parent.removeView(mCurrentView);
        }
        Utility.setupKeyboardAutoHidden(mCurrentView, getActivity());

        return mCurrentView;
    }

    private void initUI() {
        // Initialize transaction tracking
        if(keeper == null) {
            keeper = new TransactionBuilder();
            keeper.addObserver(this);
        }

        // Acquire the layout that will hold all of our user bubbles.
        mBubbleFrameLayout = (FrameLayout) mCurrentView.findViewById(R.id.layout_send_frag_bubbles);

        // Clicking in the blank space on this layout should close the
        // options panel on each bubble
        mBubbleFrameLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Un-expand small bubbles
                for (UserInfo user : keeper) {
                    RadarUserBubble view = mUserBubbles.get(user);
                    if (view != null) {
                        view.switchExpandPanel(false);
                    } else {
                        Log.e(TAG, "No bubble for user: " + user.getUserName());
                    }
                }

                // Catch the focus (from TotalAmount EditText)
                mBubbleFrameLayout.requestFocus();
            }
        });

        // Find the Views we need to make this UI work.
        mSelfBubble = (RadarUserBubble) mCurrentView.findViewById(R.id.user_bubble_myself);
        mSelectCountText = (TextView) mCurrentView.findViewById(R.id.send_num_of_people);
        mTotalAmount = (EditText) mCurrentView.findViewById(R.id.edit_text_total_amount);
        mSendMoneyButton = (Button) mCurrentView.findViewById(R.id.send_money_next);
        mGroupNote = (EditText) mCurrentView.findViewById(R.id.group_note);
        mResetButton = (Button) mCurrentView.findViewById(R.id.btn_radar_view_cross);

        // Listen for transaction resets
        mResetButton.setOnClickListener(this);

        // Nobody has their amount locked before a transaction is created.
        keeper.setTotal(BigDecimal.valueOf(0));

        // Create a UserInfo for the current user.
        myUserInfo = new UserInfo(Constants.userName, true);
        myUserInfo.setUserId(new Random().nextInt());
        keeper.add(myUserInfo);

        mSelfBubble.setUserInfo(myUserInfo);
        mSelfBubble.setEditListener(new EditButtonHandler());
        mSelfBubble.setSelectListener(new SelectUserOnClickListener());
        mSelfBubble.setDeselectListener(new DeselectUserOnClickListener());
        mSelfBubble.setLockListener(new LockButtonOnClickListener());

        mUserBubbles = new HashMap<>();
        mUserBubbles.put(myUserInfo, mSelfBubble);
        mTotalAmount.setOnFocusChangeListener(new OnTotalMoneyFocusChangeListener());
        mTotalAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Create a DecimalFormat that fits your requirements
                DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                symbols.setGroupingSeparator(',');
                symbols.setDecimalSeparator('.');
                String pattern = "#,##0.0#";
                DecimalFormat decimalFormat = new DecimalFormat(pattern, symbols);
                decimalFormat.setParseBigDecimal(true);

                // parse the string
                String total = mTotalAmount.getText().toString();
                try {
                    BigDecimal newTotal = (BigDecimal) decimalFormat.parse(total);
                    Log.d(TAG, "Parse total: " + newTotal.toString());

                    // Should attempt to change the total for the transaction
                    if (!keeper.setTotal(newTotal)) {
                        Log.e(TAG, "Total: " + total + " not accepted.");
                    }
                } catch (ParseException e) {
                    String error = String.format("Could not parse total "
                                    + "\"%s\":%s", total, e.getMessage());
                    Log.e(TAG, error);
                }

                // TODO there are better ways to format this string.
            }
        });

        mGroupNote.setOnFocusChangeListener(new OnGroupNoteFocusChangeListener());
    }

    public final void update(final Observable o, final Object data) {
        if(o == null) {
            Log.e(TAG, "Received update from " + o);
            return;
        }

        if(!(o instanceof TransactionBuilder)
                || !((TransactionBuilder)o).equals(keeper)) {
            Log.e(TAG, "Received update from wrong object: " + o);
            return;
        }

        String debug = String.format("Selected Users: %d | Total: %s", keeper.selectedUsers(),
                keeper.total().toString());
        Log.d(TAG, debug);
        for(UserInfo user: mUserBubbles.keySet()) {
            Log.d(TAG, user.getUserName() + " has " + user.getAmountOfMoney());
        }

        // Amount should only be accessible if user's are selected
        if(keeper.selectedUsers() > 0) {
            mTotalAmount.setEnabled(true);
            Log.d(TAG, "Users selected, enabling total field");
        } else {
            mTotalAmount.setEnabled(false);
            Log.d(TAG, "No users selected, disabling and resetting total field");
        }

        // Update the number of selected users
        mSelectCountText.setText("" + keeper.selectedUsers());

        // Enable the next button if a valid transaction has been created
        String enabled;
        Log.d(TAG, "keeper total comparison: " + keeper.total().compareTo(BigDecimal.valueOf(0)));
        if(keeper.selectedUsers() > 0 && keeper.total().compareTo(BigDecimal.valueOf(0)) > 0) {
            mSendMoneyButton.setEnabled(true);
            enabled = "enabled";
        } else {
            mSendMoneyButton.setEnabled(false);
            enabled = "disabled";
        }
        Log.d(TAG, "Next button " + enabled);
    }

    /**
     * Removes a user's from the radar view.
     *
     * @param user The user to be removed.
     */
    public void removeUserFromView(UserInfo user) {
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
    public void addContactToView(String contactName) {
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
    public UserInfo generateBubbles(String username) {
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
        float pos[] = {PositionHandler.RAND_BUBBLE_CENTER_POS_X[mUserBubbles.size()],
                PositionHandler.RAND_BUBBLE_CENTER_POS_Y[mUserBubbles.size()]};
        pos[0] = pos[0] * frameWidth - widgetWidth / 2;
        pos[1] = pos[1] * frameHeight - widgetWidth / 2;
        Log.d(TAG, "x=" + pos[0] + ", y=" + pos[1]);

        // Create layout parameters to place the bubble in the generated position.
        params = new FrameLayout.LayoutParams(mSelfBubble.getLayoutParams());
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.setMargins((int) pos[0], (int) pos[1], 0, 0);

        // Generate this bubble's user information and associate it with the user
        UserInfo info = new UserInfo(username, false);
        info.setUserId(random.nextInt());
        RadarUserBubble ruv = new RadarUserBubble(getActivity(), info);
        mBubbleFrameLayout.addView(ruv, params);

        // Add the user to the transaction keeper
        keeper.add(info);

        // Associate the bubble with the current user
        ruv.setEditListener(new EditButtonHandler());
        ruv.setLockListener(new LockButtonOnClickListener());
        ruv.setSelectListener(new SelectUserOnClickListener());
        ruv.setDeselectListener(new DeselectUserOnClickListener());
        mUserBubbles.put(info, ruv);
        return info;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    @Override
    public Loader<Void> onCreateLoader(int arg0, Bundle arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Void> arg0, Void arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLoaderReset(Loader<Void> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    /**
     * Displays the popup window which allows the user to edit transaction
     * details specifically for the given user.
     *
     * @param userInfo The user for this popup window.
     */
    public void showBigBubble(UserInfo userInfo) {
        View popupView = getActivity().getLayoutInflater().inflate(R.layout.big_bubble, null);
        mBigBubble = new BigBubblePopupWindow(popupView, userInfo);
        mBigBubble.setTouchable(true);

        // Configure listeners to pull data out of the
        mBigBubble.setOnDismissListener(new PopupListener());

        // Display the popup window
        mBigBubble.showAtLocation(
                getActivity().findViewById(R.id.radar_view_title),
                Gravity.CENTER | Gravity.TOP, 0, 200);
    }

    private class PopupListener implements OnDismissListener, TextWatcher {

        /**
         * For tagging logs from this class.
         */
        private final String TAG = "big_bubble";

        @Override
        public void onDismiss() {
            // TODO fix this
            Log.d(TAG, "Popup dismissed");
            // BigBubble will have updated the user's amount to reflect
            // the input value.
            UserInfo userInfo = mBigBubble.getUserInfo();

            // Check to make sure user exists
            if (!keeper.contains(userInfo)) {
                Log.e(TAG, "User not tracked: " + userInfo);
                return;
            }

            // Attempt to update the keeper's record of the user.
            Log.d(TAG, "Updating user amount for user: " + userInfo);
            // TODO update amount
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Do nothing
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Pull the text out of the popup window and update the user amount
            //TODO implement this
            Log.d(TAG, "String from popup: \"" + s.toString() + "\"");
        }
    }

    /**
     * This is a handler for when the user presses the edit button on
     * a user bubble.
     */
    private class EditButtonHandler implements OnEditListener {

        @Override
        public void onEdit(UserInfo user) {
            // Display the transaction edit pop up bubble for this user.
            showBigBubble(user);
        }
    }

    /**
     * Customized OnClickListener listening to the button-click of
     * `LOCK` button in the expanded small bubble of each user.
     */
    private class LockButtonOnClickListener implements OnLockListener {

        @Override
        public void onLock(UserInfo user) {
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
     * Customized OnClickListener listening to the button-click of
     * small bubble of each user
     */
    private class SelectUserOnClickListener implements OnSelectListener {

        @Override
        public void onSelect(UserInfo user) {
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
    }

    /**
     * Customized OnClickListener listening to the button-click of
     * user de-selection action
     */
    private class DeselectUserOnClickListener implements OnDeselectListener {

        @Override
        public void onDeselect(UserInfo user) {
            Log.d(TAG, "deselect bubble for user: " + user.getUserName());
            if (!keeper.selectUser(user, false)) {
                Log.e(TAG, "Failed to deselect user: " + user.getUserName());
            }
        }
    }

    public abstract ArrayList<String[]> getPaymentInfo();

    private class OnTotalMoneyFocusChangeListener implements OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus)
                return;

            // TODO should update total here
        }
    }

    /**
     * This helper class handles updating the general message for the
     * transaction.
     */
    private class OnGroupNoteFocusChangeListener implements
            OnFocusChangeListener {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus)
                return;

            // Update the group message
            String groupNote = mGroupNote.getEditableText().toString();
            keeper.setGeneralMessage(groupNote);
        }
    }
}
