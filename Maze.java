import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.*;
import tester.Tester;

import java.awt.*;
import java.util.*;

//Describes the world
class MazeWorld extends World {
  static final int boxSize = 30;
  int xCoordinate;
  int yCoordinate;
  User user;
  boolean done;
  Vertex stop;
  ArrayList<Vertex> makePath = new ArrayList<Vertex>();
  ArrayList<Edge> edgeList = new ArrayList<Edge>();
  ArrayList<Edge> treeSpan = new ArrayList<Edge>();
  HashMap<Vertex, Vertex> map = new HashMap<Vertex, Vertex>();
  WorldScene scene = new WorldScene(0, 0);
  ArrayList<ArrayList<Vertex>> board;

  TextImage winner = new TextImage("You winner!", 30, Color.BLACK);
  double tickRate = 0.01;

  // constructor
  MazeWorld() {
    this.xCoordinate = 2;
    this.yCoordinate = 3;
    this.edgeList = new ArrayList<Edge>(
        Arrays.asList(new Edge(new Vertex(0, 0), new Vertex(1, 0), 1),
            new Edge(new Vertex(0, 0), new Vertex(0, 1), 2),
            new Edge(new Vertex(1, 0), new Vertex(1, 1), 3),
            new Edge(new Vertex(0, 1), new Vertex(1, 1), 4),
            new Edge(new Vertex(0, 1), new Vertex(0, 2), 5),
            new Edge(new Vertex(1, 1), new Vertex(1, 2), 6),
            new Edge(new Vertex(0, 2), new Vertex(1, 2), 7)));
    this.treeSpan = new ArrayList<Edge>(
        Arrays.asList(new Edge(new Vertex(0, 0), new Vertex(1, 0), 1),
            new Edge(new Vertex(0, 0), new Vertex(0, 1), 2),
            new Edge(new Vertex(1, 0), new Vertex(1, 1), 3),
            new Edge(new Vertex(0, 1), new Vertex(0, 2), 5),
            new Edge(new Vertex(1, 1), new Vertex(1, 2), 6)));
    this.board = this.makeGrid(2, 3, "test");
    this.board.get(0).get(0).moveBottomMaker = false;
    this.board.get(0).get(1).moveBottomMaker = false;
    this.board.get(1).get(0).moveBottomMaker = false;
    this.board.get(1).get(1).moveBottomMaker = false;
    this.board.get(2).get(0).moveBottomMaker = true;
    this.board.get(2).get(1).moveBottomMaker = true;
    this.board.get(0).get(0).moveRightMaker = false;
    this.board.get(0).get(1).moveRightMaker = true;
    this.board.get(1).get(0).moveRightMaker = true;
    this.board.get(1).get(1).moveRightMaker = true;
    this.board.get(2).get(0).moveRightMaker = true;
    this.board.get(2).get(1).moveRightMaker = true;
    this.map.put(this.board.get(0).get(0), this.board.get(0).get(0));
    this.map.put(this.board.get(0).get(1), this.board.get(0).get(1));
    this.map.put(this.board.get(1).get(0), this.board.get(1).get(0));
    this.map.put(this.board.get(1).get(1), this.board.get(1).get(1));
    this.map.put(this.board.get(2).get(0), this.board.get(2).get(0));
    this.map.put(this.board.get(2).get(1), this.board.get(2).get(1));
    this.user = new User(this.board.get(0).get(0));
    this.done = false;
    this.makePath = new ArrayList<Vertex>();
    this.stop = this.board.get(2).get(1);
    this.worldMaker();
  }

  // constructor
  MazeWorld(int xCoordinate, int yCoordinate) {
    this.xCoordinate = xCoordinate;
    this.yCoordinate = yCoordinate;
    this.board = this.makeGrid(xCoordinate, yCoordinate);
    this.edgesMake(this.board);
    this.mapMaker(board);
    this.makeMST();
    this.user = new User(board.get(0).get(0));
    this.done = false;
    this.stop = this.board.get(yCoordinate - 1).get(xCoordinate - 1);
    this.worldMaker();
  }

