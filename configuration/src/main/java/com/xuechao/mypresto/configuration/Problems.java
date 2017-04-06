package com.xuechao.mypresto.configuration;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.ConfigurationException;
import com.google.inject.spi.Message;

class Problems {
	private final List<Message> errors = Lists.newArrayList();
	private final List<Message> warnings = Lists.newArrayList();
	private final Monitor monitor;

	public interface Monitor {
		void onError(Message errorMessage);

		void onWarning(Message warningMessage);

	}

	private static final class NullMonitor implements Monitor {

		@Override
		public void onError(Message errorMessage) {
		}

		@Override
		public void onWarning(Message warningMessage) {
		}
	}

	public static final Problems.Monitor NULL_MONITOR = new NullMonitor();

	public Problems() {
		this.monitor = NULL_MONITOR;
	}

	public Problems(Monitor monitor) {
		this.monitor = monitor;
	}

	public void throwIfHasErrors() throws ConfigurationException {
		if (!errors.isEmpty()) {
			throw getException();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ConfigurationException getException() {
		ImmutableList<Message> messages = new ImmutableList.Builder().add(errors).add(warnings).build();
		return new ConfigurationException(messages);
	}

	public List<Message> getErrors() {
		return ImmutableList.copyOf(errors);
	}

	public List<Message> getWarnings() {
		return ImmutableList.copyOf(warnings);
	}

	public void addError(String format, Object... params) {
		Message message = new Message("Error: " + String.format(format, params));
		errors.add(message);
		monitor.onError(message);
	}

	public void addError(Throwable e, String format, Object... params) {
		Message message = new Message(Collections.emptyList(), "Erros: " + String.format(format, params), e);
		errors.add(message);
		monitor.onError(message);
	}

	public void addWarning(String format, Object... params) {
		Message message = new Message("Warning: " + String.format(format, params));
		warnings.add(message);
		monitor.onWarning(message);
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Message error : errors) {
			builder.append(error.getMessage()).append('\n');
		}
		for (Message warning : warnings) {
			builder.append(warning.getMessage()).append('\n');
		}
		return builder.toString();
	}

	public static ConfigurationException exceptionFor(String format, Object... params) {
		Problems problems = new Problems();
		problems.addError(format, params);
		return problems.getException();
	}

	public static ConfigurationException exceptionFor(Throwable e, String format, Object... params) {
		Problems problems = new Problems();
		problems.addError(e, format, params);
		return problems.getException();
	}
}
