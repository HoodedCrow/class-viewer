package classviewer.changes;

import classviewer.model.Source;

public abstract class FieldChange<T> extends Change {
	/** Description of this change. */
	private String field;
	private T newValue;
	private T oldValue;

	public FieldChange(Source source, String field, T newValue, T oldValue,
			Object object) {
		super(source, Change.MODIFY, object);
		this.field = field;
		this.newValue = newValue;
		this.oldValue = oldValue;
	}

	@Override
	public String getDescription() {
		return field;
	}

	@Override
	public Object getNewValue() {
		return newValue;
	}

	@Override
	public Object getOldValue() {
		return oldValue;
	}
}
