package com.cloud.api.response;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.ResponseObject;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class ApiResponseSerializer {
    private static final Logger s_logger = Logger.getLogger(ApiResponseSerializer.class.getName());

    public static String toSerializedString(ResponseObject result, String responseType) {
        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            return toJSONSerializedString(result);
        } else {
            return toXMLSerializedString(result);
        }
    }

    private static String toJSONSerializedString(ResponseObject result) {
        if (result != null) {
            Gson gson = GsonHelper.getBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
            StringBuilder sb = new StringBuilder();

            sb.append("{ \"" + result.getResponseName() + "\" : ");
            if (result instanceof ListResponse) {
                List<? extends ResponseObject> responses = ((ListResponse)result).getResponses();
                if ((responses != null) && !responses.isEmpty()) {
                    int count = responses.size();
                    String jsonStr = gson.toJson(responses.get(0));
                    sb.append("{ \"" + responses.get(0).getResponseName() + "\" : [  " + jsonStr);
                    for (int i = 1; i < count; i++) {
                        jsonStr = gson.toJson(responses.get(i));
                        sb.append(", " + jsonStr);
                    }
                    sb.append(" ] }");
                } else {
                    sb.append("{ }");
                }
/*
 * If the old style (2.1.x) async job responses are desired, uncomment the following code.  Note:  Many of the commands will need to set the response name to
 * something like "getResultObjectName()" [see StopVMCmd for an example] in order to truly reinstate the old behavior.  The current response names are based
 * on the new style.  Also, this is done for JSON, so the XML Serializer will need to be fixed up to compensate, but the following code can be used to guide
 * the changes to XML serializer. */
//            } else if (result instanceof AsyncJobResponse) {
//                // this code is in here to preserve old behavior for the async job result response
//                AsyncJobResponse asyncResponse = (AsyncJobResponse)result;
//                if ("object".equalsIgnoreCase(asyncResponse.getJobResultType())) {
//                    // we require special handling for object, otherwise we serialize it the standard way
//                    ResponseObject subResponse = asyncResponse.getJobResult();
//                    asyncResponse.setJobResult(null);
//                    String jsonStr = gson.toJson(result);
//                    int index = jsonStr.lastIndexOf('}');
//                    sb.append(jsonStr.substring(0, index));
//                    String subRespJson = gson.toJson(subResponse);
//                    sb.append(", \"" + subResponse.getResponseName() + "\" : [ " + subRespJson + " ] }");
//                } else {
//                    String jsonStr = gson.toJson(result);
//                    if ((jsonStr != null) && !"".equals(jsonStr)) {
//                        sb.append(jsonStr);
//                    } else {
//                        sb.append("{ }");
//                    }
//                }


            } else {
                String jsonStr = gson.toJson(result);
                if ((jsonStr != null) && !"".equals(jsonStr)) {
                    sb.append(jsonStr);
                } else {
                    sb.append("{ }");
                }
            }
            sb.append(" }");
            return sb.toString();
        }
        return null;
    }

    private static String toXMLSerializedString(ResponseObject result) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        sb.append("<" + result.getResponseName() + " cloud-stack-version=\""+ApiDBUtils.getVersion()+ "\">");

        if (result instanceof ListResponse) {
            List<? extends ResponseObject> responses = ((ListResponse)result).getResponses();
            if ((responses != null) && !responses.isEmpty()) {
                for (ResponseObject obj : responses) {
                    serializeResponseObjXML(sb, obj);
                }
            }
        } else {
            serializeResponseObjFieldsXML(sb, result);
        }
        
        sb.append("</" + result.getResponseName() + ">");
        return sb.toString();
    }

    private static void serializeResponseObjXML(StringBuilder sb, ResponseObject obj) {
        if (!(obj instanceof SuccessResponse)&& !(obj instanceof ExceptionResponse))
            sb.append("<" + obj.getResponseName() + ">");
        serializeResponseObjFieldsXML(sb, obj);
        if (!(obj instanceof SuccessResponse) && !(obj instanceof ExceptionResponse))
            sb.append("</" + obj.getResponseName() + ">");
    }

    private static void serializeResponseObjFieldsXML(StringBuilder sb, ResponseObject obj) {
        boolean isAsync = false;
        if (obj instanceof AsyncJobResponse)
            isAsync = true;
        
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) {
                continue;  // skip transient fields
            }

            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (serializedName == null) {
                continue; // skip fields w/o serialized name
            }

            String propName = field.getName();                
            Method method = getGetMethod(obj, propName);
            if (method != null) {
                try {
                    Object fieldValue = method.invoke(obj);
                    if (fieldValue != null) {
                        if (fieldValue instanceof ResponseObject) {
                            ResponseObject subObj = (ResponseObject)fieldValue;
                            if (isAsync) {
                                sb.append("<jobresult>");
                            }
                            serializeResponseObjXML(sb, subObj);
                            if (isAsync) {
                                sb.append("</jobresult>");
                            }
                        } else if (fieldValue instanceof Date) {
                            sb.append("<" + serializedName.value() + ">" + BaseCmd.getDateString((Date)fieldValue) + "</" + serializedName.value() + ">");
                        } else {
                            sb.append("<" + serializedName.value() + ">" + fieldValue.toString() + "</" + serializedName.value() + ">");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    s_logger.error("Illegal argument exception when calling ResponseObject " + obj.getClass().getName() + " get method for property: " + propName);
                } catch (IllegalAccessException e) {
                    s_logger.error("Illegal access exception when calling ResponseObject " + obj.getClass().getName() + " get method for property: " + propName);
                } catch (InvocationTargetException e) {
                    s_logger.error("Invocation target exception when calling ResponseObject " + obj.getClass().getName() + " get method for property: " + propName);
                }
            }
        }
    }

    private static Method getGetMethod(Object o, String propName) {
        Method method = null;
        String methodName = getGetMethodName("get", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting ResponseObject " + o.getClass().getName() + " get method for property: "
                    + propName);
        } catch (NoSuchMethodException e1) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("ResponseObject " + o.getClass().getName() + " does not have " + methodName
                        + "() method for property: " + propName
                        + ", will check is-prefixed method to see if it is boolean property");
            }
        }

        if (method != null)
            return method;

        methodName = getGetMethodName("is", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting ResponseObject " + o.getClass().getName() + " get method for property: "
                    + propName);
        } catch (NoSuchMethodException e1) {
            s_logger.warn("ResponseObject " + o.getClass().getName() + " does not have " + methodName + "() method for property: "
                    + propName);
        }
        return method;
    }

    private static String getGetMethodName(String prefix, String fieldName) {
        StringBuffer sb = new StringBuffer(prefix);

        if (fieldName.length() >= prefix.length() && fieldName.substring(0, prefix.length()).equals(prefix)) {
            return fieldName;
        } else {
            sb.append(fieldName.substring(0, 1).toUpperCase());
            sb.append(fieldName.substring(1));
        }

        return sb.toString();
    }
}
