package org.apache.dubbo.config;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Package Life Manager Loader
 */
public class PackageLifeManagerLoader {

    private static final String INIT = "init";

    private static final String PRE_DESTROY = "preDestroy";

    private static final String POST_DESTROY = "postDestroy";

    private static final String MODULE_CHANGED = "moduleChanged";

    private final DefaultApplicationDeployer defaultApplicationDeployer;

    private List<PackageLifeCycleManager> initSequence;

    private List<PackageLifeCycleManager> preDestroySequence;

    private List<PackageLifeCycleManager> postDestroySequence;

    private List<PackageLifeCycleManager> moduleChangeSequence;

    private Map<String, PackageLifeCycleManager> managers;

    public PackageLifeManagerLoader(DefaultApplicationDeployer defaultApplicationDeployer) {
        this.defaultApplicationDeployer = defaultApplicationDeployer;
        load();
        loadSequence();
    }

    public void packageRunInit() {
        this.initSequence.forEach(PackageLifeCycleManager::initialize);
    }

    public void packageRunPreDestroy() {
        this.preDestroySequence.forEach(PackageLifeCycleManager::preDestroy);
    }

    public void packageRunPostDestroy() {
        this.postDestroySequence.forEach(PackageLifeCycleManager::postDestroy);
    }

    public void packageRunModuleChanged(ModuleModel changedModule, DeployState deployState) {
        this.moduleChangeSequence.forEach(packageLifeManager -> packageLifeManager.moduleChanged(changedModule, deployState));
    }

    private void load() {
        ExtensionLoader<PackageLifeCycleManager> loader = defaultApplicationDeployer.getExtensionLoader(PackageLifeCycleManager.class);
        this.managers = loader.getActivateExtensions().stream().collect(
            Collectors.toMap(PackageLifeCycleManager::name, packageLifeManager -> packageLifeManager)
        );
    }

    private void loadSequence() {

        this.initSequence = new ArrayList<>();
        this.preDestroySequence = new ArrayList<>();
        this.postDestroySequence = new ArrayList<>();
        this.moduleChangeSequence = new ArrayList<>();

        for (Map.Entry<String, PackageLifeCycleManager> manager : this.managers.entrySet()) {

            if (!initSequence.contains(manager.getValue())) {
                initSequence.addAll(startFind(INIT, manager.getValue()));
            }
            if (!preDestroySequence.contains(manager.getValue())) {
                preDestroySequence.addAll(startFind(PRE_DESTROY, manager.getValue()));
            }
            if (!postDestroySequence.contains(manager.getValue())) {
                postDestroySequence.addAll(startFind(POST_DESTROY, manager.getValue()));
            }
            if (!moduleChangeSequence.contains(manager.getValue())) {
                moduleChangeSequence.addAll(startFind(MODULE_CHANGED, manager.getValue()));
            }
        }
    }

    private List<PackageLifeCycleManager> startFind(String type, PackageLifeCycleManager manager) {
        return findDependChain(type, manager, new HashMap<>(), new ArrayList<>());
    }

    private List<PackageLifeCycleManager> findDependChain(String type, PackageLifeCycleManager current, Map<String, PackageLifeCycleManager> loaded, List<PackageLifeCycleManager> result) {

        List<String> depends;

        switch (type) {
            case INIT:
                depends = current.dependOnInit();
                break;
            case PRE_DESTROY:
                depends = current.dependOnPreDestroy();
                break;
            case POST_DESTROY:
                depends = current.dependOnPostDestroy();
                break;
            case MODULE_CHANGED:
                depends = current.dependOnModuleChanged();
                break;
            default:
                throw new IllegalArgumentException("Unknown type:" + type);
        }
        Assert.assertTrue(loaded.get(current.name()) == null, "There are cyclic dependencies between PackageLifeManagers.");

        if (depends != null && !depends.isEmpty()) {
            //Prevent cyclic dependencies
            loaded.putIfAbsent(current.name(), current);

            for (String depend : depends) {

                PackageLifeCycleManager manager = managers.get(depend);

                Assert.assertTrue(manager != null, "One of required package life manager not found:" + depend);
                Assert.assertTrue(manager.needInitialize(),"A required PackageLifeCycleManager is not started:"+manager.name());
                //dfs
                findDependChain(type, manager, loaded, result);
            }
        }
        //No more dependencies
        result.add(current);
        loaded.remove(current.name());

        return result;
    }
}
