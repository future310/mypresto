package com.xuechao.mypresto.configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.collections.Maps;
import org.testng.collections.Sets;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.ConfigurationException;
import com.xuechao.mypresto.configuration.Problems.Monitor;

public class ConfigurationMetadata<T> {
	public static <T> ConfigurationMetadata<T> getValidConfigurationMetadata(Class<T> configClass)
			throws ConfigurationException {
		return getValidConfigurationMetadata(configClass, Problems.NULL_MONITOR);
	}

	static <T> ConfigurationMetadata<T> getValidConfigurationMetadata(Class<T> configClass, Problems.Monitor monitor)
			throws ConfigurationException {
		ConfigurationMetadata<T> metadata = getConfigurationMetadata(configClass, monitor);
		metadata.getProblems().throwIfHasErrors();
		return metadata;
	}

	public static <T> ConfigurationMetadata<T> getConfigurationMetadata(Class<T> configClass) {
		return getConfigurationMetadata(configClass, Problems.NULL_MONITOR);
	}

	static <T> ConfigurationMetadata<T> getConfigurationMetadata(Class<T> configClass, Problems.Monitor monitor) {
		return new ConfigurationMetadata<T>(configClass, monitor);
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
		this.problems = new Problems(monitor);
		this.configClass = configClass;
		if (Modifier.isAbstract(configClass.getModifiers())) {
			problems.addError("Config class [%s] is abstract", configClass.getName());
		}
		if (!Modifier.isPublic(configClass.getModifiers())) {
			problems.addError("Config class [%s] is not public", configClass.getName());
		}
		this.defunctConfig = Sets.newHashSet();
		if (configClass.isAnnotationPresent(DefunctConfig.class)) {
			DefunctConfig defunctConfig = configClass.getAnnotation(DefunctConfig.class);
			if (defunctConfig.value().length < 1) {
				problems.addError("@DefunctConfig annotation on class [%s] is empty", configClass.getName());
			}
			for (String defunct : configClass.getAnnotation(DefunctConfig.class).value()) {
				if (defunct.isEmpty()) {
					problems.addError("@DefunctConfig annotation on class [%s] contains empty values",
							configClass.getName());
				} else if (!this.defunctConfig.add(defunct)) {
					problems.addError("Defunct property '%s' is listed more than once in @DefunctConfig for class [%s]",
							defunct, configClass.getName());
				}
			}
		}
		Constructor<T> constructor = null;
		try {
			constructor = configClass.getDeclaredConstructor();
			if (Modifier.isPublic(constructor.getModifiers())) {
				problems.addError("Constructor [%s] is not public", constructor.toGenericString());
			}
		} catch (Exception e) {
			problems.addError("Configuration class [%s] does not have a public no-arg constructor",
					configClass.getName());
		}
		this.constructor = constructor;
		this.attributes = buildAttributeMetadata(configClass);
		for (Class<?> clazz = configClass; clazz != null
				&& !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(Config.class)) {
					if (!Modifier.isPublic(method.getModifiers())) {
						problems.addError("@Config method [%s] is not public", method.toGenericString());
					}
					if (Modifier.isStatic(method.getModifiers())) {
						problems.addError("@Config method [%s] is static", method.toGenericString());
					}
				}
			}
		}
		if (problems.getErrors().isEmpty() && this.attributes.isEmpty()) {
			problems.addError("Configuration class [%s] does not have any @Config annotations", configClass.getName());
		}
	}

	public Class<T> getConfigClass() {
		return configClass;
	}

	public Constructor<T> getConstructor() {
		return constructor;
	}

	public Map<String, AttributeMetadata> getAttributes() {
		return attributes;
	}

	Problems getProblems() {
		return problems;
	}

	private Map<String, AttributeMetadata> buildAttributeMetadata(Class<T> configClass) {
		Map<String, AttributeMetadata> attributes = Maps.newHashMap();
		for (Method configMethod : findConfigMethods(configClass)) {
			AttributeMetadata attribute = buildAttributeMetadata(configClass, configMethod);
			if (attribute != null) {
				if (attributes.containsKey(attribute.getName())) {
					problems.addError(
							"Configuration class [%s] Multiple methods are annotated for @Config attribute [%s]",
							configClass.getName(), attribute.getName());
				}
				attributes.put(attribute.getName(), attribute);
			}
		}
		Collection<Method> legacyMethods = findLegacyConfigMethods(configClass);
		for (AttributeMetadata attribute : attributes.values()) {
			for (InjectionPointMetaData injectionPoint : attribute.getLegacyInjectionPoints()) {
				if (legacyMethods.contains(injectionPoint.getSetter())) {
					legacyMethods.remove(injectionPoint.getSetter());
				}
			}
		}
		for (Method method : legacyMethods) {
			if (!method.isAnnotationPresent(Config.class)) {
				validateSetter(method);
				problems.addError("@LegacyConfig method [%s] is not associated with any valid @Config attribute.",
						method.toGenericString());
			}
		}
		return attributes;
	}

	private AttributeMetadata buildAttributeMetadata(Class<T> configClass, Method configMethod) {
		Preconditions.checkArgument(configMethod.isAnnotationPresent(Config.class));
		if (!validateAnnotations(configMethod)) {
			return null;
		}
		String propertyName = configMethod.getAnnotation(Config.class).value();
		if (!validateSetter(configMethod)) {
			return null;
		}
		String attributeName = configMethod.getName().substring(3);
		AttributeMetaDataBuilder builder = new AttributeMetaDataBuilder(configClass, attributeName);
		if (configMethod.isAnnotationPresent(ConfigDescription.class)) {
			builder.setDescription(configMethod.getAnnotation(ConfigDescription.class).value());
		}
		Method getter = findGetter(configClass, configMethod, attributeName);
		if (getter != null) {
			builder.setGetter(getter);
			if (configMethod.isAnnotationPresent(Deprecated.class) != getter.isAnnotationPresent(Deprecated.class)) {
				problems.addError("Methods [%s] and [%s] must be @Deprecated together", configMethod, getter);
			}
		}
		if (defunctConfig.contains(propertyName)) {
			problems.addError("@Config property '%s' on method [%s] is defunct on class [%s]", propertyName,
					configMethod, configClass);
		}
		builder.addInjectionPoint(InjectionPointMetaData.newCurrent(configClass, propertyName, configMethod));

		for (InjectionPointMetaData injectionPoint : findLegacySetters(configClass, propertyName, attributeName)) {
			if (!injectionPoint.getSetter().isAnnotationPresent(Config.class)
					&& !injectionPoint.getSetter().isAnnotationPresent(Deprecated.class)) {
				problems.addWarning("Replaced @LegacyConfig method [%s] should be @Deprecated",
						injectionPoint.getSetter().toGenericString());
			}
			builder.addInjectionPoint(injectionPoint);
		}
		return builder.build();
	}

	private static Collection<Method> findLegacyConfigMethods(Class<?> configClass) {
		return findAnnotatedMethods(configClass, LegacyConfig.class);
	}

	private static Collection<Method> findAnnotatedMethods(Class<?> configClass,
			Class<? extends Annotation> annotation) {
		List<Method> result = new ArrayList<>();
		for (Method method : configClass.getMethods()) {
			if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())) {
				continue;
			}
			Method managedMethod = findAnnotatedMethod(configClass, annotation, method.getName(),
					method.getParameterTypes());
			if (managedMethod != null) {
				result.add(managedMethod);
			}
		}
		return result;
	}

	private static Collection<Method> findConfigMethods(Class<?> configClass) {
		return findAnnotatedMethods(configClass, Config.class);
	}

	public static Method findAnnotatedMethod(Class<?> configClass, Class<? extends Annotation> annotation,
			String methodName, Class<?>... parameterTypes) {
		Method method;
		try {
			method = configClass.getDeclaredMethod(methodName, parameterTypes);
			if (method != null && method.isAnnotationPresent(annotation)) {
				return method;
			}
		} catch (Exception e) {
		}
		if (configClass.getSuperclass() != null) {
			Method managedMethod = findAnnotatedMethod(configClass.getSuperclass(), annotation, methodName,
					parameterTypes);
			if (managedMethod != null) {
				return managedMethod;
			}
		}
		for (Class<?> interfac : configClass.getInterfaces()) {
			Method managedMethod = findAnnotatedMethod(interfac, annotation, methodName, parameterTypes);
			if (managedMethod != null) {
				return managedMethod;
			}
		}
		return null;
	}

	private Method findGetter(Class<?> configClass, Method configMethod, String attributeName) {
		String getterName = "get" + attributeName;
		String isName = "is" + attributeName;
		List<Method> getters = new ArrayList<>();
		List<Method> unusableGetters = new ArrayList<>();
		for (Class<?> clazz = configClass; clazz != null
				&& !Object.class.equals(clazz.getSuperclass()); clazz = configClass.getSuperclass()) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (method.getName().equals(getterName) || method.getName().equals(isName)) {
					if (isUsableMethod(method) && !method.getReturnType().equals(Void.TYPE)
							&& method.getParameterTypes().length == 0) {
						getters.add(method);
					} else {
						unusableGetters.add(method);
					}
				}
			}
		}
		if (getters.isEmpty()) {
			String unusable = "";
			if (!unusableGetters.isEmpty()) {
				StringBuilder builder = new StringBuilder(" The following methods are unusable: ");
				for (Method method : unusableGetters) {
					builder.append('[').append(method.toGenericString()).append('[');
				}
				unusable = builder.toString();
			}
			problems.addError("No getter for @Config method [%s].%s", configMethod.toGenericString(), unusable);
			return null;
		}
		if (getters.size() > 1) {
			problems.addError("Multiple getters found for @Config setter [%s]", configMethod.toGenericString());
			return null;
		}
		return getters.get(0);
	}

	private Set<InjectionPointMetaData> findLegacySetters(Class<?> configClass, String propertyName,
			String attributeName) {
		Set<InjectionPointMetaData> setters = Sets.newHashSet();

		String setterName = "set" + attributeName;

		for (Class<?> clazz = configClass; (clazz != null)
				&& !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (isUsableMethod(method)) {
					if (method.getName().equals(setterName) && method.isAnnotationPresent(LegacyConfig.class)) {
						// Found @LegacyConfig setter with matching attribute
						// name
						if (validateSetter(method)) {
							for (String property : method.getAnnotation(LegacyConfig.class).value()) {
								if (defunctConfig.contains(property)) {
									problems.addError(
											"@LegacyConfig property '%s' on method [%s] is defunct on class [%s]",
											property, method, configClass);
								}

								if (!property.equals(propertyName)) {
									setters.add(InjectionPointMetaData.newLegacy(configClass, property, method));
								} else {
									problems.addError(
											"@LegacyConfig property '%s' on method [%s] is replaced by @Config property of same name on method [%s]",
											property, method.toGenericString(), setterName);
								}
							}
						}
					} else if (method.isAnnotationPresent(LegacyConfig.class)
							&& method.getAnnotation(LegacyConfig.class).replacedBy().equals(propertyName)) {
						// Found @LegacyConfig setter linked by replacedBy()
						// property
						if (validateSetter(method)) {
							for (String property : method.getAnnotation(LegacyConfig.class).value()) {
								if (defunctConfig.contains(property)) {
									problems.addError(
											"@LegacyConfig property '%s' on method [%s] is defunct on class [%s]",
											property, method, configClass);
								}

								if (!property.equals(propertyName)) {
									setters.add(InjectionPointMetaData.newLegacy(configClass, property, method));
								} else {
									problems.addError(
											"@LegacyConfig property '%s' on method [%s] is replaced by @Config property of same name on method [%s]",
											property, method.toGenericString(), setterName);
								}
							}
						}
					}
				}
			}
		}
		return setters;
	}

	private boolean validateAnnotations(Method configMethod) {
		Config config = configMethod.getAnnotation(Config.class);
		LegacyConfig legacyConfig = configMethod.getAnnotation(LegacyConfig.class);
		if (config == null) {
			problems.addError("Method [%s] must have @Config annotation", configMethod.toGenericString());
			return false;
		}
		boolean isValid = true;
		if (config.value().isEmpty()) {
			problems.addError("@Config method [%s] annotation has an empty value", configMethod.toGenericString());
			isValid = false;
		}

		if (legacyConfig != null) {
			if (legacyConfig.value().length == 0) {
				problems.addError("@LegacyConfig method [%s] annotation has an empty list",
						configMethod.toGenericString());
				isValid = false;
			}
			if (!legacyConfig.replacedBy().isEmpty()) {
				problems.addError(
						"@Config method [%s] has annotation claiming to be replaced by another property ('%s')",
						configMethod.toGenericString(), legacyConfig.replacedBy());
				isValid = false;
			}
			for (String arrayEntry : legacyConfig.value()) {
				if (arrayEntry == null || arrayEntry.isEmpty()) {
					problems.addError("@LegacyConfig method [%s] annotation contains null or empty value",
							configMethod.toGenericString());
					isValid = false;
				} else if (arrayEntry.equals(config.value())) {
					problems.addError("@Config property name '%s' appears in @LegacyConfig annotation for method [%s]",
							config.value(), configMethod.toGenericString());
					isValid = false;
				}
			}
		}
		return isValid;
	}

	private boolean validateSetter(Method method) {
		if (method == null) {
			return false;
		}

		if (!method.getName().startsWith("set")) {
			problems.addError("Method [%s] is not a valid setter (e.g. setFoo) for configuration annotation",
					method.toGenericString());
			return false;
		}

		if (method.getParameterTypes().length != 1) {
			problems.addError("Configuration setter method [%s] does not have exactly one parameter",
					method.toGenericString());
			return false;
		}

		return true;
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

		public static InjectionPointMetaData newCurrent(Class<?> configClass, String property, Method setter) {
			return new InjectionPointMetaData(configClass, property, setter, true);
		}

		public static InjectionPointMetaData newLegacy(Class<?> configClass, String property, Method setter) {
			return new InjectionPointMetaData(configClass, property, setter, false);
		}

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

	public static class AttributeMetaDataBuilder {
		private final Class<?> configClass;
		private final String name;
		private String description;
		private Method getter;
		private InjectionPointMetaData injectionPoint;
		private final Set<InjectionPointMetaData> legacyInjectionPoints = Sets.newHashSet();

		public AttributeMetaDataBuilder(Class<?> configClass, String name) {
			Preconditions.checkNotNull(configClass);
			Preconditions.checkNotNull(name);
			Preconditions.checkArgument(!name.isEmpty());
			this.configClass = configClass;
			this.name = name;
		}

		public void setDescription(String description) {
			Preconditions.checkNotNull(description);
			this.description = description;
		}

		public void setGetter(Method getter) {
			Preconditions.checkNotNull(getter);
			this.getter = getter;
		}

		public void addInjectionPoint(InjectionPointMetaData injectionPointMetaData) {
			Preconditions.checkNotNull(injectionPointMetaData);
			if (injectionPointMetaData.isLegacy()) {
				this.legacyInjectionPoints.add(injectionPointMetaData);
				return;
			}
			if (this.injectionPoint != null) {
				throw Problems.exceptionFor(
						"Trying to set current property twice: '%s' on method [%s] and '%s' on method [%s]",
						this.injectionPoint.getProperty(), this.injectionPoint.getSetter().toGenericString(),
						injectionPointMetaData.getProperty(), injectionPointMetaData.getSetter().toGenericString());
			}
			this.injectionPoint = injectionPointMetaData;
		}

		public AttributeMetadata build() {
			if (getter == null) {
				return null;
			}
			return new AttributeMetadata(configClass, name, description, getter, injectionPoint, legacyInjectionPoints);
		}
	}

	private static boolean isUsableMethod(Method method) {
		return !method.isSynthetic() && !method.isBridge() && !Modifier.isStatic(method.getModifiers())
				&& Modifier.isPublic(method.getModifiers());
	}
}
