package de.tudresden.inf.st.mquat.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.tudresden.inf.st.mquat.jastadd.model.ASTNode;
import de.tudresden.inf.st.mquat.jastadd.model.ASTNodeAnnotation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ASTNodeSerializer extends StdSerializer<ASTNode> {

  public ASTNodeSerializer() {
    this(null);
  }

  public ASTNodeSerializer(Class<ASTNode> t) {
    super(t);
  }

  @Override
  public void serialize(
    ASTNode value, JsonGenerator jgen, SerializerProvider provider)
    throws IOException {

    jgen.writeStartObject();
    jgen.writeStringField("k", "NT");
    jgen.writeStringField("t", value.getClass().getSimpleName());
    jgen.writeObjectFieldStart("c");
    for (Method m : value.getClass().getMethods()) {
      try {
        if (m.getAnnotation(ASTNodeAnnotation.Child.class) != null) {
          jgen.writeFieldName(m.getAnnotation(ASTNodeAnnotation.Child.class).name());
          provider.defaultSerializeValue(m.invoke(value), jgen);
        } else if (m.getAnnotation(ASTNodeAnnotation.Token.class) != null) {
          jgen.writeFieldName(m.getAnnotation(ASTNodeAnnotation.Token.class).name());
          jgen.writeStartObject();
          jgen.writeStringField("k", m.getReturnType().isEnum() ? "enum" : "t");
          jgen.writeStringField("t", m.getReturnType().getName());
          jgen.writeFieldName("v");
          provider.defaultSerializeValue(m.invoke(value), jgen);
          jgen.writeEndObject();
        } else if (m.getAnnotation(ASTNodeAnnotation.ListChild.class) != null) {
          jgen.writeFieldName(m.getAnnotation(ASTNodeAnnotation.ListChild.class).name());
          provider.defaultSerializeValue(m.invoke(value), jgen);
        } else if (m.getAnnotation(ASTNodeAnnotation.OptChild.class) != null) {
          jgen.writeFieldName(m.getAnnotation(ASTNodeAnnotation.OptChild.class).name());
          provider.defaultSerializeValue(m.invoke(value), jgen);
        }
      } catch (IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
      }
    }
    jgen.writeEndObject();
    jgen.writeEndObject();
  }
}
