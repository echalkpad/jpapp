package com.soontobe.joinpay.activities;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter.FilterListener;
import android.widget.ListView;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.R;

import java.util.HashSet;
import java.util.Set;


/**
* This class handles the operation of selecting people from the contact list to the radar view.
* It also incorporates the search function. After the "Add" button is clicked, people selected will be returned to the radar view pane.
*
*/
public class ContactListActivity extends ListActivity {

	private final Set<String> nameSelected = new HashSet<>();

	private ListView lv;

	// Listview Adapter
	private ArrayAdapter<String> adapter;

	// Search EditText
	private EditText inputSearch;

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//No Title Bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_from_contact_list);
		setContactListView();
		setInputSearch();
		setEventListeners();
	}

	/**
	 * Set the event listeners.
	 * Add button & search bar.
	 */
	private void setEventListeners() {
		Button contactListAddButton = (Button) findViewById(R.id.contact_list_add_button);

		// Change background color of button, when the button is touched
		contactListAddButton.setOnTouchListener(new OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				Button btn = (Button) v;
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					btn.setBackgroundResource(R.drawable.button_active);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					btn.setBackgroundResource(R.drawable.button_normal);
				}
				return false;
			}
		});
	}

	/**
	 * Set the contact list view.
	 * Connect the array adapter,
	 * Flip the checkbox check when the item is clicked.
	 */
	private void setContactListView() {
		// Setup the list view
		int layoutType = android.R.layout.simple_list_item_multiple_choice;
		lv = getListView();
		// Enable multiple choices
		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		adapter = new ArrayAdapter<>(this, layoutType, Constants.contactNameList);
		setListAdapter(adapter);

		// When an item is clicked, flip the checkbox
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> myAdapter, final View myView, final int position, final long mylng) {
				String name = (String) (lv.getItemAtPosition(position));
				if (lv.isItemChecked(position)) {
					nameSelected.add(name);
				} else {
					nameSelected.remove(name);
				}
			}
		});
	}

	/**
	 * Actions when search input is changed.
	 */
	private void setInputSearch() {
		inputSearch = (EditText) findViewById(R.id.contact_search_input);
		inputSearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(final CharSequence cs, final int start, final int before, final int count) {
				// Show or hide clear text (X) button when the text in search box is changed
				Button clearButton = (Button) findViewById(R.id.button_clear_contact_search_input);
				if (count == 0) {
					clearButton.setVisibility(View.INVISIBLE);
				} else {
					clearButton.setVisibility(View.VISIBLE);
				}
				// When user changed the Text, filter the users
				adapter.getFilter().filter(cs, new FilterListener() {
					@Override
					public void onFilterComplete(final int count) {
						for (int i = 0; i < adapter.getCount(); i++) {
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
			public void beforeTextChanged(final CharSequence arg0, final int arg1, final int arg2,
										  final int arg3) {
				// Do nothing
			}

			@Override
			public void afterTextChanged(final Editable arg0) {
				// Do nothing
			}
		});

		// Clear button action
		findViewById(R.id.button_clear_contact_search_input).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				EditText contactSearchBox = (EditText) findViewById(R.id.contact_search_input);
				contactSearchBox.setText("");
			}
		});
	}

	/**
	 * Callback function for "Add" button.
	 * Generate the result and return.
	 * Generate the list of selected names and return it to the caller activity
	 * @param v The view "Add" button
	 */
	public final void addContactAndBackToMain(final View v) {
		Intent data = new Intent();
		String strArray[] = nameSelected.toArray(new String[nameSelected.size()]);
		data.putExtra("name", strArray);
		data.setData(Uri.parse(nameSelected.toString()));
		setResult(RESULT_OK, data);
		finish();
	}
}