package org.apache.dubbo.registry.pilot;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.remoting.pilot.PilotTransporter;

/**
 * pilot registryfactory
 * @author hzj
 * @date 2019/03/20
 */
public class PilotRegistryFactory extends AbstractRegistryFactory {

    private PilotTransporter pilotTransporter;

    @Override
    protected Registry createRegistry(URL url) {
        return new PilotRegistry(url, pilotTransporter);
    }

    public void setPilotTransporter(PilotTransporter pilotTransporter) {
        this.pilotTransporter = pilotTransporter;
    }
}
