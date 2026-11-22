package dev.daishin.soulspawner.client;

import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

// a slider over a fixed list of integer values
class StepSlider extends AbstractSliderButton
{
	private final String _label_key;
	private final List<Integer> _values;
	private final IntConsumer _on_change;
	private int _index = 0;
	private boolean _dragging = false;

	StepSlider(int x, int y, int width, int height, String label_key, List<Integer> values, IntConsumer on_change)
	{
		super(x, y, width, height, Component.empty(), 0);
		_label_key = label_key;
		_values = values;
		_on_change = on_change;
		active = values.size() > 1;
		updateMessage();
	}

	boolean isDragging() { return _dragging; }

	// shows a value from the server without sending it back
	void showValue(int value)
	{
		_index = Math.max(0, _values.indexOf(value));
		this.value = _values.size() > 1 ? (double) _index / (_values.size() - 1) : 0;
		updateMessage();
	}

	@Override
	protected void updateMessage() { setMessage(_values.isEmpty() ? Component.empty() : Component.translatable(_label_key, _values.get(_index))); }

	@Override
	protected void applyValue()
	{
		int index = (int) Math.round(value * (_values.size() - 1));
		if (index == _index) return;
		_index = index;
		_on_change.accept(_values.get(index));
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean double_click)
	{
		_dragging = true;
		super.onClick(event, double_click);
	}

	@Override
	public void onRelease(MouseButtonEvent event)
	{
		_dragging = false;
		showValue(_values.get(_index));
		super.onRelease(event);
	}
}
