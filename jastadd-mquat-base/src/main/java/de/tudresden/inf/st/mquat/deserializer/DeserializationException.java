package de.tudresden.inf.st.mquat.deserializer;

public class DeserializationException extends RuntimeException {
  public DeserializationException() {
  }

  public DeserializationException(String message) {
    super(message);
  }

  public DeserializationException(String message, Throwable cause) {
    super(message, cause);
  }

  public DeserializationException(Throwable cause) {
    super(cause);
  }

  public DeserializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
