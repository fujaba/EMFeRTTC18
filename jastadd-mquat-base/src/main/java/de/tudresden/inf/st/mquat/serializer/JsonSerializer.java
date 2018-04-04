package de.tudresden.inf.st.mquat.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.tudresden.inf.st.mquat.jastadd.model.ASTNode;
import de.tudresden.inf.st.mquat.jastadd.model.List;
import de.tudresden.inf.st.mquat.jastadd.model.Opt;
import de.tudresden.inf.st.mquat.jastadd.model.Root;

import java.io.File;
import java.io.IOException;

public class JsonSerializer {

  public static void write(Root r, String fileName) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    SimpleModule module = new SimpleModule();
    module.addSerializer(ASTNode.class, new ASTNodeSerializer());
    module.addSerializer(List.class, new ListSerializer());
    module.addSerializer(Opt.class, new OptSerializer());
    mapper.registerModule(module);

    try {
      mapper.writeValue(new File(fileName), r);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