  // makes the world
  WorldScene worldMaker() {
    // makes bottom right box
    int x = (xCoordinate - 1) * boxSize;
    int y = (yCoordinate - 1) * boxSize;
    this.scene.placeImageXY(board.get(this.yCoordinate - 1).get(this.xCoordinate - 1)
        .make(this.xCoordinate, this.yCoordinate, Color.PINK), x, y);

    // make the screen
    int i = 0;
    while (i < yCoordinate) {
      int j = 0;
      while (j < xCoordinate) {
        this.changemoveBottomMaker(this.board.get(i).get(j));
        this.changemoveRightMaker(this.board.get(i).get(j));
        if (this.board.get(i).get(j).distance) {
          this.scene.placeImageXY(
              board.get(i).get(j).make(this.xCoordinate, this.yCoordinate, Color.YELLOW),
              boxSize * j, boxSize * i);
        }
        WorldImage horizontalWall = board.get(i).get(j).rightEdgeMaker();

        if (board.get(i).get(j).moveRightMaker) {
          this.scene.placeImageXY(horizontalWall, MazeWorld.boxSize * j, MazeWorld.boxSize * i);
        }
        WorldImage verticalWall = board.get(i).get(j).bottomEdgeMaker();

        if (board.get(i).get(j).moveBottomMaker) {
          this.scene.placeImageXY(verticalWall, MazeWorld.boxSize * j, MazeWorld.boxSize * i);
        }
        j++;
      }
      i++;
    }

    // make the User
    int userX = this.user.tap.x * boxSize;
    int userY = this.user.tap.y * boxSize;
    this.scene.placeImageXY(user.makeUser(), userX, userY);
    return scene;
  }

  // changes game after ticks to update
  public WorldScene makeScene() {
    if (makePath.size() > 1) {
      this.endFinder();
    }
    else if (this.stop.previous != null && this.done) {
      this.backwards();
    }
    else if (makePath.size() > 0) {
      this.endMaker();
    }
    if (user.tap == this.board.get(yCoordinate - 1).get(xCoordinate - 1)) {
      this.scene.placeImageXY(winner, xCoordinate * boxSize / 2, yCoordinate * boxSize / 2);
    }
    return scene;
  }

  //OnKeyEvent when user presses keys
  public void onKeyEvent(String key) {
    switch (key) {
      case "r":
        this.scene = this.getEmptyScene();
        this.board = this.makeGrid(xCoordinate, yCoordinate);
        this.stop = this.board.get(this.yCoordinate - 1).get(this.xCoordinate - 1);
        this.user = new User(board.get(0).get(0));
        this.edgesMake(this.board);
        this.mapMaker(board);
        this.makeMST();
        this.worldMaker();
        break;
      case "up":
        if (user.moveCorrect("up")) {
          user.tap.distance = true;
          user.tap = user.tap.top;
        }
        break;
      case "down":
        if (user.moveCorrect("down")) {
          user.tap.distance = true;
          user.tap = user.tap.bottom;
        }
        break;
      case "left":
        if (user.moveCorrect("left")) {
          user.tap.distance = true;
          user.tap = user.tap.left;
        }
        break;
      case "right":
        if (user.moveCorrect("right")) {
          user.tap.distance = true;
          user.tap.distance = true;
          user.tap = user.tap.right;
        }
        break;
      case "d":
        this.stop = this.board.get(this.yCoordinate - 1).get(this.xCoordinate - 1);
        this.makePath = new Graph().makeDFS(this.board.get(0).get(0),
            this.board.get(this.yCoordinate - 1).get(this.xCoordinate - 1));
        break;
      case "b":
        this.stop = this.board.get(this.yCoordinate - 1).get(this.xCoordinate - 1);
        this.makePath = new Graph().makeBFS(this.board.get(0).get(0),
            this.board.get(this.yCoordinate - 1).get(this.xCoordinate - 1));
        break;
      default:
        break;
        
    }
    
    this.scene.placeImageXY(user.makeUser(), user.tap.x * boxSize, user.tap.y * boxSize);
    this.worldMaker();
  }

  // puts all the cells together
  void vertexCombine(ArrayList<ArrayList<Vertex>> b) {
    for (ArrayList<Vertex> row : b) {
      for (int j = 0; j < row.size(); j++) {
        Vertex v = row.get(j);
        if (j + 1 < row.size()) {
          v.right = row.get(j + 1);
        }
        if (j - 1 >= 0) {
          v.left = row.get(j - 1);
        }
        if (b.indexOf(row) + 1 < b.size()) {
          v.bottom = b.get(b.indexOf(row) + 1).get(j);
        }
        if (b.indexOf(row) - 1 >= 0) {
          v.top = b.get(b.indexOf(row) - 1).get(j);
        }
      }
    }
  }

