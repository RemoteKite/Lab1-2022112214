package com.harukite;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Factory.to;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.Node;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;

/**
 * 文本图处理器类，负责构建和操作基于词语关系的图结构，并提供图形用户界面（GUI）交互。
 *
 * <p>该类继承自 {@link JFrame}，集成了图构建、词频统计、游走算法、
 * 以及路径可视化等功能，适用于处理自然语言文本中的词图分析。.
 */
public class GraphProcessor extends JFrame {

  private int wordNum;
  private Map<String, Integer> wordCount;
  private Map<String, Map<String, Integer>> graph;
  private final SecureRandom random;
  private final Set<String> visitedEdges;
  private final List<String> walkPath;
  private volatile boolean walkStopped;
  private boolean idf; // 是否使用IDF加权
  private volatile boolean walkDelay; // 是否延迟游走
  private Thread walkThread; // 保存线程引用
  private boolean showPathOnGraph; // 是否在图上显示路径

  // UI组件
  private JTextArea outputArea;
  private JTextField filePathField;
  private JTextField word1Field;
  private JTextField word2Field;
  private JTextField inputTextField;
  private JTextField startWordField;
  private JTextField endWordField;
  private JTextField targetWordField;
  private JTextField graphOutputField;
  private JSVGCanvas svgCanvas;

  /**
   * 构造方法，初始化图处理器所需的成员变量并构建图形用户界面。
   *
   * <p>该方法设置默认参数，例如是否使用 IDF 加权、是否延迟游走等，
   * 并调用 {@code initializeUi()} 方法加载主界面。.
   */
  public GraphProcessor() {
    wordNum = 0;
    wordCount = new HashMap<>();
    graph = new HashMap<>();
    random = new SecureRandom();
    visitedEdges = new HashSet<>();
    walkPath = new ArrayList<>();
    walkStopped = true;
    idf = false; // 默认不使用IDF加权
    walkDelay = false; // 默认不延迟游走
    showPathOnGraph = false; // 默认不在图上显示路径

    initializeUi();
  }

  /**
   * 读取指定路径的文本文件内容。
   *
   * <p>该方法以 UTF-8 编码按行读取文件，并将每行内容以空格分隔拼接为一个字符串返回。.
   *
   * @param filePath 要读取的文件路径，不能为空
   * @return 文件内容拼接后的字符串
   * @throws IOException 如果文件读取过程中发生 I/O 错误
   */
  @SuppressFBWarnings(
      value = "PATH_TRAVERSAL_IN",
      justification = "BY DESIGN: The file path is provided through a file chooser dialog."
  )
  private static String readFile(String filePath) throws IOException {
    StringBuilder content = new StringBuilder();
    Path fp = Paths.get(filePath);
    BufferedReader reader = Files.newBufferedReader(fp, StandardCharsets.UTF_8);
    String line;
    while ((line = reader.readLine()) != null) {
      content.append(line).append(" ");
    }
    reader.close();
    return content.toString();
  }

