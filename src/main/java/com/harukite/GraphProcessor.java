package com.harukite;

import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static guru.nidi.graphviz.model.Factory.*;

public class GraphProcessor extends JFrame
{
    private Map<String, Map<String, Integer>> graph;
    private Random random;
    private Set<String> visitedEdges;
    private List<String> walkPath;
    private Map<String, Double> pageRank;
    private boolean walkStopped;

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
    private JLabel imageLabel;
    private JScrollPane imagePane;

    public GraphProcessor()
    {
        graph = new HashMap<>();
        pageRank = new HashMap<>();
        random = new Random();
        visitedEdges = new HashSet<>();
        walkPath = new ArrayList<>();
        walkStopped = true;

        initializeUI();
    }

    private static String readFile(String filePath) throws IOException
    {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null)
        {
            content.append(line).append(" ");
        }
        reader.close();
        return content.toString();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(GraphProcessor::new);
    }

    private void initializeUI()
    {
        setTitle("文本图处理器");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 顶部面板 - 文件选择和基本操作
        JPanel topPanel = new JPanel(new GridLayout(2, 1));

        // 文件选择面板
        JPanel filePanel = new JPanel(new BorderLayout());
        filePathField = new JTextField();
        JButton browseButton = new JButton("浏览");
        browseButton.addActionListener(e -> browseFile());
        JButton loadButton = new JButton("加载文件");
        loadButton.addActionListener(e -> loadFile());

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

        topPanel.add(filePanel);
        topPanel.add(graphPanel);

        // 中间面板 - 功能区和输出区
        JPanel centerPanel = new JPanel(new GridLayout(1, 2));

        // 功能区
        JPanel functionPanel = new JPanel(new GridLayout(6, 1));

        // 1. 桥接词查询
        JPanel bridgePanel = new JPanel(new BorderLayout());
        word1Field = new JTextField();
        word2Field = new JTextField();
        JButton bridgeButton = new JButton("查询桥接词");
        bridgeButton.addActionListener(e -> queryBridgeWords());

        JPanel bridgeInputPanel = new JPanel(new GridLayout(2, 2));
        bridgeInputPanel.add(new JLabel("单词1:"));
        bridgeInputPanel.add(word1Field);
        bridgeInputPanel.add(new JLabel("单词2:"));
        bridgeInputPanel.add(word2Field);

        bridgePanel.add(bridgeInputPanel, BorderLayout.CENTER);
        bridgePanel.add(bridgeButton, BorderLayout.SOUTH);

        // 2. 生成新文本
        JPanel newTextPanel = new JPanel(new BorderLayout());
        inputTextField = new JTextField();
        JButton newTextButton = new JButton("生成新文本");
        newTextButton.addActionListener(e -> generateNewText());

        newTextPanel.add(new JLabel("输入文本:"), BorderLayout.NORTH);
        newTextPanel.add(inputTextField, BorderLayout.CENTER);
        newTextPanel.add(newTextButton, BorderLayout.SOUTH);

        // 3. 最短路径
        JPanel pathPanel = new JPanel(new BorderLayout());
        startWordField = new JTextField();
        endWordField = new JTextField();
        JButton pathButton = new JButton("计算最短路径");
        pathButton.addActionListener(e -> calcShortestPath());

        JPanel pathInputPanel = new JPanel(new GridLayout(2, 2));
        pathInputPanel.add(new JLabel("起始单词:"));
        pathInputPanel.add(startWordField);
        pathInputPanel.add(new JLabel("目标单词:"));
        pathInputPanel.add(endWordField);

        pathPanel.add(pathInputPanel, BorderLayout.CENTER);
        pathPanel.add(pathButton, BorderLayout.SOUTH);

        // 4. PageRank
        JPanel rankPanel = new JPanel(new BorderLayout());
        targetWordField = new JTextField();
        JButton rankButton = new JButton("计算PageRank");
        rankButton.addActionListener(e -> calPageRank());

        rankPanel.add(new JLabel("目标单词:"), BorderLayout.NORTH);
        rankPanel.add(targetWordField, BorderLayout.CENTER);
        rankPanel.add(rankButton, BorderLayout.SOUTH);

        // 5. 随机游走
        JButton walkButton = new JButton("随机游走");
        walkButton.addActionListener(e -> randomWalk());

        // 添加所有功能组件
        functionPanel.add(bridgePanel);
        functionPanel.add(newTextPanel);
        functionPanel.add(pathPanel);
        functionPanel.add(rankPanel);
        functionPanel.add(walkButton);

        // 6. 显示图形
        imageLabel = new JLabel();
        imagePane = new JScrollPane(imageLabel);

        // 输出区
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JPanel leftPanel = new JPanel(new GridLayout(2, 1));

        leftPanel.add(imagePane);
        leftPanel.add(scrollPane);

        centerPanel.add(functionPanel);
        centerPanel.add(leftPanel);

        // 添加到主窗口
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private void browseFile()
    {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void loadFile()
    {
        String filePath = filePathField.getText();
        if (filePath.isEmpty())
        {
            outputArea.append("请先选择文件!\n");
            return;
        }

        try
        {
            graph = new HashMap<>();
            String text = readFile(filePath);
            buildGraph(text);
            outputArea.append("图构建完成!\n");
        }
        catch (IOException e)
        {
            outputArea.append("读取文件失败: " + e.getMessage() + "\n");
        }
    }

    private void queryBridgeWords()
    {
        if (word1Field.getText().isEmpty() || word2Field.getText().isEmpty())
        {
            outputArea.append("请填写两个单词!\n");
            return;
        }
        String word1 = word1Field.getText().toLowerCase();
        String word2 = word2Field.getText().toLowerCase();
        String result = queryBridgeWords(word1, word2);
        outputArea.append(result + "\n");
    }

    private void generateNewText()
    {
        if (inputTextField.getText().isEmpty())
        {
            outputArea.append("请输入文本!\n");
            return;
        }
        String inputText = inputTextField.getText();
        String result = generateNewText(inputText);
        outputArea.append("生成的新文本: " + result + "\n");
    }

    private void calcShortestPath()
    {
        if (startWordField.getText().isEmpty())
        {
            outputArea.append("请填写起始单词!\n");
            return;
        }
        String word1 = startWordField.getText().toLowerCase();
        String word2 = endWordField.getText().toLowerCase();
        String result = calcShortestPath(word1, word2.isEmpty() ? null : word2);
        outputArea.append(result + "\n");
    }

    private void calPageRank()
    {
        String word = targetWordField.getText().toLowerCase();
        Double result = calPageRank(word);
        outputArea.append(String.format("单词 '%s' 的PageRank值为: %.6f\n", word, result));
    }

    private void randomWalk()
    {
        if (walkStopped == false)
        {
            walkStopped = true;
            outputArea.append("随机游走中断\n");
            return;
        }
        String result = randomWalks();
        outputArea.append("随机游走路径: " + result + "\n");
    }

    public void buildGraph(String text)
    {
        String[] words = text.toLowerCase().split("[^a-zA-Z]+");
        for (int i = 0; i < words.length - 1; i++)
        {
            String current = words[i];
            String next = words[i + 1];

            if (current.isEmpty() || next.isEmpty())
            {
                continue;
            }

            graph.putIfAbsent(current, new HashMap<>());
            graph.get(current).merge(next, 1, Integer::sum);
        }
    }

    public Set<String> BridgeWords(String word1, String word2)
    {
        Set<String> bridges = new HashSet<>();
        Map<String, Integer> word1Neighbors = graph.get(word1);

        for (String neighbor : word1Neighbors.keySet())
        {
            if (graph.containsKey(neighbor))
            {
                Map<String, Integer> neighborNeighbors = graph.get(neighbor);
                if (neighborNeighbors.containsKey(word2))
                {
                    bridges.add(neighbor);
                }
            }
        }
        return bridges;
    }

    public String queryBridgeWords(String word1, String word2)
    {
        if (!graph.containsKey(word1) || !graph.containsKey(word2))
        {
            return "No \"" + word1 + "\" or \"" + word2 + "\" in the graph!";
        }

        Set<String> bridges = BridgeWords(word1, word2);

        if (bridges.isEmpty())
        {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        }
        else
        {
            StringBuilder result = new StringBuilder("The bridge words from \"" + word1 + "\" to \"" + word2 + "\" ");
            int i = bridges.size();
            if (i > 1)
            {
                result.append("are: ");
            }
            else
            {
                result.append("is: ");
            }
            for (String bridge : bridges)
            {
                result.append("\"").append(bridge).append("\"");
                if (--i > 0) //如果是最后一个，加and
                {
                    if (i == 1)
                    {
                        result.append(" and ");
                    }
                    else
                    {
                        result.append(", ");
                    }
                }
            }
            result.append(".");
            return result.toString();
        }
    }

    public String generateNewText(String inputText)
    {
        String[] words = inputText.toLowerCase().split("[^a-zA-Z]+");
        StringBuilder newText = new StringBuilder();

        for (int i = 0; i < words.length - 1; i++)
        {
            String current = words[i];
            String next = words[i + 1];

            newText.append(current).append(" ");

            if (graph.containsKey(current) && graph.containsKey(next))
            {
                Set<String> bridges = BridgeWords(current, next);
                // 如果有桥接词，则随机选择一个
                if (!bridges.isEmpty())
                {
                    String bridge = new ArrayList<>(bridges).get(random.nextInt(bridges.size()));
                    newText.append(bridge).append(" ");
                }
            }
        }
        newText.append(words[words.length - 1]);

        return newText.toString();
    }

    public String calcShortestPath(String word1, String word2)
    {
        if (!graph.containsKey(word1))
        {
            return "起始单词 \"" + word1 + "\" 不在图中!";
        }
        if (word2 != null && !graph.containsKey(word2))
        {
            return "目标单词 \"" + word2 + "\" 不在图中!";
        }
        Map<String, Integer> distances = new HashMap<>();
        Map<String, List<String>> previous = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        // 初始化
        for (String node : graph.keySet())
        {
            if (node.equals(word1))
            {
                distances.put(node, 0);
            }
            else
            {
                distances.put(node, Integer.MAX_VALUE);
            }
            queue.add(node);
        }

        // Dijkstra算法
        while (!queue.isEmpty())
        {
            String current = queue.poll();
            if (distances.getOrDefault(current, Integer.MAX_VALUE) == Integer.MAX_VALUE)
            {
                break;
            }

            for (Map.Entry<String, Integer> neighbor : graph.getOrDefault(current, Collections.emptyMap()).entrySet())
            {
                String next = neighbor.getKey();
                int newDist = distances.get(current) + neighbor.getValue();

                if (newDist < distances.getOrDefault(next, Integer.MAX_VALUE))
                {
                    distances.put(next, newDist);
                    previous.put(next, new ArrayList<>(List.of(current)));
                    queue.remove(next);
                    queue.add(next);
                }
                else if (newDist == distances.get(next))
                {
                    previous.get(next).add(current);
                }
            }
        }

        // 输出所有路径
        StringBuilder result = new StringBuilder();
        if (word2 == null)
        {
            for (String node : graph.keySet())
            {
                if (!node.equals(word1))
                {
                    List<List<String>> paths = new ArrayList<>();
                    findPaths(node, word1, previous, new ArrayList<>(), paths);

                    if (paths.isEmpty())
                    {
                        result.append("从 ").append(word1).append(" 到 ").append(node).append(" 没有路径\n");
                    }
                    else
                    {
                        result.append("从 ").append(word1).append(" 到 ").append(node).append(" 的所有最短路径:\n");
                        for (List<String> path : paths)
                        {
                            result.append(String.join(" -> ", path)).append(" (距离: ").append(distances.get(node)).append(")\n");
                        }
                    }
                }
            }
        }
        else
        {
            List<List<String>> allPaths = new ArrayList<>();
            findPaths(word2, word1, previous, new ArrayList<>(), allPaths);
            if (allPaths.isEmpty())
            {
                result.append("没有从 ").append(word1).append(" 到 ").append(word2).append(" 的路径。");
            }
            else
            {
                result.append("从 ").append(word1).append(" 到 ").append(word2).append(" 的所有最短路径:\n");
                for (List<String> path : allPaths)
                {
                    result.append(String.join(" -> ", path)).append(" (距离: ").append(distances.get(word2)).append(")\n");
                }
            }
        }

        return result.toString();
    }

    private static void findPaths(String current, String start, Map<String, List<String>> previous,
                                  List<String> path, List<List<String>> allPaths)
    {
        if (current.equals(start))
        {
            List<String> fullPath = new ArrayList<>(path);
            fullPath.add(start);
            Collections.reverse(fullPath);
            allPaths.add(fullPath);
            return;
        }
        if (!previous.containsKey(current))
        {
            return;
        }

        path.add(current);
        for (String prev : previous.get(current))
        {
            findPaths(prev, start, previous, path, allPaths);
        }
        path.remove(path.size() - 1);
    }

    public Double calPageRank(String word)
    {
        if (!graph.containsKey(word))
        {
            return 0.0;
        }

        // 初始化PageRank值
        double initialRank = 1.0 / graph.size();
        Map<String, Double> currentRank = new HashMap<>();
        for (String node : graph.keySet())
        {
            currentRank.put(node, initialRank);
        }

        // 迭代计算PageRank (10次迭代)
        for (int i = 0; i < 10; i++)
        {
            Map<String, Double> nextRank = new HashMap<>();
            double danglingRank = 0.0;

            // 计算悬挂节点的贡献
            for (String node : graph.keySet())
            {
                if (graph.get(node).isEmpty())
                {
                    danglingRank += currentRank.get(node) / graph.size();
                }
            }

            // 计算每个节点的PageRank
            for (String node : graph.keySet())
            {
                double rank = danglingRank + (1 - 0.85) / graph.size();

                for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet())
                {
                    if (entry.getValue().containsKey(node))
                    {
                        rank += 0.85 * currentRank.get(entry.getKey()) / entry.getValue().size();
                    }
                }

                nextRank.put(node, rank);
            }

            currentRank = nextRank;
        }

        pageRank = currentRank;
        return currentRank.get(word);
    }

    public String randomWalks()
    {
        if (graph.isEmpty())
        {
            return "图为空，无法进行随机游走！";
        }

        String currentNode = graph.keySet().toArray(new String[0])[random.nextInt(graph.size())];
        walkPath.clear();
        visitedEdges.clear();
        walkStopped = false;

        while (!walkStopped)
        {
            walkPath.add(currentNode);
            List<String> neighbors = new ArrayList<>(graph.get(currentNode).keySet());
            if (neighbors.isEmpty())
            {
                break;
            }

            String nextNode = neighbors.get(random.nextInt(neighbors.size()));
            String edge = currentNode + "->" + nextNode;
            if (visitedEdges.contains(edge))
            {
                walkStopped = true;
            }
            else
            {
                visitedEdges.add(edge);
                currentNode = nextNode;
            }
        }

        return String.join(" -> ", walkPath);
    }

    public void showDirectedGraph(String filename)
    {
        if (graph.isEmpty())
        {
            outputArea.append("图为空，无法生成图形文件！\n");
            return;
        }
        try
        {
            // 使用Graphviz库创建图形
            MutableGraph g = mutGraph("文本有向图").setDirected(true);

            // 添加所有节点
            Map<String, Node> nodes = new HashMap<>();
            for (String nodeName : graph.keySet())
            {
                nodes.put(nodeName, node(nodeName).with(Shape.ELLIPSE));
            }

            // 添加所有边
            for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet())
            {
                String from = entry.getKey();
                for (Map.Entry<String, Integer> edge : entry.getValue().entrySet())
                {
                    String to = edge.getKey();
                    int weight = edge.getValue();
                    nodes.putIfAbsent(to, node(to).with(Shape.ELLIPSE));
                    g.add(nodes.get(from).link(to(nodes.get(to)).with(Label.of(String.valueOf(weight)))));
                }
            }

            // 渲染并保存图形
            Graphviz.fromGraph(g)
                    .engine(Engine.DOT)
                    .width(1200)
                    .render(Format.PNG)
                    .toFile(new File(filename));

            outputArea.append("图形文件已保存为 " + filename + "\n");

            // 显示图形到JLabel
            //获取JLabel的长宽
            Dimension size = imagePane.getSize();
            //在内存中调用Graphviz重绘图片
            BufferedImage image = Graphviz.fromGraph(g)
                    .engine(Engine.DOT)
                    .width(size.width)
                    .render(Format.PNG)
                    .toImage();
            //将图片显示到JLabel上
            imageLabel.setIcon(new ImageIcon(image));
            imageLabel.repaint();
        }
        catch (Exception e)
        {
            outputArea.append("生成图形文件失败: " + e.getMessage() + "\n");
        }
    }
}