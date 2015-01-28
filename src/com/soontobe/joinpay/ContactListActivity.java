package com.soontobe.joinpay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter.FilterListener;
import android.widget.ListView;


/**
* This class handles the operation of selecting people from the contact list to the radar view.
* It also incorporates the search function. After the "Add" button is clicked, people selected will be returned to the radar view pane.
*
*/
public class ContactListActivity extends ListActivity {

	final Set<String> nameSelected = new HashSet<String>();

	private ListView lv;

	// Listview Adapter
	private ArrayAdapter<String> adapter;

	// Search EditText
	private EditText inputSearch;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  //No Title Bar
		setContentView(R.layout.choose_from_contact_list);
		setContactListView();
		setInputSearch();
		setEventListeners();
	}

	private void setEventListeners() {
		Button contactListAddButton = (Button) findViewById(R.id.contact_list_add_button);
		contactListAddButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Button btn = (Button) v;
				// TODO Auto-generated method stub
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					btn.setBackgroundResource(R.drawable.button_active);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					btn.setBackgroundResource(R.drawable.button_normal);
				}
				return false;
			}
		});

		EditText contactSearchInput = (EditText) findViewById(R.id.contact_search_input);
		contactSearchInput.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				Button clearButton = (Button) findViewById(R.id.button_clear_contact_search_input);
				// TODO Auto-generated method stub
				if (count == 0) {
					clearButton.setVisibility(View.INVISIBLE);
				} else {
					clearButton.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
			}
		});
	}

	private void setContactListView() {
		int layoutType = android.R.layout.simple_list_item_multiple_choice;
		lv = getListView();
		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		adapter = new ArrayAdapter<String>(this, layoutType, Constants.contactNameList);
		setListAdapter(adapter);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> myAdapter, View myView, int position, long mylng) {
				String name = (String) (lv.getItemAtPosition(position));
				if (lv.isItemChecked(position)) {
					nameSelected.add(name);
				} else {
					nameSelected.remove(name);
				}
			}
		});
	}

	private void setInputSearch() {
		inputSearch = (EditText) findViewById(R.id.contact_search_input);
		inputSearch.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				// When user changed the Text
				adapter.getFilter().filter(cs, new FilterListener() {
					@Override
					public void onFilterComplete(int count) {
						// TODO Auto-generated method stub
						for (int i = 0;i < adapter.getCount();i++) {
							if (nameSelected.contains(adapter.getItem(i))) {
								lv.setItemChecked(i, true);
							} else {
								lv.setItemChecked(i, false);
							}
						}    
					}
				});
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				// TODO Auto-generated method stub
			}

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub  
			}
		});

		findViewById(R.id.button_clear_contact_search_input).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				EditText contactSearchBox = (EditText) findViewById(R.id.contact_search_input);
				contactSearchBox.setText("");
			}
		});
	}

	public void AddContactAndBackToMain(View v) {
		Intent data = new Intent();
		String strArray[] = nameSelected.toArray(new String[nameSelected.size()]);
		data.putExtra("name", strArray);
		data.setData(Uri.parse(nameSelected.toString()));
		setResult(RESULT_OK, data);
		finish();
	}
}