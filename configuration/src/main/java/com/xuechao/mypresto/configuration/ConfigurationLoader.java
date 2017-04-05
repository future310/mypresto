package com.xuechao.mypresto.configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;

public class ConfigurationLoader {
	public Map<String, String> loadProperties() throws IOException {
		Map<String, String> results = Maps.newTreeMap();
		String configFile = System.getProperty("config");
		if (configFile != null) {
			results.putAll(loadPropertiesFrom(configFile));
		}
		results.putAll(getSystemProperties());
		return results;
	}

	public Map<String, String> loadPropertiesFrom(String path) throws IOException {
		Properties properties = new Properties();
		try (Reader reader = new FileReader(new File(path))) {
			properties.load(reader);
		}
		return Maps.fromProperties(properties);
	}

	public Map<String, String> getSystemProperties() {
		return Maps.fromProperties(System.getProperties());
	}

}
