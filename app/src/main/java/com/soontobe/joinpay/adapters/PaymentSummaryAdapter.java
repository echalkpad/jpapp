package com.soontobe.joinpay.adapters;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.Globals;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.helpers.Rest;
import com.soontobe.joinpay.model.Transaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class adapts a list of transactions into a pretty layout for a ListView.
 * Created by Dale Avery on 10/8/2015.
 */
public class PaymentSummaryAdapter extends BaseAdapter {

    /**
     * Used for tagging logs from this class.
     */
    private static final String TAG = "transaction_adapter";
    private Context context;
    private ArrayList<Transaction> values;
    private LayoutInflater mInflater;
    private Rest.httpResponseHandler mApprovalResponseHandler;

    /**
     * Constructs a new PaymentSummaryAdapter with the given data.
     *
     * @param context  The context in which the PaymentSummaryAdapter is operating.
     * @param values   The transactions that the adapter will beautify.
     * @param inflater The inflater used to inflate layouts for transactions.
     */
    public PaymentSummaryAdapter(Context context, Collection<Transaction> values,
                                 LayoutInflater inflater, Rest.httpResponseHandler approvalResponseHandler) {
        Log.d(TAG, "Constructor");
        this.context = context;
        this.values = new ArrayList<>(values);
        this.mInflater = inflater;
        this.mApprovalResponseHandler = approvalResponseHandler;
    }

    @Override
    public Object getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        Log.d(TAG, "GetView called");
        // check if the view already exists
        // if so, no need to inflate and findViewById again!
        if (convertView == null) {
            Log.d(TAG, "convertView doesn't exist, inflating layout");
            // Inflate the custom row layout from your XML.
            convertView = mInflater.inflate(R.layout.confirm_page_item, null);

            // create a new "Holder" with subviews
            holder = new ViewHolder();
            holder.groupNoteview = (TextView) convertView.findViewById(R.id.confirm_personal_note_normal2);
            holder.decNeeded = (TextView) convertView.findViewById(R.id.decisionNeeded);
            holder.payerView = (TextView) convertView.findViewById(R.id.activity_confirm_payer);
            holder.payeeView = (TextView) convertView.findViewById(R.id.activity_confirm_payee);
            holder.amountView = (TextView) convertView.findViewById(R.id.amount_confirm3);
            holder.transId = (TextView) convertView.findViewById(R.id.transacation_id);
            holder.statusView = (TextView) convertView.findViewById(R.id.payment_status);
            holder.date = (TextView) convertView.findViewById(R.id.trans_date);

            // hang onto this holder for future recyclage
            convertView.setTag(holder);
        } else {

            // skip all the expensive inflation/findViewById
            // and just get the holder you already made
            Log.d(TAG, "convertView already exists, collecting ViewHolder");
            holder = (ViewHolder) convertView.getTag();
        }

        // Collect the data to adapt to this View
        Transaction trans = (Transaction) getItem(position);
        Log.d(TAG, String.format("Building view for Transaction: %s", trans));

        // Update view to reflect transaction
        Log.d(TAG, "Updating view to match transaction");
        holder.transId.setText(trans.getId());
        holder.amountView.setText(trans.getPrettyAmount());
        holder.payerView.setText(trans.getFromUser());
        holder.payeeView.setText(trans.getToUser());
        holder.groupNoteview.setText(trans.getDescription());
        holder.date.setText(trans.prettyDate().toString());

        // Attempt to identify and show the status of the transaction
        if (trans.getStatus().equals(Transaction.STATUS.PENDING)) {
            holder.statusView.setText(context.getText(R.string.trans_pending));
            holder.statusView.setVisibility(View.VISIBLE);
        } else if (trans.getStatus().equals(Transaction.STATUS.DENIED)) {
            holder.statusView.setText(context.getText(R.string.trans_denied));
            holder.statusView.setVisibility(View.VISIBLE);
        } else if (trans.getStatus().equals(Transaction.STATUS.APPROVED)) {
            holder.statusView.setText(context.getText(R.string.trans_approved));
            holder.statusView.setVisibility(View.VISIBLE);
        } else {
            Log.e(TAG, String.format("Transaction status \'%s\' could not be identified",
                    trans.getStatus()));
            holder.statusView.setText("");
            holder.statusView.setVisibility(View.GONE);
        }

