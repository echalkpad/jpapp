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

    private View mCurrentView;
    private WebView mWebView;

    /**
     * Factory method for creating a new instance of ChatFragment
     * @return A new ChatFragment instance
     */
    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        return fragment;
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(mCurrentView == null)
            mCurrentView = inflater.inflate(R.layout.fragment_chat, container, false);

        // Remove association with any old parent so that we can be added as a child after returning
        ViewGroup parent = (ViewGroup) mCurrentView.getParent();
        if(parent != null){
            parent.removeView(mCurrentView);
        }

        // Connect the WebView to our online BusinessHub chat client
        mWebView = (WebView)mCurrentView.findViewById(R.id.webView);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        Log.d("chat", "connecting chat to url");
        mWebView.loadUrl(getResources().getString(R.string.test_chat_url));
        mWebView.setWebViewClient(new WebViewClient());

        return mCurrentView;
    }
}
