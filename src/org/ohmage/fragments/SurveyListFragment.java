package org.ohmage.fragments;


import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.Gravity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.ohmage.ConfigHelper;
import org.ohmage.library.R;
import org.ohmage.activity.SubActionClickListener;
import org.ohmage.adapters.SurveyListCursorAdapter;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Survey;
import org.ohmage.db.utils.SelectionBuilder;
import org.ohmage.logprobe.Analytics;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

/**
 * <p>The {@link SurveyListFragment} shows a list of surveys</p>
 * 
 * <p>The {@link SurveyListFragment} accepts {@link CampaignFilter#EXTRA_CAMPAIGN_URN} as an extra</p>
 * @author cketcham
 *
 */
public class SurveyListFragment extends FilterableListFragment implements SubActionClickListener {

	/**
	 * Pass in the arguments to determine if this should show the pending surveys or all surveys.
	 * true for pending, false for all
	 */
	public static final String KEY_PENDING = "key_pending";

	private static final String TAG = "SurveyListFragment";
		
	private boolean mShowPending = false;
	
	private CursorAdapter mAdapter;
	private OnSurveyActionListener mListener;
	
	public interface OnSurveyActionListener {
		public void onSurveyActionView(Uri surveyUri);
        public void onSurveyActionStart(Uri surveyUri);
        public void onSurveyActionUnavailable(Uri surveyUri);
        public void onSurveyActionError(Uri surveyUri, int status);
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(getArguments() != null)
			mShowPending = getArguments().getBoolean(KEY_PENDING, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        setShowPending();
        
        // style the empty text
		TextView emptyView = (TextView)getListView().getEmptyView();
		emptyView.setGravity(Gravity.LEFT);
		emptyView.setPadding(25, 25, 25, 0);
		
		mAdapter = new SurveyListCursorAdapter(getActivity(), null, this, 0);
		setListAdapter(mAdapter);
		
		// Start out with a progress indicator.
        setListShown(false);
        
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSurveyActionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSurveyActionListener");
        }
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) getListAdapter().getItem(position);
		String campaignUrn = cursor.getString(cursor.getColumnIndex(Surveys.CAMPAIGN_URN));
		String surveyId = cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_ID));

		Analytics.widget(v, null, campaignUrn + ":" + surveyId);

		Uri uri = Campaigns.buildSurveysUri(campaignUrn, surveyId);
		mListener.onSurveyActionView(uri);
	}
	
	@Override
	public void onSubActionClicked(Uri uri) {
		if(uri == null)
			mListener.onSurveyActionError(uri, 0);
		else
			mListener.onSurveyActionStart(uri);
		
//		mListener.onSurveyActionUnavailable();
	}

	public void setShowPending(boolean showPending) {
		mShowPending = showPending;
		if(isVisible()) {
			setShowPending();
			getLoaderManager().restartLoader(0, null, this);
		}
	}
	
	private void setShowPending() {
		if (mShowPending) {
			if(ConfigHelper.isSingleCampaignMode())
				setEmptyText(getString(R.string.surveys_empty_pending_single));
			else
				setEmptyText(getString(R.string.surveys_empty_pending));
		} else {
			if(ConfigHelper.isSingleCampaignMode())
				setEmptyText(getString(R.string.single_campaign_error));
			else
				setEmptyText(getString(R.string.surveys_empty_all));
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri baseUri = Surveys.CONTENT_URI;
		
		SelectionBuilder builder = new SelectionBuilder();
		
		if (getCampaignUrn() != null) {
			baseUri = Campaigns.buildSurveysUri(getCampaignUrn());
		}
		
		if (mShowPending) {
			builder.where(Surveys.SURVEY_STATUS + "=" + Survey.STATUS_TRIGGERED);
		} 
		
		return new CursorLoader(getActivity(), baseUri, null, builder.getSelection(), builder.getSelectionArgs(), Surveys._ID);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		
		// The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