  // make grid for every cell
  ArrayList<ArrayList<Vertex>> makeGrid(int w, int h, String a) {
    ArrayList<ArrayList<Vertex>> board = new ArrayList<>();
    for (int i = 0; i < h; i++) {
      ArrayList<Vertex> row = new ArrayList<>();
      for (int j = 0; j < w; j++) {
        row.add(new Vertex(j, i));
      }
      board.add(row);
    }
    this.vertexCombine(board);
    return board;
  }

  // makes scene for maze
  ArrayList<ArrayList<Vertex>> makeGrid(int w, int h) {
    ArrayList<ArrayList<Vertex>> board = new ArrayList<ArrayList<Vertex>>();
    board = new ArrayList<ArrayList<Vertex>>(h);
    for (int i = 0; i < h; i++) {
      ArrayList<Vertex> row = new ArrayList<Vertex>(w);
      for (int j = 0; j < w; j++) {
        row.add(new Vertex(j, i));
      }
      board.add(row);
    }
    this.vertexCombine(board);
    this.edgesMake(board);
    this.mapMaker(board);
    return board;
  }

  // moveBottomtMaker is changed based on the vertex
  void changemoveBottomMaker(Vertex v) {
    for (Edge edge : this.treeSpan) {
      if (edge.v1.equals(v) && edge.v2.x == edge.v1.x) {
        edge.v1.moveBottomMaker = false;
      }
    }
  }

  // moveRightMaker is changed based on the vertex
  void changemoveRightMaker(Vertex v) {
    for (Edge edge : this.treeSpan) {
      if (edge.v1.equals(v) && edge.v2.y == edge.v1.y) {
        edge.v1.moveRightMaker = false;
      }
    }
  }

  // makes edges
  ArrayList<Edge> edgesMake(ArrayList<ArrayList<Vertex>> n) {
    Random wRando = new Random();
    for (ArrayList<Vertex> row : n) {
      for (int j = 0; j < row.size(); j++) {
        Vertex currVertex = row.get(j);
        if (j < row.size() - 1) {
          Vertex rightVertex = row.get(j + 1);
          int weight = wRando.nextInt(50);
          Edge e1 = new Edge(currVertex, rightVertex, weight);
          edgeList.add(e1);
        }
        if (n.indexOf(row) < n.size() - 1) {
          Vertex bottomVertex = n.get(n.indexOf(row) + 1).get(j);
          int weight = wRando.nextInt(50);
          Edge e2 = new Edge(currVertex, bottomVertex, weight);
          edgeList.add(e2);
        }
      }
    }
    Collections.sort(edgeList, new CompareWeights());
    return edgeList;
  }

  // makes hashmap
  HashMap<Vertex, Vertex> mapMaker(ArrayList<ArrayList<Vertex>> vertex) {
    for (ArrayList<Vertex> row : vertex) {
      for (Vertex v : row) {
        this.map.put(v, v);
      }
    }
    return map;
  }

  // makes minimum spanning tree
  ArrayList<Edge> makeMST() {
    for (int i = 0; i < edgeList.size() && treeSpan.size() < edgeList.size(); i++) {
      Edge e = edgeList.get(i);
      if (lookFor(lookFor(e.v1)).equals(lookFor(lookFor(e.v2)))) {
        continue;
      }
      treeSpan.add(e);
      union(lookFor(e.v1), lookFor(e.v2));
    }
    // Adds all the edgesInTree for each vertex
    for (int y = 0; y < this.yCoordinate; y += 1) {
      for (int x = 0; x < this.xCoordinate; x += 1) {
        for (Edge e : this.treeSpan) {
          if (this.board.get(y).get(x).equals(e.v1) || this.board.get(y).get(x).equals(e.v2)) {
            this.board.get(y).get(x).edgesInTree.add(e);
          }
        }
      }
    }
    return this.treeSpan;
  }

  // puts the vertexs together
  void union(Vertex item, Vertex rep) {
    this.map.put(this.lookFor(item), this.lookFor(rep));
  }

