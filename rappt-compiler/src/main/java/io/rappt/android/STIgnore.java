package io.rappt.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation for fields not to be added to StringTemplate
// Will have no affect on nested template parameters that is it will only work on fields in
// a class that subclasses FieldTemplate
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface STIgnore {
}
