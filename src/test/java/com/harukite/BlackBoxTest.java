package com.harukite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BlackBoxTest {

  private GraphProcessor graphProcessor;

  private void setUp() {
    graphProcessor = new GraphProcessor();
    String inputText =
        """
            start begin alpha beta gamma
            start begin alpha delta gamma
            start begin alpha epsilon gamma
            start begin alpha zeta gamma
            quick fox jumps over lazy dog
            quick fox runs past sleepy cat
            quick fox leaps over tired wolf
            """;
    graphProcessor.buildGraph(inputText);
  }

  @Test
  public void testEmptyString() {
    setUp();
    String result = graphProcessor.generateNewText("");
    assertEquals("输入文本似乎不包含任何单词!", result);
  }

  @Test
  public void testInvalidString() {
    setUp();
    String result = graphProcessor.generateNewText("你好");
    assertEquals("输入文本似乎不包含任何单词!", result);
  }

  @Test
  public void testValidStringNoBridge() {
    setUp();
    String result = graphProcessor.generateNewText("quick fox jumps over lazy dog");
    assertEquals("quick fox jumps over lazy dog", result);
  }

  @Test
  public void testValidStringSingleBridge() {
    setUp();
    String result = graphProcessor.generateNewText("quick jumps over lazy dog");
    assertEquals("quick fox jumps over lazy dog", result);
  }

  @Test
  public void testValidStringTwoBridge() {
    setUp();
    String result = graphProcessor.generateNewText("quick fox over lazy dog");
    assertTrue(result.equals("quick fox jumps over lazy dog") ||
        result.equals("quick fox leaps over lazy dog"));
  }

  @Test
  public void testValidStringMultipleBridge() {
    setUp();
    String result = graphProcessor.generateNewText("start begin alpha gamma");
    assertTrue(result.equals("start begin alpha beta gamma") || result.equals(
        "start begin alpha delta gamma") || result.equals("start begin alpha epsilon gamma") ||
        result.equals("start begin alpha zeta gamma"));
  }

  @Test
  public void testSingleWord() {
    setUp();
    String result = graphProcessor.generateNewText("start");
    assertEquals("start", result);
  }

  @Test
  public void testCombinedCapital() {
    setUp();
    String result = graphProcessor.generateNewText("qUiCk JuMps ovEr lAZy dOg");
    assertEquals("qUiCk fox JuMps ovEr lAZy dOg", result);
  }

  @Test
  public void testCombinedCharacter() {
    setUp();
    String result = graphProcessor.generateNewText("qUiCk你JuMps好ovEr啊lAZy。dOg");
    assertEquals("qUiCk fox JuMps ovEr lAZy dOg", result);
  }
}
