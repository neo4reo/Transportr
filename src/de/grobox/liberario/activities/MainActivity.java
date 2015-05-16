/*    Liberario
 *    Copyright (C) 2013 Torsten Grote
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.grobox.liberario.activities;

import de.cketti.library.changelog.ChangeLog;
import de.grobox.liberario.LiberarioApplication;
import de.grobox.liberario.TransportNetwork;
import de.grobox.liberario.fragments.AboutMainFragment;
import de.grobox.liberario.fragments.DirectionsFragment;
import de.grobox.liberario.fragments.FavTripsFragment;
import de.grobox.liberario.Preferences;
import de.grobox.liberario.R;
import de.grobox.liberario.fragments.PrefsFragment;
import de.grobox.liberario.fragments.StationsFragment;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialAccountListener;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;

public class MainActivity extends MaterialNavigationDrawer implements TransportNetwork.Handler {
	static final public int CHANGED_NETWORK_PROVIDER = 1;
	static final public int CHANGED_HOME = 2;

	@Override
	public void init(Bundle savedInstanceState) {
		// Initialize Application Context with all Transport Networks
		((LiberarioApplication) getApplicationContext()).initilize(this);

		TransportNetwork network = Preferences.getTransportNetwork(this);

		checkFirstRun(network);

		if(network != null) {
			MaterialAccount account = new MaterialAccount(this.getResources(), network.getName(), network.getRegion(), network.getLogo(), network.getBackground());
			addAccount(account);
		} else {
			// add fake account, so there's something to be changed later. Otherwise crashes
			MaterialAccount account = new MaterialAccount(this.getResources(), "null", "null", R.drawable.ic_placeholder, null);
			addAccount(account);
		}

		addSection(newSection(getString(R.string.tab_directions), getResources().getDrawable(android.R.drawable.ic_menu_directions), new DirectionsFragment()));
		addSection(newSection(getString(R.string.tab_fav_trips), getResources().getDrawable(R.drawable.ic_action_star), new FavTripsFragment()));
		addSection(newSection(getString(R.string.tab_departures), getResources().getDrawable(R.drawable.ic_tab_stations), new StationsFragment()));

		addBottomSection(newSection(getString(R.string.action_settings), getResources().getDrawable(android.R.drawable.ic_menu_preferences), new PrefsFragment()));
		addBottomSection(newSection(getResources().getString(R.string.action_about) + " " + getResources().getString(R.string.app_name), getResources().getDrawable(android.R.drawable.ic_menu_info_details), new AboutMainFragment()));
		addBottomSection(newSection(getString(R.string.action_changelog), getResources().getDrawable(R.drawable.ic_action_changelog), new MaterialSectionListener() {
			@Override
			public void onClick(MaterialSection materialSection) {
				new HoloChangeLog(getContext()).getFullLogDialog().show();
				materialSection.unSelect();
			}
		}));

		setAccountListener(new MaterialAccountListener() {
			@Override
			public void onAccountOpening(MaterialAccount materialAccount) {
				startActivityForResult(new Intent(getContext(), PickNetworkProviderActivity.class), CHANGED_NETWORK_PROVIDER);
			}

			@Override
			public void onChangeAccount(MaterialAccount materialAccount) {
				// nop
			}
		});


		// show Changelog
		HoloChangeLog cl = new HoloChangeLog(this);
		if(cl.isFirstRun() && !cl.isFirstRunEver()) {
			cl.getLogDialog().show();
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if(requestCode == CHANGED_NETWORK_PROVIDER && resultCode == RESULT_OK) {
			onNetworkProviderChanged(Preferences.getTransportNetwork(this));
		}
	}

	public void onNetworkProviderChanged(TransportNetwork network) {
		Toolbar toolbar = getToolbar();
		if(toolbar != null) {
			// get and set new network name for app bar
			toolbar.setSubtitle(network.getName());
		}

		// TODO take the last two used networks into consideration and display them
		MaterialAccount account = getCurrentAccount();
		account.setTitle(network.getName());
		account.setSubTitle(network.getRegion());
		account.setPhoto(network.getLogo());
		account.setBackground(network.getBackground());

		notifyAccountDataChanged();

		// call this method for each fragment
		for(Object section : getSectionList()) {
			Object obj = ((MaterialSection) section).getTargetFragment();

			if(obj instanceof TransportNetwork.Handler) {
				((TransportNetwork.Handler) obj).onNetworkProviderChanged(network);
			}
		}
	}

	private void checkFirstRun(TransportNetwork network) {
		// return if no network is set
		if(network == null) {
			Intent intent = new Intent(this, PickNetworkProviderActivity.class);

			// force choosing a network provider
			intent.putExtra("FirstRun", true);

			startActivityForResult(intent, CHANGED_NETWORK_PROVIDER);
		}
		else {
			Toolbar toolbar = getToolbar();
			if(toolbar != null) toolbar.setSubtitle(network.getName());

			disableLearningPattern();
		}
	}

	private Context getContext() {
		return this;
	}


	public static class HoloChangeLog extends ChangeLog {
		public static final String DARK_THEME_CSS =
				"body { color: #f3f3f3; font-size: 0.9em; background-color: #282828; } h1 { font-size: 1.3em; } ul { padding-left: 2em; }";

		public static final String MATERIAL_THEME_CSS =
				"body { color: #f3f3f3; font-size: 0.9em; background-color: #424242; } h1 { font-size: 1.3em; } ul { padding-left: 2em; }";

		public HoloChangeLog(Context context) {
			super(new ContextThemeWrapper(context, R.style.DialogTheme), theme());
		}

		private static String theme() {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				return MATERIAL_THEME_CSS;
			} else {
				return DARK_THEME_CSS;
			}
		}
	}
}
