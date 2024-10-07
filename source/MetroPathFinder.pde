class MetroPathFinder extends Thread {
  ArrayList<Station> path = new ArrayList<Station>();
  ArrayList<Node> openSet = new ArrayList<Node>();
  ArrayList<Node> closedSet = new ArrayList<Node>();
  Node startNode;
  Node endNode;

  MetroPathFinder(ArrayList<Station> path, Station A, Station B) {
    this.path = path;

    for (Node n : stationNodes) {
      if (n.station == A) startNode = n;
      if (n.station == B) endNode = n;
    }

    openSet.add(startNode);
  }

  @Override
    public void run() {
    try {
      while (openSet.size() > 0) {
        startNode.parent = null;
        Node currentNode = openSet.get(0);
        for (int i = 1; i < openSet.size(); i++) {
          if (openSet.get(i).f < currentNode.f) {
            currentNode = openSet.get(i);
          }
        }

        if (currentNode == endNode) {
          Node current = currentNode;
          while (current != null) {
            path.add(current.station);
            current = current.parent;
          }
          Collections.reverse(path);
          return;
        }

        openSet.remove(currentNode);
        closedSet.add(currentNode);

        for (int i=0; i<stations.size(); i++) {
          Node neighbor = stationNodes.get(i);

          boolean isNeighbor = false;
          for (Station s : currentNode.station.connectingStations) {
            if (neighbor.station == s) isNeighbor = true;
          }
          if (!isNeighbor) continue;

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
