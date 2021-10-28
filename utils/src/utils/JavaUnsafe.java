package utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class JavaUnsafe {
    static Unsafe unsafe;
    static boolean patchVm;

    static{
        unsafe=getUnsafe();
        try {
            if (Class.class.getMethod("getModule")!=null){
                patchVm = true;
            }else{
                patchVm=false;
            }
        }catch(Exception e){
            patchVm=false;
        }
    }
    private static Unsafe getUnsafe() {
        Unsafe unsafe = null;

        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return unsafe;
    }
    public static Object getFieldValue(Object obj,String fieldName){
        Field f=getField(obj.getClass(),fieldName);
        try {

            Object oldModule = null;
            Object systemModule;

            if (patchVm){
                systemModule = getModule(f.getDeclaringClass());
                oldModule = getModule(JavaUnsafe.class);
                setModule(f.getDeclaringClass(),systemModule);
            }

            Object value = f.get(obj);

            if (patchVm){
                setModule(f.getDeclaringClass(),oldModule);
            }

            return value;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static Object getModule(Class clazz){
        Object module=null;
        try {
            module=Class.class.getMethod("getModule").invoke(clazz,null);
        }catch (Exception e){

        }
        return module;
    }
    public static boolean setModule(Class clazz,Object module){
        try {
            Field moduleField =Class.class.getDeclaredField("module");
            unsafe.putObject(clazz,unsafe.objectFieldOffset(moduleField),module);
            return true;
        }catch (Exception e){

        }
        return false;
    }

    public static boolean setFieldValue(Object obj,String fieldName,Object value){
        Field f=getField(obj.getClass(),fieldName);
        if (f!=null){
            try {
                Object oldModule = null;
                Object systemModule;
                if (patchVm){
                    systemModule = getModule(f.getDeclaringClass());
                    oldModule = getModule(JavaUnsafe.class);
                    setModule(JavaUnsafe.class,systemModule);
                    f.setAccessible(true);
                }
                f.set(obj,value);

                if (patchVm){
                    setModule(JavaUnsafe.class,oldModule);
                }
                return true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }


    public static Field getField(Class cls, String fieldName){

        while (cls!=null){
            try {
                Field field=cls.getDeclaredField(fieldName);
                if (!patchVm){
                    field.setAccessible(true);
                }
                return field;
            }catch (Exception e){
                cls=cls.getSuperclass();
            }
        }
        return null;
    }
}