  // looks for the representor of the vertex item
  Vertex lookFor(Vertex item) {
    if (item.equals(this.map.get(item))) {
      return item;
    }
    else {
      return this.lookFor(this.map.get(item));
    }
  }

  // sees when it ends
  void endFinder() {
    Vertex next = makePath.remove(0);

    Color imageColor = Color.BLUE;
    WorldImage image = next.make(this.xCoordinate, this.yCoordinate, imageColor);
    this.scene.placeImageXY(image, next.x * boxSize, next.y * boxSize);
  }

  // makes the last part of the maze when it ends
  void endMaker() {
    Vertex next = makePath.remove(0);
    this.scene.placeImageXY(next.make(this.xCoordinate, this.yCoordinate, Color.CYAN),
        next.x * boxSize, next.y * boxSize);

    if (this.stop.left.previous != null && !this.stop.left.moveRightMaker) {
      this.stop.previous = this.stop.left;
    }
    else if (this.stop.top.previous != null && !this.stop.top.moveBottomMaker) {
      this.stop.previous = this.stop.top;
    }
    else {
      this.stop.previous = next;
    }
    this.done = true;
  }

  // goes backwards to follow the path taken
  void backwards() {
    if (this.stop.x == this.xCoordinate && this.stop.y == this.yCoordinate) {
      this.scene.placeImageXY(this.stop.make(this.xCoordinate, this.yCoordinate, Color.magenta),
          this.stop.x * boxSize, this.stop.y * boxSize);
    }
    this.scene.placeImageXY(
        this.stop.previous.make(this.xCoordinate, this.yCoordinate, Color.magenta),
        this.stop.previous.x * boxSize, this.stop.previous.y * boxSize);
    if (this.stop.previous != null) {
      this.stop = this.stop.previous;
    }
    else {
      this.stop = new Vertex(this.xCoordinate, this.yCoordinate);
    }
  }
}

//Represents a User
class User {
  Vertex tap;

  // constructor
  User(Vertex tap) {
    this.tap = tap;
  }

  // checks if the move you made was valid
  boolean moveCorrect(String move) {
    switch (move) {
      case "up":
        return (this.tap.top != null) && !this.tap.top.moveBottomMaker;
  
      case "down":
        return (this.tap.bottom != null) && !this.tap.moveBottomMaker;
  
      case "left":
        return (this.tap.left != null) && !this.tap.left.moveRightMaker;
  
      case "right":
        return (this.tap.right != null) && !this.tap.moveRightMaker;
  
      default:
        return false;
    }
  }

  // Makes the User dot (starts in the top left)
  WorldImage makeUser() {
    int size = MazeWorld.boxSize;
    WorldImage greenRect = new RectangleImage(size - 4, size - 4, OutlineMode.SOLID, Color.GREEN)
        .movePinhole(-size / 2, -size / 2);
    return greenRect;
  }
}

//Represents an Edge
class Edge {
  Vertex v1;
  Vertex v2;
  int weight;

  Edge(Vertex v1, Vertex v2, int weight) {
    this.v1 = v1;
    this.v2 = v2;
    this.weight = weight;
  }
}

//checks the weight of the items
class CompareWeights implements Comparator<Edge> {
  // compares thing1 and thing2 by their weight
  public int compare(Edge thing2, Edge thing1) {
    return thing2.weight - thing1.weight;
  }
}

// An ICollection is one of
// - A Queue
// - A Stack
interface ICollection<T> {
  // Removes an item
  T remove();

  // Adds an item
  void add(T item);

  // Returns the size
  int size();
}

// Represents a Queue
class Queue<T> implements ICollection<T> {
  Deque<T> items;

  Queue() {
    this.items = new ArrayDeque<T>();
  }

  // Removes an item
  public T remove() {
    return this.items.removeFirst();
  }

  // Adds an item
  public void add(T item) {
    this.items.addLast(item);
  }

  // Returns the size
  public int size() {
    return this.items.size();
  }
}

// Represents a Stack
class Stack<T> implements ICollection<T> {
  Deque<T> items;

  Stack() {
    this.items = new ArrayDeque<T>();
  }

  // Removes and item to a Stack
  public T remove() {
    return this.items.removeFirst();
  }

