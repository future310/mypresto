package com.xuechao.mypresto.configuration;

@SuppressWarnings("serial")
public class InvalidConfigurationException extends Exception {

	public InvalidConfigurationException(String message, Object... orgs) {
		super(String.format(message, orgs));
	}

	public InvalidConfigurationException(Throwable cause, String message, Object... orgs) {
		super(String.format(message, orgs), cause);
	}
}
