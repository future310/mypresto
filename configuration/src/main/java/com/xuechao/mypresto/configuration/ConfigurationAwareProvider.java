package com.xuechao.mypresto.configuration;

import com.google.inject.Provider;

public interface ConfigurationAwareProvider<T> extends Provider<T> {
	void setConfigurationFactory(ConfigurationFactory configurationFactory);

	void setWarningsMonitor(WarningsMonitor warningsMonitor);
}
