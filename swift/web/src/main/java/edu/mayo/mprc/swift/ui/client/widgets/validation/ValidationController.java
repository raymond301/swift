package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.UIObject;
import edu.mayo.mprc.swift.ui.client.dialogs.ErrorDialog;
import edu.mayo.mprc.swift.ui.client.dialogs.Validatable;
import edu.mayo.mprc.swift.ui.client.dialogs.ValidationPanel;
import edu.mayo.mprc.swift.ui.client.rpc.*;
import edu.mayo.mprc.swift.ui.client.service.ServiceAsync;
import edu.mayo.mprc.swift.ui.client.widgets.Callback;
import edu.mayo.mprc.swift.ui.client.widgets.ParamSetSelectionController;
import edu.mayo.mprc.swift.ui.client.widgets.ParamSetSelectionListener;

import java.util.*;

/**
 * Controller object responsible for delivering events to and receiving events from child widgets,
 * and associating those widgets and events with Params objects from the server.  This
 * is intended to separate out the logic of handling updates from the physical
 * layout of the various widgets on the screen.
 */

public final class ValidationController implements ValueChangeHandler<ClientValue>, HasValueChangeHandlers<ClientValue>, ParamSetSelectionListener {

	private ClientParamSet paramSet;
	private ClientParamSetValues values;
	private ServiceAsync serviceAsync;
	private ParamSetSelectionController selector;
	private EventBus eventBus = new SimpleEventBus();

	/**
	 * Maps parameter id strings one-to-one onto registrations.
	 */
	private Map<java.lang.String, ValidationController.Registration> byParam = new HashMap();

	/**
	 * Maps widgets to registrations.
	 */
	private Map<Validatable, ValidationController.Registration> byWidget = new HashMap();

	private Map<ValidationPanel, ValidationController.Registration> byValidationPanel = new HashMap();

