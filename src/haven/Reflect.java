package haven;

import java.lang.reflect.Field;
import java.util.Arrays;

public class Reflect {
    public static Object getFieldValue(Object obj, String name) {
        Object v = null;
        try {
            Field f = getField(obj, name);
            v = f.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
        }
        return v;
    }

    public static int getFieldValueInt(Object obj, String name) {
        int v = 0;
        try {
            Field f = getField(obj, name);
            v = f.getInt(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
        }
        return v;
    }

    public static double getFieldValueDouble(Object obj, String name) {
        double v = 0;
        try {
            Field f = getField(obj, name);
            v = f.getDouble(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
        }
        return v;
    }

    public static String getFieldValueString(Object obj, String name) {
        return (String) getFieldValue(obj, name);
    }

    public static boolean getFieldValueBool(Object obj, String name) {
        return Boolean.TRUE.equals(getFieldValue(obj, name));
    }

    private static Field getField(Object obj, String name) throws NoSuchFieldException {
        Class cls = obj.getClass();
        while (true) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
                if (cls == null) {
                    throw e;
                }
            }

        }
    }

    public static boolean hasField(Object obj, String name) {
        try {
            getField(obj, name);
            return true;
        } catch (NoSuchFieldException e) {
            CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    public static Class getEnumSuperclass(Class c) {
        while (c != null && !c.isEnum()) {
            c = c.getSuperclass();
        }
        return c;
    }

    public static Class[] interfaces(Class c) {
        try {
            return c.getInterfaces();
        } catch (Exception e) {
            CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
        }
        return new Class[0];
    }

    public static boolean hasInterface(String name, Class c) {
        Class[] interfaces = interfaces(c);
        for (Class in : interfaces) {
            if (in.getCanonicalName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean is(Object obj, String clazz) {
        return obj != null && obj.getClass().getName().equals(clazz);
    }

    public static Object invoke(Object o, String method, Object... args) {
        Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            types[i] = arg.getClass();
        }
        try {
            return o.getClass().getDeclaredMethod(method, types).invoke(o, args);
        } catch (Exception e) {
            CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
        }
        return null;
    }
}
