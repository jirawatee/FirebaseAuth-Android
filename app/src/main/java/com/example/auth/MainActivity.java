package com.example.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
	private static final Class[] CLASSES = new Class[]{
			EmailPasswordActivity.class,
			GoogleSignInActivity.class,
			FacebookLoginActivity.class,
			TwitterLoginActivity.class,
			AnonymousAuthActivity.class,
			PhoneAuthActivity.class,
			ManageUserActivity.class
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ListView listView = findViewById(R.id.list_view);

		MyArrayAdapter adapter = new MyArrayAdapter(this, android.R.layout.simple_list_item_1, CLASSES);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Class clicked = CLASSES[position];
		startActivity(new Intent(this, clicked));
	}

	private static class MyArrayAdapter extends ArrayAdapter<Class> {
		private Context mContext;
		private Class[] mClasses;

		private MyArrayAdapter(Context context, int resource, Class[] objects) {
			super(context, resource, objects);
			mContext = context;
			mClasses = objects;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(android.R.layout.simple_list_item_1, null);
			}
			((TextView) view.findViewById(android.R.id.text1)).setText(mClasses[position].getSimpleName());

			return view;
		}
	}
}