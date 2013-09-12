package de.gandalfix.superfactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link SuperFactory} can be used to create any kind of object, if it has
 * a registered object creator for it.
 * 
 * In order to use this you have to register an object factory with this
 * {@link SuperFactory} for a given class.
 * 
 * An object factory is a method within a class that is marked with the
 * {@link ObjectFactory} annotation.
 * 
 * Currently there is only one class containing object factories per class of
 * object allowed.
 * 
 * When you want to create a new instance just call the
 * {@link #createInstance(Class, Object...)} method and pass the class you want
 * to instantiate and the arguments along.
 * 
 * If the object you ask to instantiate contains JSR-330 style \@Inject
 * annotations, the {@link SuperFactory} will try to satisfy these dependencies.
 * Right now all \@Inject occurrences will be processed like Scope=prototype, so
 * no singletons will be used for the same. Also right now only
 * {@link ObjectFactory} without arguments are used.
 * 
 * Example <code>
 * <pre>
 * 
 *    SuperFactory.registerObjectCreator(Integer.class, DummyCreator.class);
 *    SuperFactory.registerObjectCreator(MyObject.class, DummyCreator.class);
 *    SuperFactory.createInstance(MyObject.class, 8, 7.5, "Hallo");
 * 
 *    private static class ObjectCreator {
 *      \@ObjectFactory
 *      protected static Integer create(Integer value, Double v2) {
 *          return new Integer(value);
 *      }
 * 
 *      \@ObjectFactory
 *      protected static MyObject create(Integer value, Double v2, String s) {
 *          MyObject o = new MyObject();
 *          o.value = value;
 *          o.d = v2;
 *         o.s = s;
 *          return o;
 *     }
 *  }
 *  
 *  private static class MyObject {
 *      public Integer value;
 *      public Double d;
 *      public String s;
 *  }
 *  </pre>
 * </code>
 */
public final class SuperFactory {

	private SuperFactory() {
		// This class should not be instantiated
	}

	/**
	 * Map from class to instantiate to factory class.
	 */
	private static Map<Class<?>, Class<?>>	knownCreators	= new HashMap<Class<?>, Class<?>>();

	/**
	 * This method registers for the given class to instantiate a class which
	 * contains the object factory methods (creator).
	 * 
	 * The creator class has to have at least one static method that returns an
	 * instance of the classToInstantiate and is marked with the
	 * {@link ObjectFactory} annotation. Such a factory method must not return
	 * <code>null</code>!
	 * 
	 * @param clazzToInstantiate
	 *            Class that may be instantiated later on. Must not be
	 *            <code>null</code>
	 * @param creator
	 *            class that contains the creator methods for the class to
	 *            instantiated. Must not be <code>null</code>.
	 */
	public static void registerObjectCreator(final Class<?> clazzToInstantiate, final Class<?> creator) {
		if (clazzToInstantiate == null) {
			throw new IllegalArgumentException("clazzToInstantiate must not be null");
		}
		if (creator == null) {
			throw new IllegalArgumentException("creator must not be null");
		}

		if (hasAnnotatedMethod(clazzToInstantiate, creator)) {
			knownCreators.put(clazzToInstantiate, creator);
		} else {
			throw new IllegalArgumentException(creator.getName() + " has not annotated method that creates an instance of "
			        + clazzToInstantiate.getName());
		}
	}

	/**
	 * Creates an instance of the given class using the parameters.
	 * 
	 * Tries to find in the registered known creators the class for the
	 * clazzToInstantiate. After that a method that matches the parameters is
	 * looked for.
	 * 
	 * @param <T>
	 * @param clazzToInstantiate
	 *            type that should be instantiated. Must not be
	 *            <code>null</code>.
	 * @param args
	 *            arguments for the ObjectFactory method. Must not be
	 *            <code>null</code>.
	 * @return instance of the given class.
	 */
	public static <T> T createInstance(final Class<T> clazzToInstantiate, final Object... args) {
		if (clazzToInstantiate == null) {
			throw new IllegalArgumentException("The clazzToInstantiate must not be null");
		}
		if (args == null) {
			throw new IllegalArgumentException("The args must not be null");
		}

		T instance = createActualInstance(clazzToInstantiate, args);

		inject(instance);
		return instance;
	}

	@SuppressWarnings("unchecked")
	private static <T> T createActualInstance(final Class<T> clazzToInstantiate, final Object... args) {
		T instance = null;
		if (knownCreators.containsKey(clazzToInstantiate)) {
			Class<?> creator = knownCreators.get(clazzToInstantiate);
			Method methodToCall = findSuiteableMethod(clazzToInstantiate, creator, args);
			if (methodToCall != null) {
				try {
					instance = (T) methodToCall.invoke(null, args);
				} catch (Exception e) {
					throw new RuntimeException("Unable to instantiate the class " + clazzToInstantiate.getName(), e);
				}
				// A factory method is not allowed to return null, therefore
				// this is an error
				if (instance == null) {
					throw new RuntimeException("Factory method '" + methodToCall.getName() + "' for '" + clazzToInstantiate.getName()
					        + "' has returned null");
				}
			} else {
				throw new IllegalArgumentException("Unable to find factory method for class " + clazzToInstantiate.getName());
			}
		} else {
			throw new IllegalArgumentException("Unknown class to instantiate " + clazzToInstantiate.getName());
		}
		return instance;
	}

	/**
	 * This method looks in the creator for a suiteable method to create a new
	 * instance.
	 * 
	 * @return <code>null</code> if there is no such method. Method instance
	 *         otherwise.
	 */
	static Method findSuiteableMethod(final Class<?> classToInstantiate, final Class<?> creator, final Object... args) {
		Method suitableMethod = null;

		Method[] methods = creator.getDeclaredMethods();

		for (Method method : methods) {
			if (method.isAnnotationPresent(ObjectFactory.class) && classToInstantiate.isAssignableFrom(method.getReturnType())) {
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parametersMatch(args, parameterTypes)) {
					suitableMethod = method;
					break;
				}
			}
		}

		return suitableMethod;
	}

	/**
	 * Checks whether the arguments match the given parameter types.
	 */
	static boolean parametersMatch(final Object[] args, final Class<?>[] parameterTypes) {
		boolean result = true;
		if (args.length == parameterTypes.length) {
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				if (arg != null) {
					if (!parameterTypes[i].isAssignableFrom(arg.getClass())) {
						result = false;
						break;
					}
				} else {
					// ignore null args
				}
			}
		} else {
			result = false;
		}
		return result;
	}

	/**
	 * Checks whether the given creator class has at least one method that
	 * allows the creation of the classToInstantiate.
	 */
	static boolean hasAnnotatedMethod(final Class<?> classToInstantiate, final Class<?> creator) {
		boolean hasAnno = false;

		Method[] methods = creator.getDeclaredMethods();
		for (Method method : methods) {
			if (Modifier.isStatic(method.getModifiers()) && method.isAnnotationPresent(ObjectFactory.class)
			        && classToInstantiate.isAssignableFrom(method.getReturnType())) {
				hasAnno = true;
				break;
			}
		}

		return hasAnno;
	}

	/**
	 * This method is for supporting JSR-330 annotations like
	 * {@code java.inject.Inject}. If the classes are not available at runtime
	 * the method does nothing.
	 * 
	 * Otherwise the method will try to set the values that should be injected
	 * into objectToFill.
	 * 
	 * Right now this method currently supports only setting of declared fields
	 * within the object.
	 * 
	 * @param objectToFill
	 *            object that should be processed and the injected properties of
	 *            which should be filled. Must not be <code>null</code>.
	 */
	@SuppressWarnings("unchecked")
	private static <T> void inject(final T objectToFill) {
		Class<? extends Annotation> jsr330Inject;
		try {
			// Check for availability java @javax.inject.Inject.
			// If it is not available in the classpath just bail out, and do
			// nothing.
			jsr330Inject = (Class<? extends Annotation>) Class.forName("javax.inject.Inject");
		} catch (ClassNotFoundException e1) {
			return;
		}
		Class<T> clazzOfObject = (Class<T>) objectToFill.getClass();
		Field[] allFields = clazzOfObject.getDeclaredFields();
		for (Field field : allFields) {
			if (field.isAnnotationPresent(jsr330Inject)) {
				try {
					field.set(objectToFill, createInstance(field.getType()));
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Unable to Inject into '" + field.getName() + "' of '" + clazzOfObject.getName() + "'", e);
				}
			}
		}
	}
}
