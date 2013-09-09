package de.gandalfix.superfactory;

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
    private static Map<Class<?>, Class<?>> knownCreators = new HashMap<Class<?>, Class<?>>();

    /**
     * This method registers for the given class to instantiate a class which
     * contains the object factory methods (creator).
     * 
     * The creator class has to have at least one static method that returns an
     * instance of the classToInstantiate and is marked with the
     * {@link ObjectFactory} annotation.
     * 
     * @param clazzToInstantiate
     *            Class that may be instantiated later on. Must not be
     *            <code>null</code>
     * @param creator
     *            class that contains the creator methods for the class to
     *            instantiated. Must not be <code>null</code>.
     */
    public static void registerObjectCreator(Class<?> clazzToInstantiate, Class<?> creator) {
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
    public static <T> T createInstance(Class<T> clazzToInstantiate, Object... args) {
        if (clazzToInstantiate == null) {
            throw new IllegalArgumentException("The clazzToInstantiate must not be null");
        }
        if (args == null) {
            throw new IllegalArgumentException("The args must not be null");
        }

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
    static Method findSuiteableMethod(Class<?> classToInstantiate, Class<?> creator, Object... args) {
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
    static boolean parametersMatch(Object[] args, Class<?>[] parameterTypes) {
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
    static boolean hasAnnotatedMethod(Class<?> classToInstantiate, Class<?> creator) {
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
}
