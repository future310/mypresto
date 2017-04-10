package com.xuechao.mypresto.configuration;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Key;

public class ConfigurationProvider<T> implements ConfigurationAwareProvider<T> {

	private final Key<T> key;
	private final Class<T> configClass;
	private final String prefix;
	private ConfigurationFactory configurationFactory;
	private WarningsMonitor warningsMonitor;

	public ConfigurationProvider(Key<T> key, Class<T> configClass, String prefix) {
		Preconditions.checkNotNull(key, "key");
		Preconditions.checkNotNull(configClass, "configClass");
		this.key = key;
		this.configClass = configClass;
		this.prefix = prefix;
	}

	@Override
	public T get() {
		Preconditions.checkNotNull(configurationFactory, "configurationFactory");
		return null;
	}

	@Override
	@Inject
	public void setConfigurationFactory(ConfigurationFactory configurationFactory) {
		this.configurationFactory = configurationFactory;
	}

	@Override
	@Inject
	public void setWarningsMonitor(WarningsMonitor warningsMonitor) {
		this.warningsMonitor = warningsMonitor;
	}

	public Key<T> getKey() {
		return key;
	}

	public Class<T> getConfigClass() {
		return configClass;
	}

	public String getPrefix() {
		return prefix;
	}

	public ConfigurationMetadata<T> getConfigurationMetadata() {
		return ConfigurationMetadata.getConfigurationMetadata(configClass);
	}
	/*
	 * public T getDefaultConfig(){ return configurationFactory.get }
	 */

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ConfigurationProvider<?> that = (ConfigurationProvider<?>) o;

		if (!key.equals(that.key)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

}
