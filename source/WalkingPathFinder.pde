class WalkingPathFinder extends Thread {
  ArrayList<PVector> path = new ArrayList<PVector>();
  ArrayList<Node> openSet = new ArrayList<Node>();
  ArrayList<Node> closedSet = new ArrayList<Node>();
  Node startNode;
  Node endNode;

  WalkingPathFinder(ArrayList<PVector> path, PVector A, PVector B) {
    path.clear();
    this.path = path;

    for (Node n : roadNodes) {
      if (n.position == A) startNode = n;
      if (n.position == B) endNode = n;
    }

    openSet.add(startNode);
  }

  @Override
    public void run() {
    try {
      while (openSet.size() > 0) {
        Node currentNode = openSet.get(0);
        for (int i = 1; i < openSet.size(); i++) {
          if (openSet.get(i).f < currentNode.f) {
            currentNode = openSet.get(i);
          }
        }

        if (currentNode == endNode) {
          Node current = currentNode;
          while (current != null) {
            path.add(current.position);
            current = current.parent;
          }
          Collections.reverse(path);
          return;
        }

        openSet.remove(currentNode);
        closedSet.add(currentNode);

        int padding = 10;
        for (int i=0; i<roads.length; i++) {
          Node neighbor = roadNodes[i];
          if (neighbor.position.x < min(startNode.position.x, endNode.position.x) - padding ||
            neighbor.position.x > max(startNode.position.x, endNode.position.x) + padding ||
            neighbor.position.y < min(startNode.position.y, endNode.position.y) - padding ||
            neighbor.position.y > max(startNode.position.y, endNode.position.y) + padding)
            continue;

          if (distSq(currentNode.position, neighbor.position) > 1) continue;

          if (closedSet.contains(neighbor)) continue;

          float tentativeGScore = currentNode.g + distSq(currentNode.position, neighbor.position);
          boolean newNeighbor = !openSet.contains(neighbor);
          if (newNeighbor || tentativeGScore < neighbor.g) {
            neighbor.parent = currentNode;
            neighbor.g = tentativeGScore;
            neighbor.calculateHeuristic(endNode);
            neighbor.calculateScore();
            if (newNeighbor) {
              openSet.add(neighbor);
            }
          }
        }
      }
    }
    catch (Exception e) {
    }
  }
}