	/**
	 * List of all the registrations with invalid (error or worse) settings.
	 */
	private HashSet<ValidationController.Registration> invalid = new HashSet();
	private Registration awaitingUpdate = null;
	private ClientValue awaitingUpdateValue = null;
	// Cached list of allowed values
	private HashMap<String, List<ClientValue>> allowedValues = new HashMap<String, List<ClientValue>>(10);

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<ClientValue> handler) {
		return eventBus.addHandler(ValueChangeEvent.getType(), handler);
	}

	@Override
	public void fireEvent(GwtEvent<?> event) {
		eventBus.fireEventFromSource(event, this);
	}

	@Override
	public void onValueChange(ValueChangeEvent<ClientValue> event) {
		final Validatable v = (Validatable) event.getSource();
		if (v == null) {
			throw new RuntimeException("ValidationController received change event for non Validatable widget");
		}
		final Registration r = (Registration) byWidget.get(v);
		if (r == null) {
			throw new RuntimeException("ValidationController received changed event for unregistered Validatable");
		}

		if (awaitingUpdate != null) {
			// ignore duplicate events while we're creating the temporary.
			return;
		}

		if (r.getV().getValue() == null) {
			if (r.getCv() != null) {  // TODO how to deal with locally generated validations?
				//ignore
				return;
			} else {
				throw new RuntimeException("Widget returned null value for " + r.getParam());
			}
		}

		// determine whether we need to make a temporary.

		if (!paramSet.isTemporary()) {
			setEnabled(false); //prevent users from making more changes while we're doing the
			// complex machinations of making the temporary paramset.
			awaitingUpdate = r;

			// must cache the users's requested change so it will be applied to the
			// correct ParamSet.
			awaitingUpdateValue = r.getV().getValue();
			createTemporary(r, r.getV().getValue());
		} else {
			doUpdate(r, r.getV().getValue());
		}
	}

	/**
	 * Each widget has a registration that associates the various pieces together.
	 */
	private static final class Registration {
		private Validatable v;
		private ValidationPanel validationPanel;
		private ClientValidationList cv;
		private String param;
		private List<ClientValue> allowedValues;

		private Registration(final Validatable v, final String param, final ValidationPanel validationPanel) {
			this.v = v;
			this.validationPanel = validationPanel;
			this.param = param;
		}

		public Validatable getV() {
			return v;
		}

		public ValidationPanel getValidationPanel() {
			return validationPanel;
		}

		public ClientValidationList getCv() {
			return cv;
		}

		public String getParam() {
			return param;
		}

		public List<ClientValue> getAllowedValues() {
			return allowedValues;
		}
	}


	public ValidationController(final ServiceAsync serviceAsync, final ParamSetSelectionController selector, final InitialPageData pageData) {
		this.serviceAsync = serviceAsync;
		this.selector = selector;
		allowedValues = pageData.getAllowedValues();
		selector.addParamSetSelectionListener(this);
	}

	/**
	 * Register the given Validatable widget as responding to updates for the given param.
	 *
	 * @param v          The widget to register for.
	 * @param param      The param to associate this Validatable with.
	 * @param validation The ValidationPanel in which to place errors/warnings received for the given param.
	 */
	public void add(final Validatable v, final String param, final ValidationPanel validation) {
		final Registration reg = new Registration(v, param, validation);
		byParam.put(param, reg);
		byWidget.put(v, reg);
		byValidationPanel.put(validation, reg);
		v.addValueChangeHandler(this);
	}

	public void update(final String paramId, final ClientValidationList cv) {
		update(paramId, null, cv, new HashSet<Validatable>());
	}

	/**
	 * Update the widget associated with the given param.  The given value is used
	 * in preference to the value in the ClientValidation.
	 *
	 * @param paramId
	 * @param value
	 * @param ccv
	 * @param visited HashSet of validatables which have been visited during this
	 *                round of updates.  Validatables which appear in this hash don't have their
	 *                validations cleared in case there are multiple validations for a given
	 *                Validatable.
	 */
	public void update(final String paramId,
	                   ClientValue value,
	                   final ClientValidationList ccv,
	                   final HashSet<Validatable> visited) {
		final Registration rr = byParam.get(paramId);
		if (value == null && ccv != null) {
			value = ccv.getValue();
		}
		if (rr == null) {
			throw new RuntimeException("Can't find registration for " + paramId);
		}
		// if we haven't yet seen this Validatable yet for this update, then
		// remove all the existing validations for it.
		if (!visited.contains(rr.getV())) {
			rr.getValidationPanel().removeValidationsFor(rr.getV());
			visited.add(rr.getV());
		}
		if (ccv != null) {
			rr.cv = ccv;
			final int sev = ccv.getWorstSeverity();
			rr.getV().setValidationSeverity(sev);
			if (sev > ClientValidation.SEVERITY_WARNING) {
				invalid.add(rr);
			} else {
				invalid.remove(rr);
			}
			for (final ClientValidation v : ccv) {
				if (v.getSeverity() != ClientValidation.SEVERITY_NONE) {
					rr.getValidationPanel().addValidation(v.shallowCopy(), rr.getV());
				}
			}
		} else {
			rr.getV().setValidationSeverity(ClientValidation.SEVERITY_NONE);
			invalid.remove(rr);
		}
		if (rr.getAllowedValues() != null) {
			rr.getV().setAllowedValues(rr.getAllowedValues());
		}
		final boolean validationDefinesNullValue = value == null && (ccv != null && !ccv.isEmpty());
		final boolean validationDefinesValue = validationDefinesNullValue || value != null;
		if (rr.getV().getValue() == null || (validationDefinesValue && !rr.getV().getValue().equals(value))) {
			rr.getV().setValue(value);
		}
	}

	private void createTemporary(final Registration r, final ClientValue value) {
		serviceAsync.save(paramSet, null, null, null,
				false, new AsyncCallback<ClientParamSet>() {
			@Override
			public void onFailure(final Throwable throwable) {
				ErrorDialog.handleGlobalError(throwable);
			}

			@Override
			public void onSuccess(final ClientParamSet newParamSet) {
				selector.refresh(new Callback() {
					@Override
					public void done() {
						selector.select(newParamSet);
						finishedUpdating();
					}
				});
			}
		});
	}

	private void doUpdate(final Registration r, final ClientValue value) {
		serviceAsync.update(paramSet, r.getParam(), value, new UpdateCallback(r));
	}

	private class UpdateCallback implements AsyncCallback<ClientParamsValidations> {
		private Registration r;

		UpdateCallback(final Registration r) {
			this.r = r;
		}

		@Override
		public void onFailure(final Throwable throwable) {
			r.getValidationPanel().addValidation(new ClientValidation(throwable.toString()), r.getV());
		}

		@Override
		public void onSuccess(final ClientParamsValidations o) {
			final HashSet<Validatable> visited = new HashSet<Validatable>();
			// Update all validations to clear them
			for (final Registration r : byWidget.values()) {
				final String paramId = r.getParam();
				update(paramId, null);
			}
			// Update all others
			for (final Map.Entry<String, ClientValidationList> entry : o.getValidationMap().entrySet()) {
				final String paramId = entry.getKey();
				final ClientValidationList cv = entry.getValue();
				update(paramId, cv.getValue(), cv, visited);
			}
			finishedUpdating();
		}
	}

	public void updateDependent(final Callback cb) {
		fetchAllowedValues(byWidget.keySet(), new Callback() {

			@Override
			public void done() {
				final List<ClientParam> vals = values.getValues();
				final HashSet<Validatable> visited = new HashSet<Validatable>();
				for (final ClientParam val : vals) {
					update(val.getParamId(), val.getValue(), val.getValidationList(), visited);
				}
				finishedUpdating();
				if (cb != null) {
					cb.done();
				}
			}
		});

	}

	public boolean isValid() {
		return invalid.isEmpty();
	}

	public void getAllowedValuesForValidatable(final Validatable v, final Callback cb) {
		fetchAllowedValues(Arrays.asList(v), cb);
	}

	/**
	 * Fetches a list of allowed values for a list of widgets.
	 * Consults a local cache not to go back to the server.
	 * If you want to bypass the caching, call {@link #cleanAllowedValues} for the widgets to refetch values for.
	 *
	 * @param widgets List of {@link Validatable} to fetch allowed values for.
	 * @param cb      Callback to call when done.
	 */
	private void fetchAllowedValues(Collection<Validatable> widgets, final Callback cb) {
		final List<String> params = new ArrayList<String>();
		final List<String> paramsToFetch = new ArrayList<String>();
		final List<List<ClientValue>> cachedValues = new ArrayList<List<ClientValue>>();

		for (final Validatable widget : widgets) {
			final Registration r = byWidget.get(widget);
			// We do not have this value cached, so it will be set to change
			if (widget.needsAllowedValues() && r.getAllowedValues() == null && !allowedValues.containsKey(r.getParam())) {
				paramsToFetch.add(r.getParam());
			} else {
				params.add(r.getParam());
				cachedValues.add(allowedValues.get(r.getParam()));
			}
		}

		if (params.size() + paramsToFetch.size() == 0) {
			if (cb != null) {
				cb.done();
			}
			return;
		}

		if (!paramsToFetch.isEmpty()) {
			serviceAsync.getAllowedValues(
					paramsToFetch.toArray(new String[paramsToFetch.size()]),
					new AsyncCallback<List<List<ClientValue>>>() {

						@Override
						public void onFailure(final Throwable throwable) {
							ErrorDialog.handleGlobalError(throwable);
						}

						@Override
						public void onSuccess(final List<List<ClientValue>> allowedValues) {
							allowedValues.addAll(cachedValues);
							paramsToFetch.addAll(params);
							allowedValuesArrived(allowedValues, paramsToFetch, cb);
						}
					});
		} else {
			allowedValuesArrived(cachedValues, params, cb);
		}
	}

	private void allowedValuesArrived(List<List<ClientValue>> allowedValues, List<String> params, Callback cb) {
		if (allowedValues.size() != params.size()) {
			throw new RuntimeException("Incorrect number of allowed values returned.");
		}
		final HashSet<Validatable> visited = new HashSet<Validatable>();
		for (int i = 0; i < params.size(); ++i) {
			final Registration r = byParam.get(params.get(i));
			r.allowedValues = allowedValues.get(i);
			this.allowedValues.put(r.getParam(), r.allowedValues);
			update(r.getParam(), null, r.getCv(), visited);
		}

		if (cb != null) {
			cb.done();
		}
	}

	/**
	 * Forces the refetch of given allowed values.
	 */
	public void getAllowedValues(final Validatable v, final Callback cb) {
		cleanAllowedValues(v);
		fetchAllowedValues(Arrays.asList(v), cb);
	}

	public void cleanAllowedValues(Validatable v) {
		final Registration r = byWidget.get(v);
		r.allowedValues = null;
		allowedValues.remove(r.getParam());
	}

	/**
	 * Set the styles on a widget based on a validation severity.
	 * <p/>
	 * TODO this probably doesn't belong here, but where to put it?
	 */
	static void setValidationSeverity(final int validationSeverity, final UIObject o) {
		switch (validationSeverity) {
			case ClientValidation.SEVERITY_ERROR:
				o.addStyleName("severity-Error");
				o.removeStyleName("severity-Warning");
				break;
			case ClientValidation.SEVERITY_WARNING:
				o.addStyleName("severity-Warning");
				o.removeStyleName("severity-Error");
				break;
			default:
				o.removeStyleName("severity-Warning");
				o.removeStyleName("severity-Error");
		}
	}

	public void setEnabled(final boolean enabled) {
		for (final Registration r : byWidget.values()) {
			r.getV().setEnabled(enabled);
		}
	}

	private void finishedUpdating() {
		ValueChangeEvent.fire(this, null);
		setEnabled(true);
	}

	/**
	 * Fired whenever the selection changes.
	 */
	@Override
	public void selected(final ClientParamSet selection) {
		final ClientParamSet sel = selection;
		if (paramSet == null || !paramSet.equals(sel)) {
			setEnabled(false);
			if (awaitingUpdate != null) {
				// we've just created a temporary and we need to install the value that
				// the user changed that caused us to request the temporary.
				paramSet = sel;
				doUpdate(awaitingUpdate, awaitingUpdateValue);
				awaitingUpdate = null;
				awaitingUpdateValue = null;

			} else {

				serviceAsync.getParamSetValues(sel, new AsyncCallback<ClientParamSetValues>() {
					@Override
					public void onFailure(final Throwable throwable) {
						ErrorDialog.handleGlobalError(throwable);
					}

					@Override
					public void onSuccess(final ClientParamSetValues newValues) {
						if (newValues == null) {
							throw new RuntimeException("Didn't get a ClientParamSet");
						}
						values = newValues;
						paramSet = sel;
						updateDependent(null);
					}
				});
			}
		} else {
			setEnabled(true);
		}
	}
}
