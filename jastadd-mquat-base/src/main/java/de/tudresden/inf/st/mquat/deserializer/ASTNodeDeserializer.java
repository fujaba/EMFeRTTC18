package de.tudresden.inf.st.mquat.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.tudresden.inf.st.mquat.jastadd.model.ASTNode;
import de.tudresden.inf.st.mquat.jastadd.model.List;
import de.tudresden.inf.st.mquat.jastadd.model.Opt;
import de.tudresden.inf.st.mquat.jastadd.model.Root;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Deserialize JSON into an ASTNode.
 * Created by jm on 5/15/17.
 */
public class ASTNodeDeserializer extends StdDeserializer<ASTNode> {

  public ASTNodeDeserializer() {
    this(null);
  }

  public ASTNodeDeserializer(Class<?> vc) {
    super(vc);
  }

  public static Root read(File file) {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(ASTNode.class, new ASTNodeDeserializer());
    mapper.registerModule(module);

    try {
      ASTNode readValue = mapper.readValue(file, ASTNode.class);

      if (readValue instanceof Root) {
        return (Root) readValue;
      } else {
        throw new RuntimeException("Could not read a complete model");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    throw new RuntimeException("Could not read the model file " + file.getName());
  }

  @Override
  public ASTNode deserialize(JsonParser jp, DeserializationContext ctxt)
    throws IOException {

    JsonNode node = jp.getCodec().readTree(jp);

    return (ASTNode) deserializeObject(node);
  }

  private Object deserializeObject(JsonNode node) {
    if (node.isObject()) {
      String kind = node.get("k").asText();
      switch (kind) {
        case "NT":
          return deserializeNonterminal(node);
        case "List":
          return deserializeList(node);
        case "Opt":
          return deserializeOpt(node);
        case "t":
          return deserializeTerminal(node);
        case "enum":
          return deserializeEnum(node);
        default:
          throw new DeserializationException("cannot deserialize node of unknown kind " + kind);
      }
    } else {
      throw new DeserializationException("cannot deserialize non-object node as object node!");
    }
  }

  private ASTNode deserializeNonterminal(JsonNode node) {

    final String packageName = "de.tudresden.inf.st.mquat.jastadd.ast";

    // get the type we want to create
    String type = node.get("t").asText();
    Class<?> typeClass;
    try {
      typeClass = Class.forName(packageName + "." + type);
    } catch (ClassNotFoundException e) {
      throw new DeserializationException("Unable to find class of type " + type + " in package " + packageName, e);
    }

    // create the instance
    ASTNode instance;
    try {
      instance = (ASTNode) (typeClass.getConstructor().newInstance());
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new DeserializationException("Unable to construct a nonterminal of type " + typeClass.getCanonicalName(), e);
    }

    // call every setter we have a field for
    Iterator<String> f = node.get("c").fieldNames();
    while (f.hasNext()) {
      String fieldName = f.next();

      // serialize the parameter
      Object parameter = deserializeObject(node.get("c").get(fieldName));

      // find the setter to call
      boolean isList = node.get("c").get(fieldName).get("k").asText().equals("List");
      boolean isOpt = node.get("c").get(fieldName).get("k").asText().equals("Opt");
      // ... by getting its name
      String setterName = "set" + fieldName + (isList ? "List" : "") + (isOpt ? "Opt" : "");
      // ... and its type
      Class<?> setterType;
      if (isList) {
        setterType = List.class;
      } else if (isOpt) {
        setterType = Opt.class;
      } else {
        setterType = parameter.getClass();
      }
      Class<?> originalSettType = setterType;

      // get the method
      Method method = null;

      while(setterType != null && method == null) {
        try {
          method = typeClass.getMethod(setterName, setterType);
        } catch (NoSuchMethodException e1) {
          try {
            if (setterType.equals(Integer.class)) {
              method = typeClass.getMethod(setterName, int.class);
            } else if (setterType.equals(Double.class)) {
              method = typeClass.getMethod(setterName, double.class);
            } else if (setterType.equals(Long.class)) {
              method = typeClass.getMethod(setterName, long.class);
            } else if (setterType.equals(Character.class)) {
              method = typeClass.getMethod(setterName, char.class);
            } else if (setterType.equals(Boolean.class)) {
              method = typeClass.getMethod(setterName, boolean.class);
            } else if (setterType.equals(Float.class)) {
              method = typeClass.getMethod(setterName, float.class);
            }
            setterType = setterType.getSuperclass();
          } catch (NoSuchMethodException e2) {
            throw new DeserializationException("Unable to set value of " + fieldName + " with setter " + setterName, e2);
          }
        }
      }
      if (method == null) {
        throw new DeserializationException("Unable to set value of " + fieldName + " with setter " + setterName + " of type " + originalSettType.getSimpleName() + "!");
      }

      // invoke the method on the instance with the parameter
      try {
        method.invoke(instance, parameter);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new DeserializationException("Unable to set value of " + fieldName + " with setter " + setterName, e);
      }
    }

    // finally, return the instance
    return instance;
  }

  private ASTNode deserializeOpt(JsonNode node) {
    if (node.has("c")) {
      // opts can only contain Nonterminals
      ASTNode value = deserializeNonterminal(node.get("c"));
      return new Opt<ASTNode>(value);

    } else {
      return new Opt();
    }
  }

  private Object deserializeTerminal(JsonNode node) {
    // get the type name
    String typeName = node.get("t").asText();

    // first try the builtin types
    switch (typeName) {
      case "int":
      case "Integer":
        return node.get("v").asInt();
      case "float":
      case "Float":
        return (float) node.get("v").asDouble();
      case "boolean":
      case "Boolean":
        return node.get("v").asBoolean();
      case "double":
      case "Double":
        return node.get("v").asDouble();
      case "String":
        return node.get("v").asText();
      case "long":
      case "Long":
        return node.get("v").asLong();
      default:
        throw new DeserializationException("cannot create object of type " + typeName);
    }
  }

  private Enum deserializeEnum(JsonNode node) {
    // get the type name
    String typeName = node.get("t").asText();

    Class<?> type;
    try {
      type = Class.forName(typeName);
    } catch (ClassNotFoundException e) {
      throw new DeserializationException("cannot create enum of type " + typeName, e);
    }

    Method valueOf;
    try {
      valueOf = type.getMethod("valueOf", String.class);
    } catch (NoSuchMethodException e) {
      throw new DeserializationException("cannot call valueOf() on enum of type " + typeName, e);
    }
    try {
      return (Enum) valueOf.invoke(null, node.get("v").asText());
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new DeserializationException("cannot call valueOf() on enum of type " + typeName, e);
    }
  }

  private List deserializeList(JsonNode node) {
    List<ASTNode> list = new List<>();
    Iterator<JsonNode> it = node.get("c").elements();
    while (it.hasNext()) {
      JsonNode child = it.next();
      // lists can only contain Nonterminals
      list.add(deserializeNonterminal(child));
    }
    return list;
  }
}
