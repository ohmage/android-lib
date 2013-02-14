package org.ohmage.activity;

import org.ohmage.library.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class ErrorDialogActivity extends FragmentActivity {

	public static final String EXTRA_TITLE = "extra_title";
	public static final String EXTRA_MESSAGE = "extra_message";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState == null) {
			String title = getIntent().getStringExtra(EXTRA_TITLE);
			String message = getIntent().getStringExtra(EXTRA_MESSAGE);

			if(title == null || message == null)
				throw new RuntimeException("A title and message must be supplied to the ErrorDialog");

			HelpDialogFragment.newInstance(title, message).show(getSupportFragmentManager(), "dialog");
		}
	}

	public static class HelpDialogFragment extends DialogFragment {

		public static HelpDialogFragment newInstance(String title, String message) {
			HelpDialogFragment f = new HelpDialogFragment();
			Bundle args = new Bundle();
			args.putString(EXTRA_TITLE, title);
			args.putString(EXTRA_MESSAGE, message);
			f.setArguments(args);
			return f;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getArguments().getString(EXTRA_TITLE))
			.setMessage(getArguments().getString(EXTRA_MESSAGE))
			.setPositiveButton(R.string.ok, null);

			return builder.create();
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			super.onDismiss(dialog);
			getActivity().finish();
		}
	}
}
