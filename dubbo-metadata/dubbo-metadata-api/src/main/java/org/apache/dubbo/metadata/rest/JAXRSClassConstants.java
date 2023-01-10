package org.apache.dubbo.metadata.rest;

import static org.apache.dubbo.common.utils.ClassUtils.getClassLoader;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;

public interface JAXRSClassConstants extends RestMetadataConstants.JAX_RS {
    /**
     * The annotation class of @Path
     */
    Class PATH_ANNOTATION_CLASS = resolveClass(PATH_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @FormParam
     */
    Class FORM_PARAM_ANNOTATION_CLASS = resolveClass(FORM_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());

    /**
     * The annotation class of @HeaderParam
     */
    Class HEADER_PARAM_ANNOTATION_CLASS = resolveClass(HEADER_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @MatrixParam
     */
    Class MATRIX_PARAM_ANNOTATION_CLASS = resolveClass(MATRIX_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class  of @QueryParam
     */
    Class QUERY_PARAM_ANNOTATION_CLASS = resolveClass(QUERY_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());

    /**
     * The annotation class of @Body
     */
    Class REST_EASY_BODY_ANNOTATION_CLASS = resolveClass(REST_EASY_BODY_ANNOTATION_CLASS_NAME, getClassLoader());

    /**
     * The annotation class of @PathParam
     */
    Class PATH_PARAM_ANNOTATION_CLASS = resolveClass(PATH_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());


}
