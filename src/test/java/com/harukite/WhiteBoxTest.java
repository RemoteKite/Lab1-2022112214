package com.harukite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class WhiteBoxTest {

  private GraphProcessor graphProcessor;

  private void setUp(String inputText) {
    graphProcessor = new GraphProcessor();
    graphProcessor.buildGraph(inputText);
  }

  @Test
  public void testPath1() {
    setUp("a b c");
    String result = graphProcessor.queryBridgeWords("d", "e");
    assertEquals("No \"d\" or \"e\" in the graph!", result);
  }

  @Test
  public void testPath2() {
    setUp("a b c");
    String result = graphProcessor.queryBridgeWords("c", "e");
    assertEquals("No \"c\" or \"e\" in the graph!", result);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPath3() throws NoSuchFieldException, IllegalAccessException {
    setUp("a b c");
    // 获取 graph 字段
    Field graphField = graphProcessor.getClass().getDeclaredField("graph");
    graphField.setAccessible(true);
    // 获取原始 Map 对象
    Map<String, Map<String, Integer>> graph =
        (Map<String, Map<String, Integer>>) graphField.get(graphProcessor);
    // 删除 key 为 "b"
    graph.remove("b");
    String result = graphProcessor.queryBridgeWords("a", "c");
    assertEquals("No bridge words from \"a\" to \"c\"!", result);
  }

  @Test
  public void testPath4() {
    setUp("a b c");
    String result = graphProcessor.queryBridgeWords("b", "a");
    assertEquals("No bridge words from \"b\" to \"a\"!", result);
  }

  @Test
  public void testPath5() {
    setUp("a b c");
    String result = graphProcessor.queryBridgeWords("a", "c");
    assertEquals("The bridge words from \"a\" to \"c\" is: \"b\".", result);
  }

  @Test
  public void testPath6() {
    setUp("a b d b c");
    String result = graphProcessor.queryBridgeWords("b", "b");
    assertEquals("The bridge words from \"b\" to \"b\" is: \"d\".", result);
  }

  @Test
  public void testPath7() {
    setUp("a b d b c b e b");
    String result = graphProcessor.queryBridgeWords("b", "b");
    assertEquals("The bridge words from \"b\" to \"b\" are: \"c\", \"d\" and \"e\".", result);
  }

}