  // Adds an item to a Stack
  public void add(T item) {
    this.items.addFirst(item);
  }

  // Returns the size of this Stack
  public int size() {
    return this.items.size();
  }
}

//Represents a Vertex
class Vertex {
  private static final int WallThickness = 2;
  Vertex top;
  Vertex bottom;
  Vertex left;
  Vertex right;
  int x;
  int y;
  Vertex previous;
  boolean distance;
  boolean moveRightMaker;
  boolean moveBottomMaker;
  ArrayList<Edge> edgesInTree = new ArrayList<Edge>();

  Vertex(int x, int y) {
    this.x = x;
    this.y = y;
    this.left = null;
    this.right = null;
    this.top = null;
    this.bottom = null;
    this.distance = false;
    this.previous = null;
    this.moveRightMaker = true;
    this.moveBottomMaker = true;
  }

  // makes rectangles
  WorldImage make(int x, int y, Color c) {
    return new RectangleImage(MazeWorld.boxSize - 2, MazeWorld.boxSize - 2, OutlineMode.SOLID, c)
        .movePinhole(-x * MazeWorld.boxSize / x / 2, -x * MazeWorld.boxSize / x / 2);
  }

  // makes bottom edge
  WorldImage bottomEdgeMaker() {
    return new RectangleImage(MazeWorld.boxSize, WallThickness, "solid", Color.green)
        .movePinhole(MazeWorld.boxSize / -2, -1 * MazeWorld.boxSize);
  }

  // makes right edge
  WorldImage rightEdgeMaker() {
    return new RectangleImage(WallThickness, MazeWorld.boxSize, "solid", Color.green)
        .movePinhole(-1 * MazeWorld.boxSize, MazeWorld.boxSize / -2);
  }

  // Looks for the cell that you were on before
  void previousGet() {
    if (this.top != null && !this.top.moveBottomMaker && this.top.previous == null) {
      this.previous = this.top;
    }
    else if (this.bottom != null && !this.moveBottomMaker && this.bottom.previous == null) {
      this.previous = this.bottom;
    }
    else if (this.left != null && !this.left.moveRightMaker && this.left.previous == null) {
      this.previous = this.left;
    }
    else if (this.right != null && !this.moveRightMaker && this.right.previous == null) {
      this.previous = this.right;
    }
  }
}

// Represents a graph
class Graph {
  ArrayList<Vertex> vertices;

  Graph() {
  }

  // lookFors the makePath using a Queue
  // Is an implementation of BFSD
  ArrayList<Vertex> makeBFS(Vertex previous, Vertex next1) {
    return this.createmakePath(previous, next1, new Queue<Vertex>());
  }

  // lookFors the makePath using a Stack
  // Is an implementation of DFS
  ArrayList<Vertex> makeDFS(Vertex previous, Vertex next1) {
    return this.createmakePath(previous, next1, new Stack<Vertex>());
  }

  // lookFors the makePath using an ICollection
  ArrayList<Vertex> createmakePath(Vertex previous, Vertex next1, ICollection<Vertex> loWork) {
    ArrayList<Vertex> makePath = new ArrayList<Vertex>();

    loWork.add(previous);
    while (loWork.size() > 0) {
      Vertex next = loWork.remove();
      if (next == next1) {
        return makePath;
      }
      
      else if (makePath.contains(next)) {
        // nothing to change
      }
      
      else {
        for (Edge e : next.edgesInTree) {
          loWork.add(e.v1);
          loWork.add(e.v2);

          if (makePath.contains(e.v1)) {
            next.previous = e.v1;
          }
          else if (makePath.contains(e.v2)) {
            next.previous = e.v2;
          }
        }
        makePath.add(next);
      }
      
    }
    return makePath;
  }
}

//Predicate
interface IPred<T> {
  // Applies to item
  boolean apply(T t);
}

//Examples and tests
class ExamplesMazeGame {
  MazeWorld RunGame = new MazeWorld(50, 30);

  Graph graph = new Graph();