  /**
   * 应用程序的入口点。
   *
   * <p>该方法使用 {@link javax.swing.SwingUtilities#invokeLater(Runnable)}
   * 在事件调度线程（EDT）上启动图形界面。.
   *
   * @param args 命令行参数（当前未使用）
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(GraphProcessor::new);
  }

  private void initializeUi() {
    setTitle("文本图处理器");
    setSize(1000, 700);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    filePathField = new JTextField();
    // if debug is needed , change this to true
    filePathField.setEditable(false);
    JButton browseButton = new JButton("浏览");
    browseButton.addActionListener(e -> browseFile());
    JButton loadButton = new JButton("加载文件");
    loadButton.addActionListener(e -> loadFile());

    // 文件选择面板
    JPanel filePanel = new JPanel(new BorderLayout());
    filePanel.add(new JLabel("文本文件路径:"), BorderLayout.WEST);
    filePanel.add(filePathField, BorderLayout.CENTER);
    filePanel.add(browseButton, BorderLayout.EAST);
    filePanel.add(loadButton, BorderLayout.SOUTH);

    // 图形生成面板
    JPanel graphPanel = new JPanel(new BorderLayout());
    graphOutputField = new JTextField("graph.png");
    JButton generateGraphButton = new JButton("生成图形");
    generateGraphButton.addActionListener(e -> showDirectedGraph(graphOutputField.getText()));

    graphPanel.add(new JLabel("图形输出文件:"), BorderLayout.WEST);
    graphPanel.add(graphOutputField, BorderLayout.CENTER);
    graphPanel.add(generateGraphButton, BorderLayout.EAST);

    // 顶部面板 - 文件选择和基本操作
    JPanel topPanel = new JPanel(new GridLayout(2, 1));
    topPanel.add(filePanel);
    topPanel.add(graphPanel);

    // 1. 桥接词查询
    word1Field = new JTextField();
    word2Field = new JTextField();
    JButton bridgeButton = new JButton("查询桥接词");
    bridgeButton.addActionListener(e -> queryBridgeWordsUi());

    JPanel bridgeInputPanel = new JPanel(new GridLayout(2, 2));
    bridgeInputPanel.add(new JLabel("单词1:"));
    bridgeInputPanel.add(word1Field);
    bridgeInputPanel.add(new JLabel("单词2:"));
    bridgeInputPanel.add(word2Field);

    JPanel bridgePanel = new JPanel(new BorderLayout());
    bridgePanel.add(bridgeInputPanel, BorderLayout.CENTER);
    bridgePanel.add(bridgeButton, BorderLayout.SOUTH);

    // 2. 生成新文本
    JPanel newTextPanel = new JPanel(new BorderLayout());
    inputTextField = new JTextField();
    JButton newTextButton = new JButton("生成新文本");
    newTextButton.addActionListener(e -> generateNewTextUi());

    newTextPanel.add(new JLabel("输入文本:"), BorderLayout.NORTH);
    newTextPanel.add(inputTextField, BorderLayout.CENTER);
    newTextPanel.add(newTextButton, BorderLayout.SOUTH);

    // 3. 最短路径
    startWordField = new JTextField();
    endWordField = new JTextField();
    JButton pathButton = new JButton("计算最短路径");
    pathButton.addActionListener(e -> calcShortestPathUi());

    JPanel pathInputPanel = new JPanel(new GridLayout(2, 2));
    pathInputPanel.add(new JLabel("起始单词:"));
    pathInputPanel.add(startWordField);
    pathInputPanel.add(new JLabel("目标单词:"));
    pathInputPanel.add(endWordField);

    JCheckBox showPathCheckBox = new JCheckBox("在图上显示路径");
    showPathCheckBox.addActionListener(e -> showPathOnGraph = showPathCheckBox.isSelected());

    JPanel pathPanel = new JPanel(new BorderLayout());
    pathPanel.add(pathInputPanel, BorderLayout.CENTER);
    pathPanel.add(showPathCheckBox, BorderLayout.EAST);
    pathPanel.add(pathButton, BorderLayout.SOUTH);

    // 4. PageRank
    targetWordField = new JTextField();
    JButton rankButton = new JButton("计算PageRank");
    rankButton.addActionListener(e -> calPageRankUi());

    // IDF加权选项
    JCheckBox idfCheckBox = new JCheckBox("使用IDF加权");
    idfCheckBox.addActionListener(e -> idf = idfCheckBox.isSelected());

    JPanel rankPanel = new JPanel(new BorderLayout());
    rankPanel.add(idfCheckBox, BorderLayout.EAST);
    rankPanel.add(new JLabel("目标单词:"), BorderLayout.NORTH);
    rankPanel.add(targetWordField, BorderLayout.CENTER);
    rankPanel.add(rankButton, BorderLayout.SOUTH);

    // 5. 随机游走
    JPanel walkPanel = new JPanel(new BorderLayout());
    JCheckBox delayCheckBox = new JCheckBox("延迟游走");
    delayCheckBox.addActionListener(e -> walkDelay = delayCheckBox.isSelected());
    walkPanel.add(delayCheckBox, BorderLayout.EAST);
    JButton walkButton = new JButton("随机游走");
    walkButton.addActionListener(e -> randomWalkUi());
    walkPanel.add(walkButton, BorderLayout.SOUTH);

    // 功能区
    JPanel functionPanel = new JPanel(new GridLayout(6, 1));
    // 添加所有功能组件
    functionPanel.add(bridgePanel);
    functionPanel.add(newTextPanel);
    functionPanel.add(pathPanel);
    functionPanel.add(rankPanel);
    functionPanel.add(walkPanel);

    // 6. 显示图形
    svgCanvas = new JSVGCanvas() {
      @Override
      public String getToolTipText(MouseEvent evt) {
        return null; // 彻底禁用所有悬浮提示
      }

      //重写paintComponent方法
      @Override
      public void paintComponent(Graphics g) {
        if (g instanceof Graphics2D g2d) {
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        super.paintComponent(g);
      }
    };

    // 输出区
    outputArea = new JTextArea();
    outputArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(outputArea);
    //clear outputArea
    JButton clearButton = new JButton("清空输出区");
    clearButton.addActionListener(e -> outputArea.setText(""));
    JPanel outputPanel = new JPanel(new BorderLayout());
    outputPanel.add(scrollPane, BorderLayout.CENTER);
    outputPanel.add(clearButton, BorderLayout.SOUTH);

    JScrollPane imagePane = new JScrollPane(svgCanvas);
    JSplitPane leftPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, outputPanel, imagePane);
    JSplitPane centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, functionPanel, leftPanel);

    // 添加到主窗口
    add(topPanel, BorderLayout.NORTH);
    add(centerPanel, BorderLayout.CENTER);

    setVisible(true);
  }

  private void browseFile() {
    JFileChooser fileChooser = new JFileChooser();
    int returnValue = fileChooser.showOpenDialog(this);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      filePathField.setText(selectedFile.getAbsolutePath());
    }
  }

  private void loadFile() {
    String filePath = filePathField.getText();
    if (filePath.isEmpty()) {
      outputArea.append("请先选择文件!\n");
      return;
    }

    try {
      graph = new HashMap<>();
      String text = readFile(filePath);
      buildGraph(text);
      outputArea.append("图构建完成!\n");
    } catch (IOException e) {
      outputArea.append("读取文件失败: " + e.getMessage() + "\n");
    }
  }

  private void queryBridgeWordsUi() {
    if (word1Field.getText().isEmpty() || word2Field.getText().isEmpty()) {
      outputArea.append("请填写两个单词!\n");
      return;
    }
    String word1 = word1Field.getText().toLowerCase();
    String word2 = word2Field.getText().toLowerCase();
    String result = queryBridgeWords(word1, word2);
    outputArea.append(result + "\n");
  }

  private void generateNewTextUi() {
    if (inputTextField.getText().isEmpty()) {
      outputArea.append("请输入文本!\n");
      return;
    }
    String inputText = inputTextField.getText();
    String result = generateNewText(inputText);
    outputArea.append("生成的新文本: " + result + "\n");
  }

  private void calcShortestPathUi() {
    if (startWordField.getText().isEmpty()) {
      outputArea.append("请填写起始单词!\n");
      return;
    }
    String word1 = startWordField.getText().toLowerCase();
    String word2 = endWordField.getText().toLowerCase();
    String result = calcShortestPath(word1, word2.isEmpty() ? null : word2);
    outputArea.append(result + "\n");
  }

  private void calPageRankUi() {
    String word = targetWordField.getText().toLowerCase();
    Double result = calPageRank(word);
    outputArea.append(String.format("单词 '%s' 的PageRank值为: %.6f%n", word, result));
  }

  private void randomWalkUi() {
    if (!walkStopped && walkThread != null) {
      // 中断正在运行的游走
      walkStopped = true;
      walkThread.interrupt(); // 发送中断信号
      outputArea.append("随机游走已中断\n");
      return;
    }

    // 开始新的游走
    walkStopped = false;
    walkThread = new Thread(() -> {
      String result = randomWalks();
      SwingUtilities.invokeLater(() -> outputArea.append("随机游走路径: " + result + "\n"));
    });
    walkThread.start();
  }

  /**
   * 构建词图结构。
   *
   * <p>该方法会将输入文本中的英文单词按顺序提取出来，构建词之间的有向图结构，
   * 统计每个单词的出现次数，并计算总词数。图中每个边的权重表示相邻词对出现的次数。.
   *
   * @param text 原始文本内容，将被统一转换为小写并按非字母字符进行分词
   */
  public void buildGraph(String text) {
    String[] words = text.toLowerCase().split("[^a-zA-Z]+");
    wordCount = new HashMap<>();
    wordNum = 0;
    for (String word : words) {
      if (!word.isEmpty()) {
        ++wordNum;
        graph.putIfAbsent(word, new HashMap<>());
        wordCount.merge(word, 1, Integer::sum);
      }
    }
    for (int i = 0; i < words.length - 1; i++) {
      String current = words[i];
      String next = words[i + 1];

      if (current.isEmpty() || next.isEmpty()) {
        continue;
      }
      graph.get(current).merge(next, 1, Integer::sum);
    }
  }

