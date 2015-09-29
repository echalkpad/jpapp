package com.soontobe.joinpay;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.DecimalFormat;

/**
 * AccountJSONAdapter translates the data retrieved from the Citi bank APIs into a row_account
 * layout.  This
 *
 * Created by Dale Avery on 9/29/2015.
 */
public class AccountJSONAdapter extends BaseAdapter {

    // Tags for pulling the appropriate data out of the JSON
    private static final String TAG_NAME = "account_name";
    private static final String TAG_NUMBER = "account_number";
    private static final String TAG_BALANCE = "balance";
    private static final String TAG_FIRST_NAME = "first_name";
    private static final String TAG_LAST_NAME = "last_name";

    // Store the account holder's name because it isn't stored in each account JSON
    private String accountHolderName;

    private Context mContext;
    private LayoutInflater mInflater;
    private JSONArray mJsonArray;

    /**
     * Creates an AccountJSONAdapter with the given parameters.
     * @param context The context in which the AccountJSONAdapter operates
     * @param inflater The LayoutInflater for which the AccountJSONAdapter is adapting information
     */
    public AccountJSONAdapter(Context context, LayoutInflater inflater) {
        mContext = context;
        mInflater = inflater;
        mJsonArray = new JSONArray();
        accountHolderName = "";
    }

    @Override
    public int getCount() {
        return mJsonArray.length();
    }

    @Override
    public Object getItem(int position) {
        return mJsonArray.optJSONObject(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Updates the Adapter's dataset and notifies any Views that the set has been changed.
     * @param jsonArray The updated dataset
     */
    public void updateData(JSONArray jsonArray, String name) {
        mJsonArray = jsonArray;
        accountHolderName = name;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        // check if the view already exists
        // if so, no need to inflate and findViewById again!
        if (convertView == null) {

            // Inflate the custom row layout from your XML.
            convertView = mInflater.inflate(R.layout.row_account, null);

            // create a new "Holder" with subviews
            holder = new ViewHolder();
            holder.accNameTextView = (TextView) convertView.findViewById(R.id.accNameTextView);
            holder.accNumTextView = (TextView) convertView.findViewById(R.id.accNumTextView);
            holder.accHolderNameTextView = (TextView) convertView.findViewById(R.id.accHolderTextView);
            holder.accBalanceTextView = (TextView) convertView.findViewById(R.id.accBalTextView);

            // hang onto this holder for future recyclage
            convertView.setTag(holder);
        } else {

            // skip all the expensive inflation/findViewById
            // and just get the holder you already made
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the current book's data in JSON form
        JSONObject jsonObject = (JSONObject) getItem(position);

        // Grab the account information from the JSON
        String name = jsonObject.optString(TAG_NAME, "@string/citi_fallback_name");
        String number = jsonObject.optString(TAG_NUMBER, "@string/citi_fallback_number");
        String balance = jsonObject.optString(TAG_BALANCE, "@string/citi_fallback_balance");
        String accHolder = jsonObject.optString(TAG_FIRST_NAME, "") + " " + jsonObject.optString(TAG_LAST_NAME, "");

        // Currently, APIs dont store holder name in the account object, so fill it in here.
        if(accHolder.length() < 3) accHolder = accountHolderName;

        // Format the balance to a pretty string
        double amount = Double.parseDouble(balance);
        DecimalFormat formatter = new DecimalFormat("#,##0.00"); // TODO should be set by a currency type
        String finalBalance = formatter.format(amount);

        // Send strings to the TextViews for display
        holder.accNameTextView.setText(name);
        holder.accNumTextView.setText(number);
        holder.accBalanceTextView.setText(finalBalance);
        holder.accHolderNameTextView.setText(accHolder);

        return convertView;
    }

    // this is used so you only ever have to do
    // inflation and finding by ID once ever per View
    private static class ViewHolder {
        public TextView accNameTextView;
        public TextView accNumTextView;
        public TextView accHolderNameTextView;
        public TextView accBalanceTextView;
    }
}
