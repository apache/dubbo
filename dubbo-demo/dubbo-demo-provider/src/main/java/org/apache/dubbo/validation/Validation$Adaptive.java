package org.apache.dubbo.validation;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class Validation$Adaptive implements org.apache.dubbo.validation.Validation {
    public org.apache.dubbo.validation.Validator getValidator(org.apache.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("validation", "jvalidation");
        if(extName == null) throw new IllegalStateException("Fail to get extension(org.apache.dubbo.validation.Validation) name from url(" + url.toString() + ") use keys([validation])");
        org.apache.dubbo.validation.Validation extension = (org.apache.dubbo.validation.Validation)ExtensionLoader.getExtensionLoader(org.apache.dubbo.validation.Validation.class).getExtension(extName);
        return extension.getValidator(arg0);
    }
}