
package org.ohmage.ui;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import org.ohmage.UserPreferencesHelper;
import org.ohmage.library.R;

/**
 * A base activity for entity info screens that includes the entity info header,
 * and provides a view below the header that scrolls independently. That have a
 * reference to a campaign
 * 
 * @author Cameron
 */
public abstract class BaseCampaignInfoActivity extends BaseInfoActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.base_info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_view_feedback).setVisible(UserPreferencesHelper.showFeedback());
        return true;
    }
}
