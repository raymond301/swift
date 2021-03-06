package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.view.client.SelectionChangeEvent;

/**
 * Lets the user choose a search type.
 * Fires an on change event when it gets modified by the user.
 */
public final class SearchTypeList extends ListBox implements ClickHandler, ChangeHandler, SelectionChangeEvent.HasSelectionChangedHandlers {

	/**
	 * Table of offered search types.
	 */
	private SearchTypeEntry[] searchTypeEntries = new SearchTypeEntry[]{
			new SearchTypeEntry(SearchType.OneToOne, "Separate experiment for each input", "one-to-one"),
			new SearchTypeEntry(SearchType.ManyToOne, "One experiment, all inputs are one biological sample", "many-to-one"),
			new SearchTypeEntry(SearchType.ManyToSamples, "One experiment, each input is a separate biological sample", "many-to-samples"),
			new SearchTypeEntry(SearchType.Custom, "Custom", "custom"),
	};
	private static final int DEFAULT_ENTRY = 2; /* ManyToSamples */

	private static final String SEARCH_TYPE_COOKIE = "search-type";

	public SearchTypeList() {
		for (int i = 0; i < searchTypeEntries.length; i++) {
			addItem(searchTypeEntries[i].getLabel(), String.valueOf(searchTypeEntries[i].getType().getType()));
			searchTypeEntries[i].index = i;
		}

		final String searchTypeCookie = Cookies.getCookie(SEARCH_TYPE_COOKIE);
		SearchTypeEntry selectedSearchType = searchTypeEntries[DEFAULT_ENTRY];
		if (searchTypeCookie != null) {
			for (final SearchTypeEntry searchTypeEntry : searchTypeEntries) {
				if (searchTypeEntry.getCookie().equalsIgnoreCase(searchTypeCookie)) {
					selectedSearchType = searchTypeEntry;
					break;
				}
			}
		}

		setSelectedIndex(selectedSearchType.getIndex());
		addClickHandler(this);
		addChangeHandler(this);
	}

	/**
	 * Given a file setup, guess which search type it corresponds to.
	 */
	public static SearchType getSearchTypeFromSetup(
			final String searchTitle,
			final String fileNameWithoutExtension,
			final String experimentName,
			final String biologicalSampleName) {
		if (experimentName.equals(searchTitle)) {
			// Likely there is one .SFD file for the entire experiment
			if (biologicalSampleName.equals(fileNameWithoutExtension)) {
				return SearchType.ManyToSamples;
			} else if (biologicalSampleName.equals(searchTitle)) {
				return SearchType.ManyToOne;
			} else {
				return SearchType.Custom;
			}
		} else {
			if (experimentName.equals(fileNameWithoutExtension) &&
					biologicalSampleName.endsWith(fileNameWithoutExtension)) {
				return SearchType.OneToOne;
			} else {
				return SearchType.Custom;
			}
		}
	}

	public SearchType getSelectedSearchType() {
		return SearchType.fromType(Integer.parseInt(getValue(getSelectedIndex())));
	}

	public void setSelectedSearchType(final SearchType type, final boolean storeUserPreference) {
		for (int i = 0; i < getItemCount(); i++) {
			if (getValue(i).equals(String.valueOf(type.getType()))) {
				setSelectedIndex(i);
				if (storeUserPreference) {
					storeSelectionInCookie();
				}
				return;
			}
		}
		throw new RuntimeException("Search type " + type.getType() + " not found amoung items");
	}

	private void storeSelectionInCookie() {
		if (getSelectedIndex() >= 0 && getSelectedIndex() < searchTypeEntries.length) {
			Cookies.setCookie(SEARCH_TYPE_COOKIE, searchTypeEntries[getSelectedIndex()].getCookie(), ParamSetSelectionController.getCookieExpirationDate(), null, "/", false);
		}
	}

	public void fireSelectionChanged() {
		SelectionChangeEvent.fire(this);
		storeSelectionInCookie();
	}

	@Override
	public void onClick(final ClickEvent event) {
		fireSelectionChanged();
	}

	@Override
	public void onChange(com.google.gwt.event.dom.client.ChangeEvent event) {
		fireSelectionChanged();
	}

	@Override
	public HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler) {
		return addHandler(handler, SelectionChangeEvent.getType());
	}

	private static final class SearchTypeEntry {
		private SearchType type;
		private String label;
		private String cookie;
		private int index;

		private SearchTypeEntry(final SearchType type, final String label, final String cookie) {
			this.type = type;
			this.label = label;
			this.cookie = cookie;
		}

		public SearchType getType() {
			return type;
		}

		public String getLabel() {
			return label;
		}

		public String getCookie() {
			return cookie;
		}

		public int getIndex() {
			return index;
		}
	}
}
