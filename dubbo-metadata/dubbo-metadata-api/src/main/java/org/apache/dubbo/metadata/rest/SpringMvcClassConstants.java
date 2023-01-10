package org.apache.dubbo.metadata.rest;

import static org.apache.dubbo.common.utils.ClassUtils.getClassLoader;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;

public interface SpringMvcClassConstants extends RestMetadataConstants.SPRING_MVC {
    /**
     * The annotation class of @RequestMapping
     */
    Class REQUEST_MAPPING_ANNOTATION_CLASS = resolveClass(REQUEST_MAPPING_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @RequestHeader
     */
    Class REQUEST_HEADER_ANNOTATION_CLASS = resolveClass(REQUEST_HEADER_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @RequestParam
     */
    Class REQUEST_PARAM_ANNOTATION_CLASS = resolveClass(REQUEST_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @RequestBody
     */
    Class REQUEST_BODY_ANNOTATION_CLASS = resolveClass(REQUEST_BODY_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @RequestBody
     */
    Class  PATH_VARIABLE_ANNOTATION_CLASS= resolveClass(PATH_VARIABLE_ANNOTATION_CLASS_NAME, getClassLoader());
}
