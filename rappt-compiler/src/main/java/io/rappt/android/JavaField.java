package io.rappt.android;

public class JavaField {
    public Template annotation;
    public String initValue;
    public String type;
    public String fieldName;
    public boolean initNewObject;
    public boolean param;

    public JavaField(String type, String fieldName) {
        this.type = type;
        this.fieldName = fieldName;
    }

    public JavaField addAnnotation(Template annotation) {
        this.annotation = annotation;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaField that = (JavaField) o;

        if (fieldName != null ? !fieldName.equals(that.fieldName) : that.fieldName != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
        return result;
    }
}