  /**
   * 查找两个单词之间的桥接词（bridge words）。
   *
   * <p>桥接词定义为：在图中，若存在一条从 {@code word1} 到某个词，再到 {@code word2} 的路径，
   * 则该中间词即为桥接词。例如，若存在路径 word1 → bridge → word2，则 bridge 是桥接词。.
   *
   * @param word1 起始单词
   * @param word2 目标单词
   * @return 所有桥接词组成的集合；如果没有桥接词，则返回空集
   * @throws NullPointerException 如果 {@code word1} 在图中不存在
   */
  public Set<String> bridgeWords(String word1, String word2) {
    Set<String> bridges = new HashSet<>();
    Map<String, Integer> word1Neighbors = graph.get(word1);

    for (String neighbor : word1Neighbors.keySet()) {
      //这一判断其实已经没什么必要了，因为在buildGraph中已经保证每个单词都在graph中
      if (graph.containsKey(neighbor)) {
        Map<String, Integer> neighborNeighbors = graph.get(neighbor);
        if (neighborNeighbors.containsKey(word2)) {
          bridges.add(neighbor);
        }
      }
    }
    return bridges;
  }

  /**
   * 查询两个单词之间的桥接词，并返回格式化的结果字符串。
   *
   * <p>如果任一单词不在词图中，返回提示信息。
   * 如果存在桥接词，则返回桥接词列表的自然语言描述；否则返回无桥接词的提示。.
   *
   * @param word1 起始单词
   * @param word2 目标单词
   * @return 包含查询结果的字符串，便于直接显示给用户
   */
  public String queryBridgeWords(String word1, String word2) {
    if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
      return "No \"" + word1 + "\" or \"" + word2 + "\" in the graph!";
    }

