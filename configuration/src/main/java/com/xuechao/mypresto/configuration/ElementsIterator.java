package com.xuechao.mypresto.configuration;

import java.util.Iterator;
import java.util.List;

import org.testng.collections.Lists;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;

public class ElementsIterator implements Module, Iterable<Element> {
	private final List<Element> boundElements;
	private final List<Element> elements;

	public ElementsIterator(Module... modules) {
		this(ImmutableList.copyOf(modules));
	}

	public ElementsIterator(Iterable<? extends Module> modules) {
		this.elements = Elements.getElements(modules);
		this.boundElements = Lists.newArrayList(elements);
	}

	@Override
	public void configure(Binder binder) {
		Module baseModule = Elements.getModule(boundElements);
		binder.install(baseModule);
	}
	public void unbindElement(Element element){
		if (element == null) {
            throw new IllegalStateException();
        }
		boundElements.remove(element);
	}

	@Override
	public Iterator<Element> iterator() {
		return elements.iterator();
	}

}
