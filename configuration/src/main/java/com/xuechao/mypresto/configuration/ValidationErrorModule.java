package com.xuechao.mypresto.configuration;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.spi.Message;

public class ValidationErrorModule implements Module {

	private final List<Message> messages;

	public ValidationErrorModule(List<Message> messages) {
		this.messages = ImmutableList.copyOf(messages);
	}

	@Override
	public void configure(Binder binder) {
		messages.forEach(binder::addError);
	}

}