    Set<String> bridges = bridgeWords(word1, word2);

    if (bridges.isEmpty()) {
      return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
    } else {
      StringBuilder result = new StringBuilder(
          "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" ");
      int i = bridges.size();
      if (i > 1) {
        result.append("are: ");
      } else {
        result.append("is: ");
      }
      for (String bridge : bridges) {
        result.append("\"").append(bridge).append("\"");
        //如果是最后一个，加and
        if (--i > 0) {
          if (i == 1) {
            result.append(" and ");
          } else {
            result.append(", ");
          }
        }
      }
      result.append(".");
      return result.toString();
    }
  }

  /**
   * 基于桥接词生成新的文本。
   *
   * <p>该方法会分析输入文本中的相邻单词，尝试在它们之间插入桥接词（如果存在）。
   * 生成的文本保持原单词的大小写和顺序，桥接词则统一小写插入（如果存在多个桥接词，则随机选取一个插入）。.
   *
   * @param inputText 原始输入文本，可以包含大小写和非字母字符，也可以是空字符串
   * @return 插入桥接词后的新文本；如果输入非空且不含有效单词，则返回提示信息
   */
  public String generateNewText(String inputText) {
    String[] words = inputText.toLowerCase().split("[^a-zA-Z]+");
    String[] originalWords = inputText.split("[^a-zA-Z]+"); //没有小写的原始单词
    if (words.length < 1) {
      return "输入文本似乎不包含任何单词!";
    }
    StringBuilder newText = new StringBuilder();

    for (int i = 0; i < words.length - 1; i++) {
      String current = words[i];
      String next = words[i + 1];

      newText.append(originalWords[i]).append(" ");

      if (graph.containsKey(current) && graph.containsKey(next)) {
        Set<String> bridges = bridgeWords(current, next);
        // 如果有桥接词，则随机选择一个
        if (!bridges.isEmpty()) {
          String bridge = new ArrayList<>(bridges).get(random.nextInt(bridges.size()));
          newText.append(bridge).append(" ");
        }
      }
    }
    newText.append(originalWords[words.length - 1]);

    return newText.toString();
  }

