package com.soontobe.joinpay.fragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.net.Uri;
import android.os.Build;
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
import android.view.ViewTreeObserver;
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
import com.soontobe.joinpay.widget.RadarUserView;
import com.soontobe.joinpay.widget.RadarUserView
        .OnCenterButtonClickedListener;
import com.soontobe.joinpay.widget.RadarUserView
        .OnDeselectButtonClickedListener;
import com.soontobe.joinpay.widget.RadarUserView
        .OnEditButtonClickedListener;
import com.soontobe.joinpay.widget.RadarUserView
        .OnLockButtonClickedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * TransactionFragment describes the process of selecting a list of users
 * and creating a shared transaction.
 */
public abstract class TransactionFragment extends Fragment
        implements LoaderCallbacks<Void> {

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
    protected static Map<UserInfo, RadarUserView> mUserBubbles;

    /**
     * The bubble corresponding to the current user.
     */
    protected static RadarUserView mSelfBubble;

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
        keeper = new TransactionBuilder();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStop() {

        super.onStop();
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
        // Acquire the layout that will hold all of our user bubbles.
        mBubbleFrameLayout = (FrameLayout) mCurrentView.findViewById(R.id.layout_send_frag_bubbles);
        mBubbleFrameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new MyOnGlobalLayoutChgListener());

        // Clicking in the blank space on this layout should close the
        // options panel on each bubble
        mBubbleFrameLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Un-expand small bubbles
                for(UserInfo user : keeper) {
                    RadarUserView view = mUserBubbles.get(user);
                    view.switchExpandPanel(false);
                }

                // Catch the focus (from TotalAmount EditText)
                mBubbleFrameLayout.requestFocus();
            }
        });

        // Find the Views we need to make this UI work.
        mSelfBubble = (RadarUserView) mCurrentView.findViewById(R.id.user_bubble_myself);
        mSelectCountText = (TextView) mCurrentView.findViewById(R.id.send_num_of_people);
        mTotalAmount = (EditText) mCurrentView.findViewById(R.id.edit_text_total_amount);
        mSendMoneyButton = (Button) mCurrentView.findViewById(R.id.send_money_next);
        mGroupNote = (EditText) mCurrentView.findViewById(R.id.group_note);

        // Nobody has their amount locked before a transaction is created.
        keeper.setTotalInPennies(0);

        // Create a UserInfo for the current user.
        myUserInfo = new UserInfo();
        myUserInfo.setUserId(new Random().nextInt());
        myUserInfo.setUserName(Constants.DemoMyName);
        myUserInfo.setMyself(true);
        keeper.add(myUserInfo);


        mSelfBubble.setUserInfo(myUserInfo);
        mSelfBubble.setEditBtnClickedListener(new OnEditButtonClickedListener() {
            @Override
            public void OnClick(View v) {
                // Editing the value means the user should be locked
                if(!keeper.lockUser(myUserInfo, true)) {
                    Log.e(TAG, "Failed to lock current user for editting");
                    return;
                }

                // Show the bubble that lets you manually change the user's value
                showBigBubble(myUserInfo);
            }
        });
        mSelfBubble.setCenterBtnClickedListener(new OnCenterButtonClickedListener() {
            @Override
            public void OnClick(View v, boolean isSelected) {
                Log.d(TAG, "selected current user");
                keeper.selectUser(myUserInfo, true);
            }
        });
        mSelfBubble.setDeselectBtnClickedListener(new OnDeselectButtonClickedListener() {
            @Override
            public void OnClick(View v) {
                Log.d(TAG, "self bubble deselect");
                myUserInfo.setSelected(false);
            }
        });

        mUserBubbles = new HashMap<>();
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
                // Should attempt to change the total for the transaction
                int total = stringToPennies(mTotalAmount.getText().toString());
                if(!keeper.setTotalInPennies(total)) {
                    Log.e(TAG, "Total: " + total + " not accepted.");
                }

                // Display the beautified total to the user.
                mTotalAmount.setText(keeper.total().toString());

                // TODO their are better ways to parse money strings.
            }
        });

        mGroupNote.setOnFocusChangeListener(new OnGroupNoteFocusChangeListener());
    }

    //convert string that has dollars and decimal point to integer of pennies
    public static int stringToPennies(String str) {
        int pennies = 0;
        int int_dollars = 0;
        int int_change = 0;
        //find decimal position
        int pos = str.indexOf('.');
        if (pos >= 0) {                                                                    //input has decimal, ie: $15.10
            String dollars = str.substring(0, pos);
            String change = str.substring(pos + 1);
            try {
                int_dollars = Integer.parseInt(dollars);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse dollars: " + dollars);
            }
            if (change.length() <= 1) {
                //input has 1 decimal... ie: $15.1
                change += '0';
            }if (change.length() >= 3) {
                //only 2 digits allowed... no fractions of a penny
                change = change.substring(0, 2);
            }
            try {
                int_change = Integer.parseInt(change);
            } catch (Exception e) {
            }
            pennies = int_dollars * 100 + int_change;
        } else {
            //input has NO decimal, ie: $15
            try {
                int_dollars = Integer.parseInt(str);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse string: " + str);
            }
        }
        pennies = int_dollars * 100 + int_change;
        //Log.d("money", "PENNIES: " + pennies);
        return pennies;
    }

    //convert integer of pennies to string with dollar and decimal point
    public static String penniesToString(int pennies) {
        String str = "";
        String dollars = "0";
        String change = "0";
        String str_pennies = Integer.toString(pennies);
        if (pennies >= 100) {                                                        //there are dollars, ie: 100 = $1.00
            dollars = str_pennies.substring(0, str_pennies.length() - 2);
            change = str_pennies.substring(str_pennies.length() - 2);
        } else if (pennies >= 10) {                                                    //there are NO dollars, 2 digits, ie: 20 = $0.20
            dollars = "0";
            change = str_pennies;
        } else {                                                                    //there are NO dollars, 1 digit, ie: 5 = $0.5
            dollars = "0";
            change = "0" + str_pennies;
        }

        str = dollars + '.' + change;
        //Log.d("money", "input: " + pennies + ", (dollars): " + dollars + ", (change): " + change);
        return str;
    }

    /**
     * Removes a user's from the radar view.
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
        RadarUserView ruv = new RadarUserView(getActivity());
        mBubbleFrameLayout.addView(ruv, params);

        // Generate this bubble's user information and associate it with the user
        UserInfo info = new UserInfo();
        info.setUserName(username);
        info.setUserId(random.nextInt());

        // Add the user to the transaction keeper
        keeper.add(info);

        // Associate the bubble with the current user
        ruv.setUserInfo(info);
        ruv.setEditBtnClickedListener(new EditButtonHandler(info));
        ruv.setLockBtnClickedListener(new LockButtonOnClickListener(info));
        ruv.setCenterBtnClickedListener(new SelectUserOnClickListener(info));
        ruv.setDeselectBtnClickedListener(new DeselectUserOnClickListener(info));
        mUserBubbles.put(info, ruv);
//		mUserBubbles.set(position, ruv);
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
            if(!keeper.contains(userInfo)) {
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

    private class MyOnGlobalLayoutChgListener implements
            ViewTreeObserver.OnGlobalLayoutListener {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onGlobalLayout() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                mBubbleFrameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            else
                mBubbleFrameLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }

    }

    /**
     * This is a handler for when the user presses the edit button on
     * a user bubble.
     */
    private class EditButtonHandler implements
            OnEditButtonClickedListener {

        /**
         * The user associated with the bubble for this listener.
         */
        private UserInfo mUser;

        /**
         * Constructs a new EditButtonHandler.
         * @param user The user associate with this listener's bubble.
         */
        public EditButtonHandler(final UserInfo user) {
            mUser = user;
        }

        @Override
        public void OnClick(View v) {
            // Display the transaction edit pop up bubble for this user.
            showBigBubble(mUser);
        }

    }

    /**
     * Customized OnClickListener listening to the button-click of
     * `LOCK` button in the expanded small bubble of each user.
     */
    private class LockButtonOnClickListener implements
            OnLockButtonClickedListener {
        UserInfo mUser;

        /**
         * Constructs a new LockButtonOnClickListener.
         * @param user The user associated with the lock button.
         */
        public LockButtonOnClickListener(UserInfo user) {
            mUser = user;
        }

        @Override
        public void OnClick(View v, boolean isLocked) {
            String locking = (isLocked ? "locking" : "unlocking");
            Log.d(TAG, locking + " user: " + mUser.getUserName());
            if(!keeper.lockUser(mUser, isLocked)) {
                Log.e(TAG, "Failed to lock user: " + mUser.getUserName());
            }

            // TODO update state?
        }
    }

    /**
     * Customized OnClickListener listening to the button-click of
     * small bubble of each user
     */
    private class SelectUserOnClickListener implements
            OnCenterButtonClickedListener {

        /**
         * Used for tagging logs from this class.
         */
        private static final String TAG = "select_user";

        /**
         * The user associated with this select button.
         */
        private UserInfo mUser;

        /**
         * Constructs a new SelectUserOnClickListener.
         * @param user The user associated with the small bubble.
         */
        public SelectUserOnClickListener(UserInfo user) {
            mUser = user;
        }

        @Override
        public void OnClick(View v, boolean isSelected) {
            String selecting = (isSelected ? "selecting" : "deselecting");
            Log.d(TAG, selecting + " user: " + mUser.getUserName());
            if(!keeper.selectUser(mUser, isSelected)) {
                Log.e(TAG, "Failed selection on user: "
                        + mUser.getUserName());
            }

            // TODO update UI?
        }
    }

    /**
     * Customized OnClickListener listening to the button-click of
     * user de-selection action
     */
    private class DeselectUserOnClickListener implements
            OnDeselectButtonClickedListener {

        /**
         * Used to tag logs from this class.
         */
        private static final String TAG = "deselect";

        /**
         * The user associated with the bubble.
         */
        private UserInfo mUser;

        /**
         * Constructs a new DeselectUserOnClickListener.
         * @param user The user to be associated with the bubble.
         */
        public DeselectUserOnClickListener(UserInfo user) {
            mUser = user;
        }

        @Override
        public void OnClick(View v) {
            Log.d(TAG, "deselect bubble for user: " + mUser.getUserName());
            if(!keeper.selectUser(mUser, false)) {
                Log.e(TAG, "Failed to deselect user: " + mUser.getUserName());
                return;
            }
            // TODO update UI?
        }
    }

    public abstract ArrayList<String[]> getPaymentInfo();

    private class OnTotalMoneyFocusChangeListener implements OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(hasFocus)
                return;

            // TODO should update total here
        }
    }

    private class OnGroupNoteFocusChangeListener implements
            OnFocusChangeListener {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus)
                return;


            String groupNote = mGroupNote.getEditableText().toString();
            keeper.setGeneralMessage(groupNote);
        }
    }

    public void clearTransaction() {
        Log.d(TAG, "Clearing transaction");
        keeper.clear();
        // TODO update state?
    }
}
