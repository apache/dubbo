package org.apache.dubbo.gen.tri.reactive;

import com.salesforce.jprotoc.ProtocPlugin;
import org.apache.dubbo.gen.AbstractGenerator;

public class ReactorDubbo3TripleGenerator extends AbstractGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            ProtocPlugin.generate(new ReactorDubbo3TripleGenerator());
        } else {
            ProtocPlugin.debug(new ReactorDubbo3TripleGenerator(), args[0]);
        }
    }

    @Override
    protected String getClassPrefix() {
        return "Dubbo";
    }

    @Override
    protected String getClassSuffix() {
        return "Triple";
    }

    @Override
    protected String getTemplateFileName() {
        return "ReactorDubbo3TripleStub.mustache";
    }

    @Override
    protected String getInterfaceTemplateFileName() {
        return "ReactorDubbo3TripleInterfaceStub.mustache";
    }

    @Override
    protected String getSingleTemplateFileName() {
        throw new IllegalStateException("Do not support single template!");
    }

    @Override
    protected boolean enableMultipleTemplateFiles() {
        return true;
    }
}