  /**
   * 计算指定起始单词到目标单词（或所有节点）的最短路径，并返回详细路径描述。
   *
   * <p>该方法使用 Dijkstra 算法在词图中计算最短路径。若 {@code word2} 为空，
   * 则计算起始单词到图中所有其他单词的最短路径并输出；否则只计算起始单词到目标单词的路径。
   *
   * <p>当 {@code showPathOnGraph} 标志为 {@code true} 时，将使用图形界面显示所有最短路径，
   * 路径上的边和节点会被不同颜色高亮。.
   *
   * @param word1 起始单词，必须存在于词图中
   * @param word2 目标单词，若为 {@code null} 则计算所有路径
   * @return 包含最短路径信息的字符串描述，如果起始或目标单词不存在，则返回错误提示
   */
  public String calcShortestPath(String word1, String word2) {
    if (!graph.containsKey(word1)) {
      return "起始单词 \"" + word1 + "\" 不在图中!";
    }
    if (word2 != null && !graph.containsKey(word2)) {
      return "目标单词 \"" + word2 + "\" 不在图中!";
    }
    Map<String, Integer> distances = new HashMap<>(); // 存储最短距离
    Map<String, List<String>> previous = new HashMap<>(); // 存储前驱节点
    PriorityQueue<String> queue = new PriorityQueue<>(
        Comparator.comparingInt(distances::get)); // 优先队列

    // 初始化
    for (String node : graph.keySet()) {
      if (node.equals(word1)) {
        distances.put(node, 0); // 起始节点距离为0
      } else {
        distances.put(node, Integer.MAX_VALUE); // 其他节点距离为无穷大
      }
      queue.add(node);
    }

    // Dijkstra算法
    while (!queue.isEmpty()) {
      String current = queue.poll();
      if (distances.getOrDefault(current, Integer.MAX_VALUE) == Integer.MAX_VALUE) {
        break;
      }

      for (Map.Entry<String, Integer> neighbor : graph.getOrDefault(current, Collections.emptyMap())
          .entrySet()) {
        String next = neighbor.getKey();
        int newDist = distances.get(current) + neighbor.getValue();

        if (newDist < distances.getOrDefault(next, Integer.MAX_VALUE)) {
          distances.put(next, newDist);
          previous.put(next, new ArrayList<>(List.of(current)));
          queue.remove(next);
          queue.add(next);
        } else if (newDist == distances.get(next)) {
          previous.get(next).add(current);
        }
      }
    }

    // 输出所有路径
    StringBuilder result = new StringBuilder();
    List<List<String>> allPaths = new ArrayList<>();
    if (word2 == null) {
      for (String node : graph.keySet()) {
        if (!node.equals(word1)) {
          List<List<String>> paths = new ArrayList<>();
          findPaths(node, word1, previous, new ArrayList<>(), paths);
          if (paths.isEmpty()) {
            result.append("从 ").append(word1).append(" 到 ").append(node).append(" 没有路径\n");
          } else {
            allPaths.addAll(paths); //添加进总表
            result.append("从 ").append(word1).append(" 到 ").append(node)
                .append(" 的所有最短路径:\n");
            for (List<String> path : paths) {
              result.append("Path ").append(allPaths.indexOf(path) + 1).append(": ");
              result.append(String.join(" -> ", path)).append(" (距离: ")
                  .append(distances.get(node)).append(")\n");
            }
          }
        }
      }
    } else {
      findPaths(word2, word1, previous, new ArrayList<>(), allPaths);
      if (allPaths.isEmpty()) {
        result.append("没有从 ").append(word1).append(" 到 ").append(word2).append(" 的路径。");
      } else {
        result.append("从 ").append(word1).append(" 到 ").append(word2)
            .append(" 的所有最短路径:\n");
        for (List<String> path : allPaths) {
          result.append("Path ").append(allPaths.indexOf(path) + 1).append(": ");
          result.append(String.join(" -> ", path)).append(" (距离: ").append(distances.get(word2))
              .append(")\n");
        }
      }
    }
    if (showPathOnGraph) {
      // 在图上显示路径
      MutableGraph g = genGraph();
      List<guru.nidi.graphviz.attribute.Color> colors = colorChooser(allPaths.size());
      for (int i = 0; i < allPaths.size(); i++) {
        List<String> path = allPaths.get(i);
        for (int j = 0; j < path.size() - 1; j++) {
          String from = path.get(j);
          String to = path.get(j + 1);
          // 添加边并设置颜色 字的颜色和边的颜色相同
          g.add(node(from).link(to(node(to)).with(
              Label.of("(" + distances.get(path.getLast()) + ")" + "Path " + (i + 1)),
              colors.get(i))));

        }
      }
      displayGraph(g);
    }
    return result.toString();
  }

