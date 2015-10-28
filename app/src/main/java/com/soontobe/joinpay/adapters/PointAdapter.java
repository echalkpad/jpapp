package com.soontobe.joinpay.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.soontobe.joinpay.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;

/**
 * This class is an adapter for the layout of the transaction summary view or history view.
 *
 */
public class PointAdapter extends ArrayAdapter<JSONArray> {
	private final Context context;
	private final JSONArray values;

	public PointAdapter(Context context, JSONArray values) {
		super(context, R.layout.activity_points);
		this.context = context;
		this.values = values;
		
		Log.d("points", "point adapter construct running: " + values.toString());
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView;		

		if(convertView == null) {
			rowView = inflater.inflate(R.layout.points_layout, parent, false);
		} else {
			rowView = convertView;
		}
		
		JSONObject obj;
		try {
			obj = values.getJSONObject(position);
		} catch (JSONException e) {
			Log.e("points", "error");
			e.printStackTrace();
			return rowView;
		}
		
		TextView pointsText = (TextView) rowView.findViewById(R.id.pointsText);
		TextView programText = (TextView) rowView.findViewById(R.id.programName);

		String points = "-";
		String name = "-";
		try {
			points = obj.getString("pointBalance");
			name = obj.getString("programName");
		} catch (JSONException e2) {
			e2.printStackTrace();
		}

		points = NumberFormat.getNumberInstance().format(Integer.parseInt(points));	
		pointsText.setText(points);
		programText.setText(name);
		return rowView;
	}
	
	@Override
	public int getCount() {
	    return values.length();
	}
} 
