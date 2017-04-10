package configuration;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class ExploreGenericType {

	public static void main(String[] args) {
		// exloreClass(ExploreGenericType.class);
		Type type = exploreObject(new Test<String, Integer>() {
		});
		Type type2 = exploreObject(new TestChild<String, Integer>());
		Type type3 = exploreObject(new TestStringInteger<String, Integer>());
		if (type.equals(type2)) {
			System.out.println("Their generic super class is same one.");
		} else {
			System.out.println("Their generic super class is not same one.");
		}

		// System.out.println(ArrayList.class.getGenericSuperclass());
	}

	private static Type exploreObject(Object object) {
		System.out.println("Explore Object : " + object);
		exloreClass(object.getClass());
		return object.getClass().getGenericSuperclass();

	}

	private static void exloreClass(Class klass) {
		System.out.println("Explore class : " + klass.getCanonicalName());
		System.out.println((klass instanceof Type));

		Type genericType = klass.getGenericSuperclass();
		System.out.println(String.format("Class %s generic superclass is %s", klass, genericType));
		System.out.println(String.format("Generic type is a %s", genericType));
		if (genericType instanceof Class) {
			System.out.println("Generic is a Object.");
			return;
		}
		ParameterizedType parameterized = (ParameterizedType) genericType;
		System.out.println(
				String.format("%s parameterized Class is %s", parameterized, parameterized.getActualTypeArguments()));

		System.out.println(
				String.format("%s parameterized OwnerType is %s", parameterized, parameterized.getOwnerType()));

		System.out.println(String.format("%s parameterized RawType is %s", parameterized, parameterized.getRawType()));

		Type[] types = parameterized.getActualTypeArguments();

		for (Type type : types) {
			if (type instanceof TypeVariable) {
				TypeVariable var = (TypeVariable) type;
				System.out.println("Type Name is :" + var.getName());
				System.out.println("Type GenericDeclaration is :" + var.getGenericDeclaration());
			}
		}
	}

	public static class Test<String, Integer> {
		protected Test() {
		}
	}

	public static class TestChild<String, Integer> {
		protected TestChild() {
		}
	}

	public static class TestStringInteger<String, Integer> {
		protected TestStringInteger() {
		}
	}
}
