package de.gandalfix.superfactory;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class SuperFactoryTest {

    @Test
    public void testHasAnnotatedMethod() {
        assertFalse(SuperFactory.hasAnnotatedMethod(Integer.class, Object.class));
        assertTrue(SuperFactory.hasAnnotatedMethod(Integer.class, DummyCreator.class));
        assertTrue(SuperFactory.hasAnnotatedMethod(Number.class, DummyCreator.class));
        assertFalse(SuperFactory.hasAnnotatedMethod(Double.class, DummyCreator.class));
    }

    @Test
    public void testParametersMatch() {
        assertTrue(SuperFactory.parametersMatch(new Object[] {7L, "Hallo"}, new Class<?>[] {Long.class, String.class}));
        assertTrue(SuperFactory.parametersMatch(new Object[] {7L, "Hallo"}, new Class<?>[] {Number.class, String.class}));
        assertTrue(SuperFactory.parametersMatch(new Object[] {null, "Hallo"}, new Class<?>[] {Number.class, String.class}));
        assertFalse(SuperFactory.parametersMatch(new Object[] {7L, "Hallo"}, new Class<?>[] {Number.class, String.class, Long.class}));
        assertFalse(SuperFactory.parametersMatch(new Object[] {7L, "Hallo"}, new Class<?>[] {Double.class, String.class}));
    }
    
    @Test
    public void testWholeStuff() {
        SuperFactory.registerObjectCreator(Integer.class, DummyCreator.class);
        SuperFactory.registerObjectCreator(MyObject.class, DummyCreator.class);
        assertEquals(SuperFactory.createInstance(Integer.class, 7, Double.valueOf(4d)), Integer.valueOf(7));
        assertEquals(SuperFactory.createInstance(Integer.class), Integer.valueOf(5));
        MyObject o = SuperFactory.createInstance(MyObject.class, 8, 7.5, "Hallo");
        assertNotNull(o);
        assertEquals(o.d, 7.5);
        assertEquals(o.s, "Hallo");
        assertEquals(o.value, Integer.valueOf(8));
    }
    
    
    private static class DummyCreator {
        @ObjectFactory
        protected static Integer create(Integer value, Double v2) {
            return new Integer(value);
        }

        @ObjectFactory
        protected static Integer create() {
            return new Integer(5);
        }

        @ObjectFactory
        protected static MyObject create(Integer value, Double v2, String s) {
            MyObject o = new MyObject();
            o.value = value;
            o.d = v2;
            o.s = s;
            return o;
        }
    }
    
    private static class MyObject {
        public Integer value;
        public Double d;
        public String s;
    }
}
