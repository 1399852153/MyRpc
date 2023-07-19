package myrpc.registry;

import myrpc.common.model.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FakeRegistry implements Registry{

    private static final Logger logger = LoggerFactory.getLogger(FakeRegistry.class);

    @Override
    public void doRegistry(ServiceInfo serviceInfo) {
        // do nothing
        logger.debug("FakeRegistry doRegistry do nothing!");
    }

    @Override
    public List<ServiceInfo> discovery(String serviceName) {
        // find nothing
        logger.debug("FakeRegistry discovery find nothing!");
        return new ArrayList<>();
    }
}
