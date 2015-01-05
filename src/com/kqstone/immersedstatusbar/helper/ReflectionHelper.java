package com.kqstone.immersedstatusbar.helper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ReflectionHelper {
	private static HashMap<String, Method> sMethodCache = new HashMap<String, Method>();
	private static HashMap<String, Field> sFieldCache = new HashMap<String, Field>();

	private static HashMap<Class<?>, Class<?>> primativeClassMap = new HashMap<Class<?>, Class<?>>();
	static {
		primativeClassMap.put(int.class, Integer.class);
		primativeClassMap.put(boolean.class, Boolean.class);
		primativeClassMap.put(float.class, Float.class);
		primativeClassMap.put(long.class, Long.class);
		primativeClassMap.put(short.class, Short.class);
		primativeClassMap.put(byte.class, Byte.class);
		primativeClassMap.put(double.class, Double.class);
		primativeClassMap.put(char.class, Character.class);
		primativeClassMap.put(void.class, Void.class);
	}

	public static Class<?> getClass(String className) {
		Class<?> clazz = null;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return clazz;
	}

	public static Class<?> getClass(String className, boolean shouldInitialize,
			ClassLoader classLoader) {
		Class<?> clazz = null;
		try {
			clazz = Class.forName(className, shouldInitialize, classLoader);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return clazz;
	}

	public static Object callStaticMethod(Class<?> clazz, String methodName,
			Object... args) {
		Object result = null;
		try {
			Method method = getMethodBestMatch(clazz, methodName,
					getArgsTypes(args));
			result = method.invoke(clazz, args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return result;
	}

	public static Object callMethod(Object object, String methodName,
			Object... args) {
		Object result = null;
		Class<?> clazz = object.getClass();
		result = callMethod(clazz, object, methodName, args);
		return result;
	}

	public static Object callMethod(Class<?> clazz, Object object,
			String methodName, Object... args) {
		Object result = null;
		Class<?>[] argsClasses = getArgsTypes(args);
		try {
			Method method = getMethodBestMatch(clazz, methodName, argsClasses);
			result = method.invoke(object, args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return result;
	}

	public static Object getStaticField(Class<?> clazz, String fieldName) {
		Object result = null;
		try {
			Field field = getField(clazz, fieldName);
			result = field.get(clazz);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return result;
	}

	public static Object getObjectField(Object object, String fieldName) {
		Object result = null;
		try {
			Field field = getField(object.getClass(), fieldName);
			result = field.get(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void setStaticField(Class<?> clazz, String fieldName,
			Object value) {
		try {
			Field field = getField(clazz, fieldName);
			field.set(clazz, value);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setObjectField(Object object, String fieldName,
			Object value) {
		try {
			Field field = getField(object.getClass(), fieldName);
			field.set(object, value);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static String genMethodFullName(Class<?> clazz, String methodName,
			Class<?>... argClasses) {
		StringBuilder name = new StringBuilder();
		name.append(clazz.getName());
		name.append(":");
		name.append(methodName);
		for (int i = 0; i < argClasses.length; i++) {
			if (i == (argClasses.length - 1)) {
				name.append(argClasses[i].getName());
			} else {
				name.append(argClasses[i].getName());
				name.append(",");
			}
		}
		return name.toString();
	}

	private static Class<?>[] getArgsTypes(Object... args) {
		Class<?>[] argClasses = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			argClasses[i] = (args[i] != null) ? args[i].getClass() : null;
			i++;
		}
		return argClasses;
	}

	private static Method getMethodBestMatch(Class<?> clazz, String methodName,
			Class<?>... argClasses) throws Throwable {
		String methodFullName = genMethodFullName(clazz, methodName, argClasses);
		if (sMethodCache.containsKey(methodFullName))
			return sMethodCache.get(methodFullName);
		for (Method method : clazz.getDeclaredMethods()) {
			if (method.getName().equals(methodName)) {
				Class<?>[] tmpArgs = method.getParameterTypes();
				if (compareArgs(tmpArgs, argClasses)) {
					method.setAccessible(true);
					sMethodCache.put(methodFullName, method);
					return method;
				}
			}
		}
		String msg = "";
		for (Class<?> cls : argClasses) {
			msg += cls.getSimpleName() + ",";
		}
		msg = "Can't get method: " + clazz.getSimpleName() + "." + methodName
				+ "(" + msg + ")";
		throw new Exception(msg);
	}

	private static boolean isChildClass(Class<?> origClass, Class<?> dstClass) {
		if (dstClass == null)
			return true;
		if (origClass.isInterface()) {
			for (Class<?> i : dstClass.getInterfaces()) {
				if (origClass == i)
					return true;
			}
		}
		if (origClass.isPrimitive()
				&& (primativeClassMap.get(origClass) == dstClass))
			return true;
		for (; dstClass != Object.class; dstClass = dstClass.getSuperclass()) {
			if (dstClass == origClass)
				return true;
		}
		return false;
	}

	private static boolean compareArgs(Class<?>[] dstArgs, Class<?>[] origArgs) {
		if (dstArgs.length != origArgs.length)
			return false;
		for (int i = 0; i < dstArgs.length; i++) {
			if (!isChildClass(dstArgs[i], origArgs[i]))
				return false;
		}
		return true;
	}

	private static Field getField(Class<?> clazz, String fieldName)
			throws Throwable {
		String fieldFullName = genFieldFullName(clazz, fieldName);
		if (sFieldCache.containsKey(fieldFullName))
			return sFieldCache.get(fieldFullName);
		Field field = null;
		try {
			field = clazz.getField(fieldName);
		} catch (NoSuchFieldException e) {
		}
		if (field == null) {
			try {
				field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);
			} catch (NoSuchFieldException e) {
			}
		}
		if (field == null) {
			for (clazz = clazz.getSuperclass(); clazz != Object.class; clazz = clazz
					.getSuperclass()) {
				try {
					field = clazz.getDeclaredField(fieldName);
					field.setAccessible(true);
				} catch (NoSuchFieldException e) {
				}
			}
		}
		if (field == null) {
			String msg = "";
			msg = "Can't get Field from Class " + clazz.getSimpleName() + ":"
					+ fieldName;
			throw new Throwable(msg);
		}
		sFieldCache.put(fieldFullName, field);
		return field;
	}

	private static String genFieldFullName(Class<?> clazz, String fieldName) {
		StringBuilder name = new StringBuilder();
		name.append(clazz.getName());
		name.append(":");
		name.append(fieldName);
		return name.toString();
	}

}
