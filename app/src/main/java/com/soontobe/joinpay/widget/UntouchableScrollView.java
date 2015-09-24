package com.soontobe.joinpay.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * A strange type of ScrollView used as the container of RadarView
 *  just for a better layout
 */

public class UntouchableScrollView extends ScrollView {

	public UntouchableScrollView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public UntouchableScrollView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	public UntouchableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		//return super.onGenericMotionEvent(event);
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		//Block scroll by touch
		return true;
	}
	
}