  // Tests the makeGrid method
  void testMakeGrid(Tester t) {
    MazeWorld firstWorld = new MazeWorld();
    t.checkExpect(firstWorld.board,
        new ArrayList<ArrayList<Vertex>>(Arrays.asList(
            new ArrayList<Vertex>(
                Arrays.asList(firstWorld.board.get(0).get(0), firstWorld.board.get(0).get(1))),
            new ArrayList<Vertex>(
                Arrays.asList(firstWorld.board.get(1).get(0), firstWorld.board.get(1).get(1))),
            new ArrayList<Vertex>(
                Arrays.asList(firstWorld.board.get(2).get(0), firstWorld.board.get(2).get(1))))));
  }

  // Tests vertexCombine method
  void testvertexCombine(Tester t) {
    MazeWorld firstWorld = new MazeWorld();
    t.checkExpect(firstWorld.board.get(0).get(0).top, null);
    t.checkExpect(firstWorld.board.get(0).get(0).bottom, firstWorld.board.get(1).get(0));

  }

  // Tests edgesMake method
  void testedgesMake(Tester t) {

    MazeWorld firstWorld = new MazeWorld();
    t.checkExpect(firstWorld.edgeList.get(3),
        new Edge(new Vertex(firstWorld.board.get(1).get(0).x, firstWorld.board.get(1).get(0).y),
            new Vertex(firstWorld.board.get(1).get(1).x, firstWorld.board.get(1).get(1).y), 4));
    t.checkExpect(firstWorld.edgeList.get(4),
        new Edge(new Vertex(firstWorld.board.get(1).get(0).x, firstWorld.board.get(1).get(0).y),
            new Vertex(firstWorld.board.get(2).get(0).x, firstWorld.board.get(2).get(0).y), 5));
  }

  // Tests mapMaker method
  void testmapMaker(Tester t) {
    MazeWorld firstWorld = new MazeWorld();
    t.checkExpect(firstWorld.map.get(firstWorld.board.get(0).get(1)),
        firstWorld.board.get(0).get(1));
    t.checkExpect(firstWorld.map.get(firstWorld.board.get(1).get(0)),
        firstWorld.board.get(1).get(0));
  }

  // Tests makeMST method
  void testmakeMST(Tester t) {
    MazeWorld firstWorld = new MazeWorld();

    firstWorld.makeGrid(firstWorld.xCoordinate, firstWorld.yCoordinate);
    t.checkExpect(firstWorld.treeSpan.get(1),
        new Edge(firstWorld.treeSpan.get(1).v1, firstWorld.treeSpan.get(1).v2, 2));
    t.checkExpect(firstWorld.treeSpan.get(4),
        new Edge(firstWorld.treeSpan.get(4).v1, firstWorld.treeSpan.get(4).v2, 6));
  }

  // makeDFS method tester
  void testmakeDFS(Tester t) {
    MazeWorld firstWorld = new MazeWorld();

    t.checkExpect(graph.makeDFS(firstWorld.board.get(0).get(0), firstWorld.board.get(2).get(1)),
        new ArrayList<Vertex>(Arrays.asList(firstWorld.board.get(0).get(0))));
  }

  // makeDFS method tester
  void testmakeBFS(Tester t) {
    MazeWorld firstWorld = new MazeWorld();

    t.checkExpect(graph.makeBFS(firstWorld.board.get(0).get(0), firstWorld.board.get(2).get(1)),
        new ArrayList<Vertex>(Arrays.asList(firstWorld.board.get(0).get(0))));
  }

  // Tests onKeyEvent method
  void testOnKeyEvent(Tester t) {

    MazeWorld firstWorld = new MazeWorld();
    firstWorld.onKeyEvent("up");
    t.checkExpect(firstWorld.user.tap, firstWorld.board.get(0).get(1));
    firstWorld.onKeyEvent("right");
    t.checkExpect(firstWorld.user.tap, firstWorld.board.get(0).get(1));
    firstWorld.onKeyEvent("d");
    t.checkExpect(firstWorld.makePath,
        new ArrayList<Vertex>(Arrays.asList(firstWorld.board.get(0).get(0))));
    firstWorld.onKeyEvent("b");
    t.checkExpect(firstWorld.makePath,
        new ArrayList<Vertex>(Arrays.asList(firstWorld.board.get(0).get(0))));
  }

  // Tests moveCorrect method
  void testmoveCorrect(Tester t) {
    MazeWorld firstWorld = new MazeWorld();

    t.checkExpect(firstWorld.user.moveCorrect("left"), false);
    t.checkExpect(firstWorld.user.moveCorrect("down"), true);
  }

