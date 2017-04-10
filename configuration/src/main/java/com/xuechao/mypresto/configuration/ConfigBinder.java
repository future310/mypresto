package com.xuechao.mypresto.configuration;

import java.util.Objects;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;

public class ConfigBinder {

	private final Binder binder;

	public static ConfigBinder configBinder(Binder binder) {
		return new ConfigBinder(binder);
	}

	private ConfigBinder(Binder binder) {
		this.binder = Objects.requireNonNull(binder, "binder in null").skipSources(getClass());
	}

	public <T> void bindConfig(Key<T> key, Class<T> configClass, String prefix) {
		binder.bind(key).toProvider(new ConfigurationProvider<T>(key, configClass, prefix));
		createConfigDefaultsBinder(key);
	}

	public <T> void bindConfigDefaults(Key<T> key, ConfigDefaults<T> configDefaults) {
		createConfigDefaultsBinder(key).addBinding().toInstance(new ConfigDefaultsHolder<>(key, configDefaults));
	}

	private <T> Multibinder<ConfigDefaultsHolder<T>> createConfigDefaultsBinder(Key<T> key) {
		return null;
	}
}
