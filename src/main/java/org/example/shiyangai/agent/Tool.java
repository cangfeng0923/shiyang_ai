package org.example.shiyangai.agent;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented //工具注解
public @interface Tool {
    String name();
    String description();
    String[] parameters() default {};
}
