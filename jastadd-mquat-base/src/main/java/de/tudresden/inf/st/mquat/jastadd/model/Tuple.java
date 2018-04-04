package de.tudresden.inf.st.mquat.jastadd.model;

import java.util.AbstractMap;

public class Tuple<T1, T2> extends AbstractMap.SimpleEntry<T1, T2> {
  public Tuple(T1 firstElement, T2 secondElement) {
    super(firstElement, secondElement);
  }

  public static <T1, T2> Tuple<T1, T2> of(T1 firstElement, T2 secondElement) {
    return new Tuple<>(firstElement, secondElement);
  }

  public T1 getFirstElement() {
    return getKey();
  }

  public T2 getSecondElement() {
    return getValue();
  }
}
