package com.example.auth;

import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ManageUserActivity extends BaseActivity implements View.OnClickListener {
	private static final String TAG = "ManageUserActivity";
	private EditText mEditTextName, mEditTextPhoto, mEditTextEmail, mEditTextPassword, mEditTextEmailReset;
	private FirebaseAuth mAuth;
	private FirebaseAuth.AuthStateListener mAuthListener;
	private TextView mTextViewProfile, mTextViewProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manage_user);

		mTextViewProfile = findViewById(R.id.profile);
		mTextViewProvider = findViewById(R.id.provider);
		mEditTextName = findViewById(R.id.field_name);
		mEditTextPhoto = findViewById(R.id.field_photo);
		mEditTextEmail = findViewById(R.id.field_email);
		mEditTextPassword = findViewById(R.id.field_password);
		mEditTextEmailReset = findViewById(R.id.field_email_reset);

		findViewById(R.id.update_profile_button).setOnClickListener(this);
		findViewById(R.id.update_email_button).setOnClickListener(this);
		findViewById(R.id.update_password_button).setOnClickListener(this);
		findViewById(R.id.send_password_reset_button).setOnClickListener(this);
		findViewById(R.id.delete_button).setOnClickListener(this);

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

	@Override
	public void onClick(View view) {
		final FirebaseUser user = mAuth.getCurrentUser();
		switch (view.getId()) {
			case R.id.update_profile_button:
				if (validateForm()) {
					updateNameAndPhoto(user);
				}
				break;
			case R.id.update_email_button:
				if (validateEmail(mEditTextEmail)) {
					updateEmail(user);
				}
				break;
			case R.id.update_password_button:
				if (validatePassword()) {
					updatePassword(user);
				}
				break;
			case R.id.send_password_reset_button:
				if (validateEmail(mEditTextEmailReset)) {
					sendPasswordReset();
				}
				break;
			case R.id.delete_button:
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setMessage("Delete " + user.getEmail() + "?");
				alert.setCancelable(false);
				alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						deleteUser(user);
					}
				});
				alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();
					}
				});
				alert.show();
				break;
		}
	}

	private void updateUI(FirebaseUser user) {
		if (user != null) {
			// User's profile
			mTextViewProfile.setText("Firebase ID: " + user.getUid());
			mTextViewProfile.append("\n");
			mTextViewProfile.append("DisplayName: " + user.getDisplayName());
			mTextViewProfile.append("\n");
			mTextViewProfile.append("Email: " + user.getEmail());
			mTextViewProfile.append("\n");
			mTextViewProfile.append("Photo URL: " + user.getPhotoUrl());

			// User's provider
			mTextViewProvider.setText(null);
			for (UserInfo profile : user.getProviderData()) {
				// Id of the provider (ex: google.com)
				mTextViewProvider.append("providerId: " + profile.getProviderId());
				mTextViewProvider.append("\n");

				// UID specific to the provider
				mTextViewProvider.append("UID: " + profile.getUid());

				mTextViewProvider.append("\n");
				mTextViewProvider.append("name: " + profile.getDisplayName());
				mTextViewProvider.append("\n");
				mTextViewProvider.append("email: " + profile.getEmail());
				mTextViewProvider.append("\n");
				mTextViewProvider.append("photoUrl: " + profile.getPhotoUrl());
				if (!"password".equals(profile.getProviderId())) {
					mTextViewProvider.append("\n\n");
				}
			}
			findViewById(R.id.provider_fields).setVisibility(View.VISIBLE);
			findViewById(R.id.update_profile_fields).setVisibility(View.VISIBLE);
			findViewById(R.id.update_email_fields).setVisibility(View.VISIBLE);
			findViewById(R.id.update_password_fields).setVisibility(View.VISIBLE);
			findViewById(R.id.send_password_reset_fields).setVisibility(View.VISIBLE);
			findViewById(R.id.delete_fields).setVisibility(View.VISIBLE);
		} else {
			mTextViewProfile.setText(R.string.signed_out);
			mTextViewProvider.setText(null);
			findViewById(R.id.provider_fields).setVisibility(View.GONE);
			findViewById(R.id.update_profile_fields).setVisibility(View.GONE);
			findViewById(R.id.update_email_fields).setVisibility(View.GONE);
			findViewById(R.id.update_password_fields).setVisibility(View.GONE);
			findViewById(R.id.send_password_reset_fields).setVisibility(View.GONE);
			findViewById(R.id.delete_fields).setVisibility(View.GONE);
		}
		hideProgressDialog();
	}

	private void updateNameAndPhoto(FirebaseUser user) {
		showProgressDialog();
		UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
				.setDisplayName(mEditTextName.getText().toString())
				.setPhotoUri(Uri.parse(mEditTextPhoto.getText().toString()))
				.build();
		user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (task.isSuccessful()) {
					mTextViewProfile.setTextColor(Color.DKGRAY);
					mTextViewProfile.setText(getString(R.string.updated, "User profile"));
				} else {
					mTextViewProfile.setTextColor(Color.RED);
					mTextViewProfile.setText(task.getException().getMessage());
				}
				hideProgressDialog();
			}
		});
	}

	private void updateEmail(FirebaseUser user) {
		showProgressDialog();
		user.updateEmail(mEditTextEmail.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (task.isSuccessful()) {
					mTextViewProfile.setTextColor(Color.DKGRAY);
					mTextViewProfile.setText(getString(R.string.updated, "User email"));
				} else {
					mTextViewProfile.setTextColor(Color.RED);
					mTextViewProfile.setText(task.getException().getMessage());
				}
				hideProgressDialog();
			}
		});
	}

	private void updatePassword(FirebaseUser user) {
		showProgressDialog();
		user.updatePassword(mEditTextPassword.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (task.isSuccessful()) {
					if (task.isSuccessful()) {
						mTextViewProfile.setTextColor(Color.DKGRAY);
						mTextViewProfile.setText(getString(R.string.updated, "User password"));
					} else {
						mTextViewProfile.setTextColor(Color.RED);
						mTextViewProfile.setText(task.getException().getMessage());
					}
					hideProgressDialog();
				}
			}
		});
	}

	private void sendPasswordReset() {
		showProgressDialog();
		mAuth.sendPasswordResetEmail(mEditTextEmailReset.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (task.isSuccessful()) {
					mTextViewProfile.setTextColor(Color.DKGRAY);
					mTextViewProfile.setText("Email sent.");
				} else {
					mTextViewProfile.setTextColor(Color.RED);
					mTextViewProfile.setText(task.getException().getMessage());
				}
				hideProgressDialog();
			}
		});
	}

	private void deleteUser(FirebaseUser user) {
		showProgressDialog();
		user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (task.isSuccessful()) {
					mTextViewProfile.setTextColor(Color.DKGRAY);
					mTextViewProfile.setText("User account deleted.");
				} else {
					mTextViewProfile.setTextColor(Color.RED);
					mTextViewProfile.setText(task.getException().getMessage());
				}
				hideProgressDialog();
			}
		});
	}

	private boolean validateForm() {
		if (TextUtils.isEmpty(mEditTextName.getText().toString())) {
			mEditTextName.setError("Required.");
			return false;
		} else if (TextUtils.isEmpty(mEditTextPhoto.getText().toString())) {
			mEditTextPhoto.setError("Required.");
			return false;
		} else {
			mEditTextName.setError(null);
			return true;
		}
	}

	private boolean validateEmail(EditText edt) {
		String email = edt.getText().toString();
		if (TextUtils.isEmpty(email)) {
			edt.setError("Required.");
			return false;
		} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			edt.setError("Invalid.");
			return false;
		} else {
			edt.setError(null);
			return true;
		}
	}

	private boolean validatePassword() {
		String password = mEditTextPassword.getText().toString();
		if (TextUtils.isEmpty(password)) {
			mEditTextPassword.setError("Required.");
			return false;
		} else {
			mEditTextPassword.setError(null);
			return true;
		}
	}
}