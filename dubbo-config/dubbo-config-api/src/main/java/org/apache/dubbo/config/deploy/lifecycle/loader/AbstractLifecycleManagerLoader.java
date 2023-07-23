package org.apache.dubbo.config.deploy.lifecycle.loader;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.deploy.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractLifecycleManagerLoader<T extends Lifecycle> {

    private final Map<String, T> managers;

    /**
     *  Map&lt;LifecycleManageOperationName,Function&lt;Lifecycle,DependencyList&gt;&gt;
     */
    private final Map<String, Function<T,List<String>>> dependencyProviders;

    /**
     * Map&lt;LifecycleManageOperationName,LifecycleManagerExecuteSequence&gt;
     */
    private final Map<String, List<T>> sequences;

    public AbstractLifecycleManagerLoader() {
        this.managers = loadManagers().stream().collect(Collectors.toMap(Lifecycle::name, manager -> manager));
        this.dependencyProviders = new HashMap<>();
        this.sequences = new HashMap<>();
        loadSequence();
        mapOperationsToDependencyProvider(this.dependencyProviders);
    }

    /**
     * Map method name to the method that provides dependency relations.
     */
    protected abstract void mapOperationsToDependencyProvider(Map<String,Function<T,List<String>>> dependencyProviders);

    /**
     * Load all lifecycle managers.
     * @return lifecycle managers.
     */
    protected abstract List<T> loadManagers();

    /**
     * Map method sequences.
     */
    protected void loadSequence(){

        managers.forEach((name, manager) ->

            dependencyProviders.forEach((operation, dependencyProvider) -> {

            List<T> lifeManagers;
            lifeManagers = getOrderedLifecycleByDependency(dependencyProvider, manager, new HashMap<>(), new ArrayList<>());

            sequences.put(operation,lifeManagers);
        }));
    }

    protected List<T> getOrderedLifecycleByDependency(Function<T, List<String>> dependencyProvider, T currentManager, Map<String, T> processedManagers, List<T> orderedManagers) {

        List<String> depends = dependencyProvider.apply(currentManager);

        Assert.assertTrue(processedManagers.get(currentManager.name()) == null, "There are cyclic dependencies between PackageLifeManagers.");

        if (depends != null && !depends.isEmpty()) {
            //Prevent cyclic dependencies
            processedManagers.putIfAbsent(currentManager.name(), currentManager);

            for (String depend : depends) {

                T manager = managers.get(depend);

                Assert.assertTrue(manager != null, "One of required Lifecycle not found:" + depend);
                Assert.assertTrue(manager.needInitialize(), "A required Lifecycle is not started:" + manager.name());
                //dfs
                getOrderedLifecycleByDependency(dependencyProvider, manager, processedManagers, orderedManagers);
            }
        }
        //No more dependencies
        orderedManagers.add(currentManager);
        processedManagers.remove(currentManager.name());

        return orderedManagers;
    }

    public Lifecycle get(String name) {
        return managers.get(name);
    }

    public List<T> getSequenceByOperationName(String operation){
        return sequences.get(operation);
    }

}
