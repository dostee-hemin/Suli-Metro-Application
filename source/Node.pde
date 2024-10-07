class Node {
  PVector position;
  float f, g, h;
  Node parent;
  Station station;
  
  Node(Station s) {
    this(s.position);
    station = s;
  }

  Node(PVector pos) {
    position = pos;
    f = 0;
    g = 0;
    h = 0;
    parent = null;
  }
  
  void calculateHeuristic(Node goal) {
    h = distSq(position, goal.position);
  }
  
  void calculateScore() {
    f = g + h;
  }
}
