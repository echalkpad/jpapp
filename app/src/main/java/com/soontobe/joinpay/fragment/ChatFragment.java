package com.soontobe.joinpay.fragment;

import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.soontobe.joinpay.R;

/**
 * A simple {@link Fragment} subclass. Use the {@link ChatFragment#newInstance}
 * factory method to create an instance of this fragment.  This fragment is used
 * to display the BusinessHub chat page to users.
 */
public class ChatFragment extends Fragment {

    /**
     * For tagging logs from this Fragment.
     */
    private static final String TAG = "chat";

    /**
     * The View that is created by the inflater in OnCreateView.
     */
    private View mCurrentView;

    /**
     * This WebView is used to display BusinessHub's chat client.
     */
    private WebView mWebView;

    /**
     * Factory method for creating a new instance of ChatFragment.
     *
     * @return A new ChatFragment instance
     */
    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        return fragment;
    }

    /**
     * Constructs a new ChatFragment.
     */
    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public final View onCreateView(final LayoutInflater inflater,
                                   final ViewGroup container,
                                   final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (mCurrentView == null) {
            mCurrentView = inflater.inflate(R.layout.fragment_chat,
                    container, false);
        }
        // Remove association with any old parent so that we
        // can be added as a child after returning
        ViewGroup parent = (ViewGroup) mCurrentView.getParent();
        if (parent != null) {
            parent.removeView(mCurrentView);
        }

        // Connect the WebView to our online BusinessHub chat client
        mWebView = (WebView) mCurrentView.findViewById(R.id.webView);

        // Initialize the UI
        initUI();

        return mCurrentView;
    }

    /**
     * Configures the WebView for and connects the WebView to the
     * online chat client.
     */
    private void initUI() {

        // Javascript is needed to for the online chat to work.
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);

        // Load the chat page.
        Log.d(TAG, "connecting chat to url");
        mWebView.loadUrl(getResources().getString(R.string.test_chat_url));
        mWebView.setWebViewClient(new WebViewClient());
    }
}
