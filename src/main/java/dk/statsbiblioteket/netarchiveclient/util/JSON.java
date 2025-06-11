package dk.statsbiblioteket.netarchiveclient.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601Utils;

import javax.ws.rs.ext.ContextResolver;
import java.io.File;
import java.io.IOException;
import java.text.FieldPosition;
import java.util.Date;


public class JSON implements ContextResolver<ObjectMapper> {
  private ObjectMapper mapper;

  public JSON() {
    mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);

    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

    mapper.setDateFormat(new RFC3339DateFormat());
  }
  
  public static String toJson(Object object) {
      return toJson(object, true);
  }
  
  public static <T> String toJson(T object, boolean indent) {
      
      if (object == null) {
          return "";
      }
      JSON json = new JSON();
      ObjectMapper mapper = json.getContext(object.getClass());
      
      
      if (indent) {
          mapper.enable(SerializationFeature.INDENT_OUTPUT);
      } else {
          mapper.disable(SerializationFeature.INDENT_OUTPUT);
      }
      
      
      try {
          return mapper.writeValueAsString(object);
      } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
      }
  }
  
  public static <T> T fromJson(String object, Class<T> type) {
      JSON json = new JSON();
      ObjectMapper mapper = json.getContext(type);
      
      try {
          return mapper.readValue(object, type);
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }
    
    public static <T> T fromJson(File object, Class<T> type) {
        JSON json = new JSON();
        ObjectMapper mapper = json.getContext(type);
        
        try {
            return mapper.readValue(object, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
  public ObjectMapper getContext(Class<?> type) {
    return mapper;
  }


  static class RFC3339DateFormat extends com.fasterxml.jackson.databind.util.StdDateFormat {

    private static final long serialVersionUID = -3215326930097719238L;

    // Same as ISO8601DateFormat but serializing milliseconds.
    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
      String value = ISO8601Utils.format(date, true);
      toAppendTo.append(value);
      return toAppendTo;
    }

  }
}
