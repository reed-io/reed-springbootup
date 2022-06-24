/**
 * E5Projects @ org.reed.controller/ReedBaseController.java
 */
package org.reed.controller;

import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author chenxiwen
 * @createTime 2019年09月17日 下午3:57
 * @description
 */
@RestController
public abstract class ReedBaseController {

    @GetMapping(value={"/", "/?", "/help"})
    @ResponseBody
    String help(){
        String packageName = this.getClass().getPackage().getName();
        String className = this.getClass().getSimpleName();
        Set<Method> methodSet = new HashSet<>();
        Method[] methods = this.getClass().getMethods();
        for(Method method : methods){
            Annotation[] annotations = method.getAnnotations();
            for(Annotation annotation : annotations){
                if(isMappingAnnotation(annotation)){
                    methodSet.add(method);
                    break;
                }
            }
        }
        ReedController controller = new ReedController();
        controller.setPackageName(packageName);
        controller.setClassName(className);
        controller.setReachableMethods(methodSet);
        controller.setVersion(version());
        return controller.toString();
    }

    public abstract String version();

    private boolean isMappingAnnotation(Annotation annotation){
        Class<?> cls = annotation.annotationType();
        return cls.equals(RequestMapping.class) ||
                cls.equals(GetMapping.class) ||
                cls.equals(PostMapping.class) ||
                cls.equals(DeleteMapping.class) ||
                cls.equals(PutMapping.class) ||
                cls.equals(PatchMapping.class);
    }

    class ReedController{
        private String packageName;
        private String className;
        private Set<Method> reachableMethods;
        private String version;

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public Set<Method> getReachableMethods() {
            return reachableMethods;
        }

        public void setReachableMethods(Set<Method> reachableMethods) {
            this.reachableMethods = reachableMethods;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return "ReedController{" +
                    "packageName='" + packageName + '\'' +
                    ", className='" + className + '\'' +
                    ", reachableMethods=" + reachableMethods +
                    ", version='" + version + '\'' +
                    '}';
        }
    }

}