  // Tests union method
  void testUnion(Tester t) {
    MazeWorld firstWorld = new MazeWorld();

    firstWorld.union(firstWorld.board.get(0).get(1), firstWorld.board.get(1).get(1));
    t.checkExpect(firstWorld.lookFor(firstWorld.board.get(0).get(1)),
        firstWorld.board.get(1).get(1));
    firstWorld.union(firstWorld.board.get(2).get(0), firstWorld.board.get(0).get(1));
    t.checkExpect(firstWorld.lookFor(firstWorld.board.get(0).get(0)),
        firstWorld.board.get(1).get(1));
  }

  // Tests lookFor method
  void testlookFor(Tester t) {
    MazeWorld firstWorld = new MazeWorld();
    t.checkExpect(firstWorld.lookFor(firstWorld.board.get(0).get(0)),
        firstWorld.board.get(0).get(0));
    t.checkExpect(firstWorld.lookFor(firstWorld.board.get(2).get(0)),
        firstWorld.board.get(2).get(0));
  }

  // Test moveRightMaker method
  void testChangemoveRightMaker(Tester t) {
    MazeWorld firstWorld = new MazeWorld();

    firstWorld.changemoveRightMaker(firstWorld.board.get(0).get(0));
    t.checkExpect(firstWorld.board.get(0).get(0).moveRightMaker, false);

    firstWorld.changemoveRightMaker(firstWorld.board.get(2).get(0));
    t.checkExpect(firstWorld.board.get(2).get(0).moveRightMaker, true);
  }

  // Tests moveBottomMaker method
  void testChangemoveBottomMaker(Tester t) {
    MazeWorld firstWorld = new MazeWorld();
    firstWorld.changemoveBottomMaker(firstWorld.board.get(0).get(1));
    t.checkExpect(firstWorld.board.get(0).get(1).moveBottomMaker, false);

    firstWorld.changemoveBottomMaker(firstWorld.board.get(2).get(1));
    t.checkExpect(firstWorld.board.get(2).get(1).moveBottomMaker, true);
  }

  // Tests add method for Queue
  void testadd(Tester t) {
    Queue<Vertex> w = new Queue<Vertex>();

    t.checkExpect(w.size(), 0);
    w.add(new Vertex(0, 0));
    t.checkExpect(w.size(), 1);
  }

  // Tests size
  void testSize(Tester t) {
    Queue<Vertex> p = new Queue<Vertex>();
    Stack<Vertex> a = new Stack<Vertex>();

    t.checkExpect(p.size(), 0);
    p.add(new Vertex(1, 0));
    t.checkExpect(p.size(), 1);

    t.checkExpect(a.size(), 0);
    a.add(new Vertex(0, 0));
    t.checkExpect(a.size(), 1);
  }

  // Tests remove for queue
  void RemoveTester(Tester t) {
    Queue<Vertex> q = new Queue<Vertex>();
    q.add(new Vertex(0, 0));
    t.checkExpect(q.remove(), new Vertex(0, 0));
  }

  // Tests add for stack
  void AddTester(Tester t) {
    Stack<Vertex> stack = new Stack<Vertex>();
    t.checkExpect(stack.size(), 0);
    stack.add(new Vertex(0, 0));
    t.checkExpect(stack.size(), 1);
  }

  // hasmakePath method tester
  void testHasmakePath(Tester t) {
    MazeWorld firstWorld = new MazeWorld();

    t.checkExpect(
        graph.createmakePath(firstWorld.board.get(0).get(0), firstWorld.board.get(2).get(1),
            new Stack<Vertex>()),
        new ArrayList<Vertex>(Arrays.asList(firstWorld.board.get(0).get(0))));

    t.checkExpect(
        graph.createmakePath(firstWorld.board.get(0).get(0), firstWorld.board.get(2).get(1),
            new Queue<Vertex>()),
        new ArrayList<Vertex>(Arrays.asList(firstWorld.board.get(0).get(0))));
  }

  // Runs the world
  void testBigBang(Tester t) {
    this.RunGame.bigBang(this.RunGame.xCoordinate * MazeWorld.boxSize,
        this.RunGame.yCoordinate * MazeWorld.boxSize + MazeWorld.boxSize, this.RunGame.tickRate);
  }
}
