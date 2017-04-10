package configuration;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

public class TestTypeToken {

	static <K, V> TypeToken<Map<K, V>> mapToken(TypeToken<K> keyToken, TypeToken<V> valueToken) {
		return new TypeToken<Map<K, V>>() {
		}.where(new TypeParameter<K>() {
		}, keyToken).where(new TypeParameter<V>() {
		}, valueToken);
	}

	private static <E> TypeToken<Set<E>> setOf(Class<E> elementType) {
		return new TypeToken<Set<E>>() {
		}.where(new TypeParameter<E>() {
		}, elementType);
	}

	static <T> TypeToken<List<T>> listOf(TypeToken<T> typeToken) {
		return new TypeToken<List<T>>() {
		}.where(new TypeParameter<T>() {
		}, typeToken);
	}

	@SuppressWarnings("serial")
	public static void main(String args[]) {
		TypeToken<ArrayList<String>> typeToken = new TypeToken<ArrayList<String>>() {
		};
		TypeToken<?> genericTypeToken = typeToken.resolveType(ArrayList.class.getTypeParameters()[0]);
		System.out.println(genericTypeToken.getType());
		System.out.println(typeToken.getType());
		System.out.println(typeToken.getRawType());
		System.out.println(typeToken.getTypes());
		Type temp = typeToken.getType();
		Type temp1 = typeToken.getRawType();
		System.out.println("=========================");
		System.out.println(temp.getTypeName());
		System.out.println(temp1.getTypeName());

		TypeToken<Map<String, BigInteger>> mapToken = mapToken(TypeToken.of(String.class),
				TypeToken.of(BigInteger.class));
		TypeToken<Map<Integer, Queue<String>>> complexToken = mapToken(TypeToken.of(Integer.class),
				new TypeToken<Queue<String>>() {
				});
		TypeToken<Set<String>> setToken = setOf(String.class);
		TypeToken<List<String>> listToken = listOf(TypeToken.of(String.class));

	}
}
