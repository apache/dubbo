package org.apache.dubbo.config.deploy.lifecycle.manager;

import org.apache.dubbo.common.constants.PackageName;
import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Spi method manager.
 */
public class SpiMethodManager {

    /**
     *  All SpiMethod that attachToApplication() returns false will load to DEFAULT instance.
     */
    private static final String DEFAULT = "default";

    /**
     * applicationName -> methodManager
     */
    private static final Map<String, SpiMethodManager> INSTANCES;

    static {
        INSTANCES = new HashMap<>();
        INSTANCES.put(DEFAULT, new SpiMethodManager(ApplicationModel.defaultModel(),true));
    }

    /**
     * packageName -> methodName -> method
     */
    private final Map<PackageName, Map<SpiMethodNames, SpiMethod>> methods;

    public SpiMethodManager(ApplicationModel applicationModel) {
        this(applicationModel,false);
    }

    private SpiMethodManager(ApplicationModel applicationModel, boolean isDefaultInstance) {

        List<SpiMethod> activeMethods = applicationModel.getExtensionLoader(SpiMethod.class).getActivateExtensions();

        this.methods = new HashMap<>();

        activeMethods.forEach(spiMethod -> {

            if(spiMethod.attachToApplication() && !isDefaultInstance) {
                this.methods.computeIfAbsent(spiMethod.methodName().getPackageName(), k -> new HashMap<>());

                this.methods.get(spiMethod.methodName().getPackageName()).putIfAbsent(spiMethod.methodName(), spiMethod);
            }
        });
    }

    /**
     * Register a new instance of ApiMethodManager by application
     */
    public static void registerNewInstance(ApplicationModel applicationModel) {
        INSTANCES.put(applicationModel.getApplicationName(), new SpiMethodManager(applicationModel));
    }

    /**
     * Get default ApiMethodManager.
     */
    public static Invoker get() {
        return INSTANCES.get(DEFAULT).new Invoker();
    }

    /**
     * Get spi method manager by application name
     * @param appName application name
     */
    public Invoker getByApplication(String appName) {
        SpiMethodManager manager = INSTANCES.get(appName);
        Assert.assertTrue(manager != null, "Application not found , application name:" + appName);
        return manager.new Invoker();
    }

    public class Invoker {

        static final byte OP_DEFAULT = 0;

        static final byte OP_IF_PRESENT = 1;

        static final byte OP_ASSERT_PRESENT = 2;

        byte operation = OP_DEFAULT;

        Supplier<?> supplier;

        /**
         * If target method/package not found, just returns null.
         */
        public Invoker ifPresent() {
            operation = OP_IF_PRESENT;
            return this;
        }

        /**
         * If target method/package not found, throw an exception.
         */
        public Invoker assertPresent() {
            operation = OP_ASSERT_PRESENT;
            return this;
        }

        /**
         * If target method/package not found, return another value.
         * @param supplier supplier
         */
        public Invoker elseReturn(Supplier<?> supplier){
            this.supplier = supplier;
            return this;
        }

        /**
         * Whether any SpiMethod instance has registered by certain package name in this application.
         *
         * @param packageName package name
         */
        public boolean packagePresent(PackageName packageName){
            return methods.containsKey(packageName);
        }

        /**
         * Whether an SpiMethod instance has registered by certain method name in this application.
         *
         * @param methodName method name
         *
         */
        public boolean methodPresent(SpiMethodNames methodName){
            return methods.containsKey(methodName.getPackageName()) && methods.get(methodName.getPackageName()).containsKey(methodName);
        }

        public Object invoke(SpiMethodNames methodName, Object... params) {

            SpiMethod method = null;

            PackageName packageName = methodName.getPackageName();

            switch (operation) {
                case OP_DEFAULT:
                    method = methods.get(packageName).get(methodName);
                    break;

                case OP_IF_PRESENT:
                    if (methods.containsKey(packageName)) {
                        method = methods.get(packageName).get(methodName);
                    } else {
                        if(supplier != null){
                            return supplier.get();
                        }
                        return null;
                    }
                    break;

                case OP_ASSERT_PRESENT:
                    if (methods.containsKey(packageName)) {
                        method = methods.get(packageName).get(methodName);
                    } else {
                        throw new RuntimeException("Required package " + packageName + " not found");
                    }
            }

            Assert.assertTrue(method != null, "Target Spi method not exist:" + methodName);

            return method.invoke(params);
        }
    }

}