  private static void findPaths(String current, String start, Map<String, List<String>> previous,
      List<String> path, List<List<String>> allPaths) {
    if (current.equals(start)) {
      List<String> fullPath = new ArrayList<>(path);
      fullPath.add(start);
      Collections.reverse(fullPath);
      allPaths.add(fullPath);
      return;
    }
    if (!previous.containsKey(current)) {
      return;
    }

    path.add(current);
    for (String prev : previous.get(current)) {
      findPaths(prev, start, previous, path, allPaths);
    }
    path.removeLast();
  }

  /**
   * 计算指定单词的 PageRank 值，基于词图结构和可选的 IDF 加权。
   *
   * <p>PageRank 迭代次数固定为 10 次。
   * <br>当 {@code idf} 标志为 {@code true} 时，初始权重基于单词的逆文档频率（IDF）进行归一化，
   * 否则所有单词初始权重均等。
   *
   * <p>算法考虑悬挂节点（无出边节点）对 PageRank 的贡献，并采用阻尼因子 0.85。.
   *
   * @param word 需要计算 PageRank 的单词
   * @return 该单词的 PageRank 值，若单词不在词图中则返回 0.0
   */
  public Double calPageRank(String word) {
    if (!graph.containsKey(word)) {
      return 0.0;
    }

    // 初始化PageRank值
    double initialRank = 1.0 / graph.size();
    double coefficient = 1.0;
    if (idf) {
      double totalIdfValue = 0.0;
      for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
        double idfValue = Math.log((double) wordNum / (entry.getValue() + 1));
        totalIdfValue += idfValue;
      }
      coefficient = 1.0 / totalIdfValue; //归一化
    }
    Map<String, Double> currentRank = new HashMap<>();
    for (String node : graph.keySet()) {
      if (idf) {
        double idfValue = Math.log((double) wordNum / (wordCount.get(node) + 1)) * coefficient;
        currentRank.put(node, idfValue);
      } else {
        currentRank.put(node, initialRank);
      }
    }

    // 迭代计算PageRank (10次迭代)
    for (int i = 0; i < 10; i++) {
      Map<String, Double> nextRank = new HashMap<>();
      double danglingRank = 0.0;

      // 计算悬挂节点的贡献
      for (Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
        String node = entry.getKey();
        if (entry.getValue().isEmpty()) {
          danglingRank += currentRank.get(node) / graph.size();
        }
      }

      // 计算每个节点的PageRank
      for (String node : graph.keySet()) {
        double rank = danglingRank + (1 - 0.85) / graph.size();

        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
          if (entry.getValue().containsKey(node)) {
            rank += 0.85 * currentRank.get(entry.getKey()) / entry.getValue().size();
          }
        }

        nextRank.put(node, rank);
      }