        // Outgoing transactions should be approvable/deniable
        if (trans.getStatus().equals(Transaction.STATUS.PENDING) &&
                trans.getType().equals(Transaction.TYPE.SENDING)) {
            Log.d(TAG, "It's a sending transaction, attaching listener");
            ViewListener listener = new ViewListener(context, trans.getId());
            holder.decNeeded.setVisibility(View.VISIBLE); // Mark the transaction for the user
            convertView.setOnClickListener(listener);
        } else {
            // Other transactions don't need to be marked
            Log.d(TAG, "It's not a sending transaction");
            holder.decNeeded.setVisibility(View.GONE);
        }
        return convertView;
    }

    @Override
    public int getCount() {
        Log.d(TAG, "Get Count: " + values.size());
        return values.size();
    }

    /**
     * This inner class helps collect Views for the getView() method above.
     */
    private class ViewHolder {
        public TextView groupNoteview;
        public TextView decNeeded;
        public TextView payerView;
        public TextView payeeView;
        public TextView amountView;
        public TextView transId;
        public TextView statusView;
        public TextView date;
    }

    /**
     * This helper class handles when a user clicks on a transaction
     * on the history screen.
     */
    private class ViewListener implements View.OnClickListener {

        private static final String TAG = "trans_dialog";

        private String transactionID;

        private Context mContext;

        public ViewListener(Context context, String transactionID) {
            mContext = context;
            this.transactionID = transactionID;
        }

        @Override
        public void onClick(View v) {
            String transId = (((TextView) v.findViewById(R.id.transacation_id)).getText()).toString();
            if (!transId.equals(transactionID)) {
                Log.e(TAG, String.format("Transaction ID mismatch: ListenerID: %s | TransID: %s",
                        transactionID, transId));
                return;
            }
            Log.d(TAG, "Clicked on transaction: " + transId);

            ///////////////// Open Approve / Deny Dialog /////////////////
            try {
                Log.d(TAG, "Creating dialog for transaction: " + transactionID);
                final Dialog dialog = new Dialog(mContext);
                dialog.setContentView(R.layout.dialog);
                dialog.setTitle(mContext.getResources().getText(R.string.trans_dialog_title));

                // Collect Views from the dialog
                TextView text = (TextView) dialog.findViewById(R.id.dialogueText);
                Button buttonPos = (Button) dialog.findViewById(R.id.dialogButtonPOS);
                Button buttonNeg = (Button) dialog.findViewById(R.id.dialogButtonNEG);
                Button buttonDis = (Button) dialog.findViewById(R.id.dialogButtonCancel);

                // Communicate to user that transaction is processing
                String payee = (((TextView) v.findViewById(R.id.activity_confirm_payee)).getText()).toString();
                text.setText(String.format(mContext.getResources().getString(R.string.trans_dialog_msg), payee));

                // Create listeners to handle the dialog buttons
                // Approve button dismisses dialog and approves transaction
                buttonPos.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "user approved: " + transactionID);
                        dialog.dismiss();
                        approveTransaction(true, transactionID);
                    }
                });

                // Deny button dismisses dialog and denies transaction
                buttonNeg.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "user denied: " + transactionID);
                        dialog.dismiss();
                        approveTransaction(false, transactionID);
                    }
                });

                // Cancel button just dismisses the dialog
                buttonDis.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "user dismissed dialog for transaction: " + transactionID);
                        dialog.dismiss();
                    }
                });

                dialog.show();
            } catch (Exception e) {
                Log.e(TAG, String.format("Transaction dialog error: %s", e.getMessage()));
                e.printStackTrace();
            }
        }

        /**
         * Generates an intent to approve or deny a transaction using push messages.
         *
         * @param approve True if the action is approval, false otherwise.
         * @param id      The id of the transaction to act upon.
         */
        public void approveTransaction(boolean approve, String id) {
            final String serviceContext = "approveTransaction";
//            Intent intent = new Intent(mContext, RESTCalls.class);
            JSONObject obj = new JSONObject();
            try {
                obj.put("status", (approve ? "APPROVED" : "DENIED"));
            } catch (JSONException e) {
                Toast.makeText(mContext, "Error creating JSON", Toast.LENGTH_SHORT).show();
            }

            // Construct the url for the REST request
/*            String url = String.format("%s/%s/%s/%s",
                    Constants.baseURL, //TODO this should be passed in at construction?
                    "transactions",
                    (approve ? "approve" : "deny"),
                    id);
  */
            String url = Constants.baseURL + "/users/" + Constants.userName + "/debits/" + id + "?access_token=" + Globals.msToken;

            Rest.put(url, null, null, obj.toString(), mApprovalResponseHandler);
        }
    }

}
