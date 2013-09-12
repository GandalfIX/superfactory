package de.gandalfix.superfactory;

import static org.fest.assertions.Assertions.*;

import javax.inject.Inject;

import org.junit.Test;

public class SuperFactoryTest {

	@Test
	public void testHasAnnotatedMethod() {
		assertThat(SuperFactory.hasAnnotatedMethod(Integer.class, Object.class)).isFalse();
		assertThat(SuperFactory.hasAnnotatedMethod(Integer.class, DummyCreator.class)).isTrue();
		assertThat(SuperFactory.hasAnnotatedMethod(Number.class, DummyCreator.class)).isTrue();
		assertThat(SuperFactory.hasAnnotatedMethod(Double.class, DummyCreator.class)).isFalse();
	}

	@Test
	public void testParametersMatch() {
		assertThat(SuperFactory.parametersMatch(new Object[] { 7L, "Hallo" }, new Class<?>[] { Long.class, String.class })).isTrue();
		assertThat(SuperFactory.parametersMatch(new Object[] { 7L, "Hallo" }, new Class<?>[] { Number.class, String.class })).isTrue();
		assertThat(SuperFactory.parametersMatch(new Object[] { null, "Hallo" }, new Class<?>[] { Number.class, String.class })).isTrue();
		assertThat(SuperFactory.parametersMatch(new Object[] { 7L, "Hallo" }, new Class<?>[] { Number.class, String.class, Long.class }))
		        .isFalse();
		assertThat(SuperFactory.parametersMatch(new Object[] { 7L, "Hallo" }, new Class<?>[] { Double.class, String.class })).isFalse();
	}

	@Test
	public void testWholeStuff() {
		SuperFactory.registerObjectCreator(Integer.class, DummyCreator.class);
		SuperFactory.registerObjectCreator(MyObject.class, DummyCreator.class);
		assertThat(SuperFactory.createInstance(Integer.class, 7, Double.valueOf(4d))).isEqualTo(Integer.valueOf(7));
		assertThat(SuperFactory.createInstance(Integer.class)).isEqualTo(Integer.valueOf(5));
		MyObject o = SuperFactory.createInstance(MyObject.class, 8, 7.5, "Hallo");
		assertThat(o).isNotNull();
		assertThat(o.d).isEqualTo(7.5);
		assertThat(o.s).isEqualTo("Hallo");
		assertThat(o.value).isEqualTo(Integer.valueOf(8));
	}

	@Test
	public void testJSR330Annotation() {
		SuperFactory.registerObjectCreator(Integer.class, DummyCreator.class);
		SuperFactory.registerObjectCreator(MyObject.class, DummyCreator.class);
		SuperFactory.registerObjectCreator(MyObject2.class, DummyCreator.class);

		MyObject2 o2 = SuperFactory.createInstance(MyObject2.class);
		assertThat(o2).isNotNull();
		assertThat(o2.object).isNotNull();
		assertThat(o2.object.getClass()).isEqualTo(MyObject.class);
	}

	private static class DummyCreator {
		@ObjectFactory
		protected static Integer create(final Integer value, final Double v2) {
			return new Integer(value);
		}

		@ObjectFactory
		protected static Integer create() {
			return new Integer(5);
		}

		@ObjectFactory
		protected static MyObject create(final Integer value, final Double v2, final String s) {
			MyObject o = new MyObject();
			o.value = value;
			o.d = v2;
			o.s = s;
			return o;
		}

		@ObjectFactory
		protected static MyObject createMyObject() {
			return new MyObject();
		}

		@ObjectFactory
		protected static MyObject2 createMyObject2() {
			return new MyObject2();
		}
	}

	private static class MyObject {
		public Integer	value;
		public Double	d;
		public String	s;
	}

	private static class MyObject2 {
		@Inject
		public MyObject	object;
	}
}
