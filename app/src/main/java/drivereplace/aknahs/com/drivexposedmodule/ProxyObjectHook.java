package drivereplace.aknahs.com.drivexposedmodule;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by aknahs on 14/12/14.
 */
public class ProxyObjectHook extends XC_MethodReplacement {

    public Object proxyObject;

    public ProxyObjectHook(Object o) {
        proxyObject = o;
    }

    @Override
    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

        Method m = XposedHelpers.findMethodExact(proxyObject.getClass(),methodHookParam.method.getName(),((Method)methodHookParam.method).getParameterTypes());

        m.invoke(proxyObject,methodHookParam.args);

        return null;
    }
}