      currentRank = nextRank;
    }
    //Print sorted data. FOR DEBUG
    //List<Map.Entry<String, Double>> sortedRank = new ArrayList<>(currentRank.entrySet());
    //sortedRank.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
    //for (Map.Entry<String, Double> entry : sortedRank)
    //{
    //    System.out.printf("%-15s : %.4f%n", entry.getKey(), entry.getValue());
    //}
    return currentRank.get(word);
  }

  /**
   * 在词图上执行随机游走，沿着边随机访问相邻节点，直到游走停止条件触发。
   *
   * <p>游走从图中随机选取一个起始节点开始，依次随机选择当前节点的一个邻居作为下一个节点，
   * 并记录路径和访问过的边。若遇到无邻居的节点或访问过的边（形成环路），则停止游走。
   *
   * <p>游走过程会实时将访问的节点写入名为 {@code walk_log.txt} 的日志文件，若遇异常会返回错误信息。
   * 支持在游走过程中通过中断线程主动停止，且会将中断信息写入日志。
   *
   * <p>当 {@code walkDelay} 为 {@code true} 时，游走每步之间会暂停300毫秒，便于可视化等用途。.
   *
   * @return 返回游走路径，节点间用 " -> " 连接；异常或停止时返回相应提示信息。
   */
  public String randomWalks() {
    if (graph.isEmpty()) {
      return "图为空，无法进行随机游走！";
    }

    String currentNode = graph.keySet().toArray(new String[0])[random.nextInt(graph.size())];
    walkPath.clear();
    visitedEdges.clear();
    walkStopped = false;
    boolean passiveStop = true;
    BufferedWriter writer = null;
    try {
      Path logPath = Paths.get("walk_log.txt");
      writer = Files.newBufferedWriter(
          logPath,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,      // 文件不存在则创建
          StandardOpenOption.APPEND);       // 追加模式（等效于 FileWriter 的 true）
      while (!walkStopped && !Thread.interrupted()) {
        walkPath.add(currentNode);
        //every time a word is added, put it into a file
        writer.write(currentNode + " ");
        writer.flush();
        List<String> neighbors = new ArrayList<>(graph.get(currentNode).keySet());
        if (neighbors.isEmpty()) {
          writer.write("[END-NO NEIGHBORS]\n");
          writer.flush();
          walkStopped = true;
          passiveStop = false; // 主动停止
          break;
        }

        String nextNode = neighbors.get(random.nextInt(neighbors.size()));
        String edge = currentNode + "->" + nextNode;
        if (visitedEdges.contains(edge)) {
          writer.write("[END-CYCLE]\n");
          writer.flush();
          walkStopped = true;
          passiveStop = false; // 主动停止
        } else {
          visitedEdges.add(edge);
          currentNode = nextNode;
        }
        if (walkDelay) {
          Thread.sleep(300);
        }
      }
    } catch (InterruptedException e) {
      try {
        if (passiveStop) {
          // 被中断时写入特殊标记
          writer.write("[INTERRUPTED]\n");
          writer.flush();
        }
        return "游走被中断：" + String.join(" -> ", walkPath);
      } catch (IOException ioException) {
        return "写入中断标记失败：" + ioException.getMessage();
      }
    } catch (IOException e) {
      return "写入日志文件失败：" + e.getMessage();
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return String.join(" -> ", walkPath);
  }

  /**
   * 根据当前的词图数据生成有向图的可视化图形文件，并在界面中显示图形。
   *
   * <p>方法首先判断图是否为空，若为空则在输出区域提示无法生成图形文件。
   * 否则调用 {@link #genGraph()} 生成可变图对象， 并通过 Graphviz 的 DOT 引擎渲染为 PNG 格式图像文件，文件名取自传入参数
   * {@code filename} 的文件名部分。
   *
   * <p>生成成功后，会在文本输出区域打印提示信息，并调用 {@link #displayGraph(MutableGraph)} 显示生成的图形。
   * 如果生成过程抛出异常，则会捕获并在输出区域显示错误信息。.
   *
   * @param filename 指定生成图形文件的路径或文件名（仅文件名部分被使用）
   */
  public void showDirectedGraph(String filename) {
    if (graph.isEmpty()) {
      outputArea.append("图为空，无法生成图形文件！\n");
      return;
    }
    try {
      MutableGraph g = genGraph();

      //渲染并保存图形
      Graphviz.fromGraph(g)
          .engine(Engine.DOT)
          .render(Format.PNG)
          .toFile(new File(FilenameUtils.getName(filename)));

      outputArea.append("图形文件已保存为 " + filename + "\n");
      displayGraph(g);
    } catch (Exception e) {
      outputArea.append("生成图形文件失败: " + e.getMessage() + "\n");
    }
  }

  /**
   * 将给定的图对象渲染为 SVG 格式，并显示在界面上的 SVG 画布组件中。
   *
   * <p>具体步骤包括：
   * <ul>
   *   <li>使用 Graphviz 的 DOT 引擎，将传入的 {@link MutableGraph} 对象渲染成 SVG 格式的字符串。</li>
   *   <li>通过 Apache Batik 的 {@link SAXSVGDocumentFactory} 解析 SVG 字符串，生成可显示的 SVG 文档对象。</li>
   *   <li>将解析后的 SVG 文档设置到界面中的 {@code svgCanvas} 组件，实现图形的可视化显示。</li>
   * </ul>
   *
   * <p>如果渲染或解析过程中发生异常，会捕获并打印堆栈跟踪，避免程序崩溃。.
   *
   * @param g 要显示的图形对象，必须是可变图类型 {@link MutableGraph}。
   */
  public void displayGraph(MutableGraph g) {
    //在内存中调用Graphviz重绘图片
    String svgContent = Graphviz.fromGraph(g)
        .engine(Engine.DOT)
        .render(Format.SVG)
        .toString();
    //将图片显示到canvas上
    try {
      String parser = XMLResourceDescriptor.getXMLParserClassName();
      SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
      Reader stringReader = new StringReader(svgContent);
      Document document = factory.createDocument(null, stringReader);
      svgCanvas.setDocument(document);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 生成一组颜色列表，用于图中不同路径的区分显示。
   *
   * <p>颜色均匀分布在色相环上，保持固定的饱和度和亮度，确保颜色鲜艳且易于辨识。.
   *
   * @param numColors 需要生成的颜色数量
   * @return 按照输入数量生成的颜色列表，类型为 {@link guru.nidi.graphviz.attribute.Color}
   */
  public List<guru.nidi.graphviz.attribute.Color> colorChooser(int numColors) {
    float saturation = 0.7f;  // 色彩饱和度 (0.0 - 1.0)
    float brightness = 0.9f;  // 明亮度 (0.0 - 1.0)

    List<guru.nidi.graphviz.attribute.Color> colors = new ArrayList<>();
    for (int i = 0; i < numColors; i++) {
      float hue = (float) i / numColors;  // 均匀分布在色相环
      Color color = Color.getHSBColor(hue, saturation, brightness);
      colors.add(guru.nidi.graphviz.attribute.Color.rgb(color.getRed(), color.getGreen(),
          color.getBlue()));
    }
    return colors;
  }

  /**
   * 生成当前文本图的有向图表示，使用 Graphviz 的 MutableGraph 类型。
   *
   * <p>该方法会：
   * <ul>
   *   <li>创建一个有向图对象，命名为“文本有向图”。</li>
   *   <li>将图中所有单词作为节点添加进去，节点形状为椭圆。</li>
   *   <li>将所有单词之间的关系（边）添加到图中，边上标注权重（出现次数）。</li>
   * </ul>.
   *
   * @return 构建好的可变有向图对象，方便后续渲染和展示
   */
  public MutableGraph genGraph() {
    // 使用Graphviz库创建图形
    MutableGraph g = mutGraph("文本有向图").setDirected(true);

    // 添加所有节点
    Map<String, Node> nodes = new HashMap<>();
    for (String nodeName : graph.keySet()) {
      nodes.put(nodeName, node(nodeName).with(Shape.ELLIPSE));
    }

    // 添加所有边
    for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
      String from = entry.getKey();
      for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
        String to = edge.getKey();
        int weight = edge.getValue();
        nodes.putIfAbsent(to, node(to).with(Shape.ELLIPSE));
        g.add(nodes.get(from).link(to(nodes.get(to)).with(Label.of(String.valueOf(weight)))));
      }
    }
    return g;
  }
}
