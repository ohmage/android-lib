
package org.ohmage.async;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.WindowManager;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;

import org.ohmage.OhmageApplication;
import org.ohmage.OhmageMarkdown;
import org.ohmage.PromptXmlParser;
import org.ohmage.Utilities;
import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Campaign;
import org.ohmage.logprobe.Log;
import org.ohmage.prompt.ChoicePrompt;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.SurveyElement;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CampaignContentDownloadTask extends AsyncTask<Void, Void, ArrayList<String>> {

    private static final String TAG = "CampaignXmlDownloadTask";

    private final String urn;

    private final Context mContext;

    public CampaignContentDownloadTask(Context context, String campaignUrn) {
        super();
        urn = campaignUrn;
        mContext = context;
    }

    @Override
    protected ArrayList<String> doInBackground(Void... params) {
        Cursor surveys = mContext.getContentResolver().query(Campaigns.buildSurveysUri(urn),
                new String[] {
                    Surveys.SURVEY_ID
                }, null, null, null);
        final ArrayList<String> images = new ArrayList<String>();
        while (surveys.moveToNext()) {
            try {
                List<SurveyElement> surveyElements = PromptXmlParser.parseSurveyElements(
                        mContext, urn, surveys.getString(0));
                for (SurveyElement elem : surveyElements) {
                    if (elem instanceof Prompt) {
                        ImageGetter imageGetter = new ImageGetter() {

                            @Override
                            public Drawable getDrawable(String source) {
                                images.add(source);
                                return null;
                            }
                        };
                        Html.fromHtml(OhmageMarkdown.parseHtml(((Prompt) elem).getPromptText()),
                                imageGetter, null);
                        if(elem instanceof ChoicePrompt) {
                            for(KVLTriplet choice : ((ChoicePrompt) elem).getChoices()) {
                                Html.fromHtml(OhmageMarkdown.parseHtml(choice.label),
                                        imageGetter, null);
                            }
                        }
                    }
                }
            } catch (NotFoundException e) {
                Log.e(TAG, "Error parsing prompts from xml", e);
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Error parsing prompts from xml", e);
            } catch (IOException e) {
                Log.e(TAG, "Error parsing prompts from xml", e);
            }
        }
        surveys.close();
        return images;
    }

    @Override
    public void onPostExecute(ArrayList<String> response) {
        if (response != null) {
            // Download images for this campaign
            final File cached = Campaign.getCacheDirFor(mContext, urn);

            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            int windowWidth = wm.getDefaultDisplay().getWidth();

            for (String image : response) {
                File cachef = new File(cached, Utilities.hashUrl(image));
                if (!cachef.exists()) {

                    OhmageApplication.getImageLoader().get(image, new ImageListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onResponse(ImageContainer response, boolean isImmediate) {
                            // If this returns immediately, and there is no
                            // bitmap, there is nothing we can do
                            if (isImmediate && response.getBitmap() == null)
                                return;

                            File cachef = new File(cached, Utilities.hashUrl(response
                                    .getRequestUrl()));

                            new BitmapCompressTask(mContext, response.getBitmap(), cachef)
                                    .execute();
                        }
                    }, windowWidth, 0);
                }
            }
        }
    }

}
