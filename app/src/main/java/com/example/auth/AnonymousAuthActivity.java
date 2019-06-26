package com.example.auth;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity to demonstrate anonymous login and account linking (with an email/password account).
 */
public class AnonymousAuthActivity extends BaseActivity implements View.OnClickListener {
	private static final String TAG = "AnonymousAuth";
	private FirebaseAuth mAuth;
	private FirebaseAuth.AuthStateListener mAuthListener;
	private EditText mEmailField, mPasswordField;
	private TextView mTextViewProfile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_anonymous_auth);

		mTextViewProfile = findViewById(R.id.profile);
		mEmailField = findViewById(R.id.field_email);
		mPasswordField = findViewById(R.id.field_password);

		findViewById(R.id.button_anonymous_sign_in).setOnClickListener(this);
		findViewById(R.id.button_anonymous_sign_out).setOnClickListener(this);
		findViewById(R.id.button_link_account).setOnClickListener(this);

		mAuth = FirebaseAuth.getInstance();
		mAuthListener = new FirebaseAuth.AuthStateListener() {
			@Override
			public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
				FirebaseUser user = firebaseAuth.getCurrentUser();
				if (user != null) {
					Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
				} else {
					Log.d(TAG, "onAuthStateChanged:signed_out");
				}
				updateUI(user);
			}
		};
	}

	@Override
	public void onStart() {
		super.onStart();
		mAuth.addAuthStateListener(mAuthListener);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mAuthListener != null) {
			mAuth.removeAuthStateListener(mAuthListener);
		}
	}

	private void signInAnonymously() {
		showProgressDialog();
		mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
				Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
				if (!task.isSuccessful()) {
					mTextViewProfile.setTextColor(Color.RED);
					mTextViewProfile.setText(task.getException().getMessage());
				} else {
					mTextViewProfile.setTextColor(Color.DKGRAY);
				}
				hideProgressDialog();
			}
		});
	}

	private void signOut() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.logout);
		alert.setCancelable(false);
		alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				mAuth.signOut();
				updateUI(null);
			}
		});
		alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
			}
		});
		alert.show();
	}

	private void linkAccount() {
		if (!validateLinkForm()) {
			return;
		}
		showProgressDialog();

		String email = mEmailField.getText().toString();
		String password = mPasswordField.getText().toString();

		AuthCredential credential = EmailAuthProvider.getCredential(email, password);
		mAuth.getCurrentUser().linkWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
				Log.d(TAG, "linkWithCredential:onComplete:" + task.isSuccessful());
				if (!task.isSuccessful()) {
					mTextViewProfile.setTextColor(Color.RED);
					mTextViewProfile.setText(task.getException().getMessage());
				} else {
					mTextViewProfile.setTextColor(Color.DKGRAY);
				}
				hideProgressDialog();
			}
		});
	}

	private boolean validateLinkForm() {
		if (TextUtils.isEmpty(mEmailField.getText().toString())) {
			mEmailField.setError("Required.");
			return false;
		} else if (TextUtils.isEmpty(mPasswordField.getText().toString())) {
			mPasswordField.setError("Required.");
			return false;
		} else {
			mEmailField.setError(null);
			return true;
		}
	}

	private void updateUI(FirebaseUser user) {
		boolean isSignedIn = (user != null);

		if (isSignedIn) {
			mTextViewProfile.setText("Email: " + user.getEmail());
			mTextViewProfile.append("\n");
			mTextViewProfile.append("Firebase ID: " + user.getUid());
		} else {
			mTextViewProfile.setText(null);
		}

		findViewById(R.id.button_anonymous_sign_in).setEnabled(!isSignedIn);
		findViewById(R.id.button_anonymous_sign_out).setEnabled(isSignedIn);
		findViewById(R.id.button_link_account).setEnabled(isSignedIn);

		hideProgressDialog();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_anonymous_sign_in:
				signInAnonymously();
				break;
			case R.id.button_anonymous_sign_out:
				signOut();
				break;
			case R.id.button_link_account:
				linkAccount();
				break;
		}
	}
}