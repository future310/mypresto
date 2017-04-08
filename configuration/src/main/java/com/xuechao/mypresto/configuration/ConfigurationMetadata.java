package com.xuechao.mypresto.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.xuechao.mypresto.configuration.Problems.Monitor;

public class ConfigurationMetadata<T> {

	public static ConfigurationMetadata<T> getConfigurationMetadata(Class<T> configClass, Problems.Monitor monitor) {
		return new ConfigurationMetadata<T>();
	}

	private final Class<T> configClass;
	private final Problems problems;
	private final Constructor<T> constructor;
	private final Map<String, AttributeMetadata> attributes;
	private final Set<String> defunctConfig;

	private ConfigurationMetadata(Class<T> configClass, Monitor monitor) {
		if (configClass == null) {
			throw new NullPointerException("configClass is null");
		}
	}

	public static class AttributeMetadata {
		private final Class<?> configClass;
		private final String name;
		private final String description;
		private final Method getter;
		private final InjectionPointMetaData injectionPoint;
		private final Set<InjectionPointMetaData> legacyInjectionPoints;

		public AttributeMetadata(Class<?> configClass, String name, String description, Method getter,
				InjectionPointMetaData injectionPoint, Set<InjectionPointMetaData> legacyInjectionPoints) {
			Preconditions.checkNotNull(configClass);
			Preconditions.checkNotNull(name);
			Preconditions.checkNotNull(getter);
			Preconditions.checkNotNull(injectionPoint);
			Preconditions.checkNotNull(legacyInjectionPoints);

			this.configClass = configClass;
			this.name = name;
			this.description = description;
			this.getter = getter;
			this.injectionPoint = injectionPoint;
			this.legacyInjectionPoints = ImmutableSet.copyOf(legacyInjectionPoints);
		}

		public Class<?> getConfigClass() {
			return configClass;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public Method getGetter() {
			return getter;
		}

		public InjectionPointMetaData getInjectionPoint() {
			return this.injectionPoint;
		}

		public Set<InjectionPointMetaData> getLegacyInjectionPoints() {
			return this.legacyInjectionPoints;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			AttributeMetadata that = (AttributeMetadata) o;

			if (!configClass.equals(that.configClass)) {
				return false;
			}
			if (!name.equals(that.name)) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = configClass.hashCode();
			result = 31 * result + name.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("name", name).toString();
		}

	}

	public static class InjectionPointMetaData {
		private final Class<?> configClass;
		private final String property;
		private final Method setter;
		private final boolean current;

		private InjectionPointMetaData(Class<?> configClass, String property, Method setter, boolean current) {
			Preconditions.checkNotNull(configClass);
			Preconditions.checkNotNull(property);
			Preconditions.checkNotNull(setter);
			Preconditions.checkArgument(!property.isEmpty());
			this.configClass = configClass;
			this.property = property;
			this.setter = setter;
			this.current = current;
		}

		public Class<?> getConfigClass() {
			return this.configClass;
		}

		public String getProperty() {
			return this.property;
		}

		public Method getSetter() {
			return this.setter;
		}

		public boolean isLegacy() {
			return !this.current;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			InjectionPointMetaData that = (InjectionPointMetaData) o;

			if (!configClass.equals(that.configClass)) {
				return false;
			}
			if (!property.equals(that.property)) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = configClass.hashCode();
			result = 31 * result + property.hashCode();
			return result;
		}
	}
}
