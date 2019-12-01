package rios.gaston.multicastRMI;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.StubNotFoundException;
import java.rmi.server.*;
import java.security.*;
import java.util.ArrayList;

public class MulticastUtil {

    public static Remote createProxy(Class<?> implClass, RemoteRef clientRef) throws StubNotFoundException {
        final ClassLoader loader = implClass.getClassLoader();
        final Class<?>[] interfaces = getRemoteInterfaces(implClass);
        final RemoteObjectInvocationHandler handler = new RemoteObjectInvocationHandler(clientRef);

        try {
            return (Remote) AccessController.doPrivileged((PrivilegedAction<Remote>) () -> (Remote) Proxy.newProxyInstance(loader, interfaces, handler));
        } catch (IllegalArgumentException var8) {
            throw new StubNotFoundException("unable to create proxy", var8);
        }
    }

    private static Class<?>[] getRemoteInterfaces(Class<?> remoteClass) {
        ArrayList<Class<?>> list = new ArrayList();
        getRemoteInterfaces(list, remoteClass);
        return (Class[])list.toArray(new Class[list.size()]);
    }

    private static void getRemoteInterfaces(ArrayList<Class<?>> list, Class<?> cl) {
        Class<?> superclass = cl.getSuperclass();
        if (superclass != null) {
            getRemoteInterfaces(list, superclass);
        }

        Class<?>[] interfaces = cl.getInterfaces();

        for(int i = 0; i < interfaces.length; ++i) {
            Class<?> intf = interfaces[i];
            if (Remote.class.isAssignableFrom(intf) && !list.contains(intf)) {
                Method[] methods = intf.getMethods();

                for(int j = 0; j < methods.length; ++j) {
                    checkMethod(methods[j]);
                }

                list.add(intf);
            }
        }

    }

    private static void checkMethod(Method m) {
        Class<?>[] ex = m.getExceptionTypes();

        for(int i = 0; i < ex.length; ++i) {
            if (ex[i].isAssignableFrom(RemoteException.class)) {
                return;
            }
        }

        throw new IllegalArgumentException("illegal remote method encountered: " + m);
    }

    public static long computeMethodHash(Method m) {
        long hash = 0L;
        ByteArrayOutputStream sink = new ByteArrayOutputStream(127);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            DataOutputStream out = new DataOutputStream(new DigestOutputStream(sink, md));
            String s = getMethodNameAndDescriptor(m);

            out.writeUTF(s);
            out.flush();
            byte[] hasharray = md.digest();

            for(int i = 0; i < Math.min(8, hasharray.length); ++i) {
                hash += (long)(hasharray[i] & 255) << i * 8;
            }
        } catch (IOException var9) {
            hash = -1L;
        } catch (NoSuchAlgorithmException var10) {
            throw new SecurityException(var10.getMessage());
        }

        return hash;
    }

    private static String getMethodNameAndDescriptor(Method m) {
        StringBuilder desc = new StringBuilder(m.getName());
        desc.append('(');
        Class<?>[] paramTypes = m.getParameterTypes();

        for (Class<?> paramType : paramTypes) {
            desc.append(getTypeDescriptor(paramType));
        }

        desc.append(')');
        Class<?> returnType = m.getReturnType();
        if (returnType == Void.TYPE) {
            desc.append('V');
        } else {
            desc.append(getTypeDescriptor(returnType));
        }

        return desc.toString();
    }

    private static String getTypeDescriptor(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == Integer.TYPE) {
                return "I";
            } else if (type == Boolean.TYPE) {
                return "Z";
            } else if (type == Byte.TYPE) {
                return "B";
            } else if (type == Character.TYPE) {
                return "C";
            } else if (type == Short.TYPE) {
                return "S";
            } else if (type == Long.TYPE) {
                return "J";
            } else if (type == Float.TYPE) {
                return "F";
            } else if (type == Double.TYPE) {
                return "D";
            } else if (type == Void.TYPE) {
                return "V";
            } else {
                throw new Error("unrecognized primitive type: " + type);
            }
        } else if (type.isArray()) {
            return type.getName().replace('.', '/');
        } else {
            String var10000 = type.getName();
            return "L" + var10000.replace('.', '/') + ";";
        }
    }
}
