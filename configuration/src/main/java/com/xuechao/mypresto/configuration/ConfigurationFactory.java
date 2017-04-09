package com.xuechao.mypresto.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.inject.Key;
import com.xuechao.mypresto.configuration.Problems.Monitor;

public class ConfigurationFactory {
	private final Map<String, String> properties;
	private final Problems.Monitor monitor;
	private final Set<String> usedProperties = Sets.newConcurrentHashSet();
	private final ListMultimap<Key<?>, ConfigDefaultsHolder<?>> registeredDefaultConfigs = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());
	private final LoadingCache<Class<?>, ConfigurationMetadata<?>> metadataCache = CacheBuilder.newBuilder()
			.build(new CacheLoader<Class<?>, ConfigurationMetadata<?>>() {
				@Override
				public ConfigurationMetadata<?> load(Class<?> configClass) throws Exception {
					return ConfigurationMetadata.getConfigurationMetadata(configClass, Problems.NULL_MONITOR);
				}

			});

	public ConfigurationFactory(Map<String, String> properties) {
		this(properties, Problems.NULL_MONITOR);
	}

	public ConfigurationFactory(Map<String, String> properties, Monitor monitor) {
		this.monitor = monitor;
		this.properties = ImmutableMap.copyOf(properties);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void consumeProperty(String property) {
		Preconditions.checkNotNull(property, "property is null");
		usedProperties.add(property);
	}

	public Set<String> getUsedProperties() {
		return ImmutableSortedSet.copyOf(usedProperties);
	}

	public <T> void registerConfigDefaults(ConfigDefaultsHolder<T> holder) {
		registeredDefaultConfigs.put(holder.getConfigKey(), holder);
	}

	private <T> ConfigDefaults<T> getConfigDefaults(Key<T> key) {
		@SuppressWarnings("unchecked")
		List<ConfigDefaults<T>> defaults = registeredDefaultConfigs.get(key).stream()
				.map(holder -> (ConfigDefaultsHolder<T>) holder).sorted().map(ConfigDefaultsHolder::getConfigDefaults)
				.collect(Collectors.toList());
		return ConfigDefaults.configDefaults(defaults);
	}

}
