package org.apache.dubbo.gen;

import com.google.protobuf.compiler.PluginProtos;
import java.io.IOException;
import java.util.List;


public class DubboProtocPlugin {

    public static void generate(AbstractGenerator generator) {
        try{
            PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(System.in);
            List<PluginProtos.CodeGeneratorResponse.File> files = generator.generateFiles(request);
            PluginProtos.CodeGeneratorResponse.newBuilder().addAllFile(files).setSupportedFeatures(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL.getNumber()).build().writeTo(System.out);
        }catch (Exception e){
            try {
                PluginProtos.CodeGeneratorResponse.newBuilder().setError(e.getMessage()).build().writeTo(System.out);
            } catch (IOException var6) {
                exit(e);
            }
        }catch (Throwable var8) {
            exit(var8);
        }
    }

    public static void exit(Throwable e){
        e.printStackTrace(System.err);
        System.exit(1);
    }
}
