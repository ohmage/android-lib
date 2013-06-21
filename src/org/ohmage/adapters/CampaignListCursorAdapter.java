package org.ohmage.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.ohmage.OhmageApplication;
import org.ohmage.activity.SubActionClickListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;

public class CampaignListCursorAdapter extends CursorAdapter{
	
	private final LayoutInflater mInflater;
	private final SubActionClickListener mListener;
	private final ImageLoader mImageLoader;
	
	public CampaignListCursorAdapter(Context context, Cursor c, SubActionClickListener listener, int flags) {
		super(context, c, flags);
		
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListener = listener;
		mImageLoader = OhmageApplication.getImageLoader();
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		NetworkImageView iconImage = (NetworkImageView) view.findViewById(R.id.icon_image);
		iconImage.setDefaultImageResId(R.drawable.apple_logo);
		TextView nameText = (TextView) view.findViewById(R.id.main_text);
		TextView urnText = (TextView) view.findViewById(R.id.sub_text);
		ImageButton actionButton = (ImageButton) view.findViewById(R.id.action_button);
		
		final String campaignUrn = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_URN));
		
		String iconUrl = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_ICON));
		iconImage.setImageUrl(iconUrl, mImageLoader);

		nameText.setText(cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_NAME)));
		urnText.setText(campaignUrn);
		actionButton.setFocusable(false);
		actionButton.setEnabled(true);
		actionButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Analytics.widget(v, null, campaignUrn);
				mListener.onSubActionClicked(Campaigns.buildCampaignUri(campaignUrn));
			}
		});
		
		int status = cursor.getInt(cursor.getColumnIndex(Campaigns.CAMPAIGN_STATUS));
		
		// first off, clear animation for any non-animating states
		if (status != Campaign.STATUS_DOWNLOADING)
			actionButton.clearAnimation();
		
		switch (status) {
		case Campaign.STATUS_REMOTE:
			actionButton.setContentDescription(context.getString(R.string.campaign_list_item_action_button_download_description));
			actionButton.setImageResource(R.drawable.subaction_campaign_download);
			break;
			
		case Campaign.STATUS_READY:
			actionButton.setContentDescription(context.getString(R.string.campaign_list_item_action_button_surveys_description));
			actionButton.setImageResource(R.drawable.subaction_surveys_list);
			break;
			
		case Campaign.STATUS_STOPPED:
		case Campaign.STATUS_OUT_OF_DATE:
		case Campaign.STATUS_INVALID_USER_ROLE:
		case Campaign.STATUS_NO_EXIST:
		case Campaign.STATUS_VAGUE:
			actionButton.setContentDescription(context.getString(R.string.campaign_list_item_action_button_error_description));
			actionButton.setImageResource(R.drawable.subaction_campaign_broken);
			break;
			
		case Campaign.STATUS_DOWNLOADING:
			actionButton.setContentDescription(context.getString(R.string.campaign_list_item_action_button_downloading_description));
	        actionButton.setEnabled(false);
			actionButton.setImageResource(R.drawable.spinner_white_48);
			
			if (actionButton.getAnimation() == null) {
				// makes the progress indicator rotate
				// this will be stopped next state change, assuming we're not still downloading
				Animation rotation = AnimationUtils.loadAnimation(context, R.anim.clockwise_rotation);
				rotation.setRepeatCount(Animation.INFINITE);
				actionButton.startAnimation(rotation);
			}

			break;
			
		default:
			//campaign is in some unknown state!
			actionButton.setVisibility(View.INVISIBLE);
			break;
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.ohmage_list_item, null);
	}
}
