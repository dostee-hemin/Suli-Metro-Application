class Route {
  ArrayList<Intersection> intersections = new ArrayList<Intersection>();
  color lineColor;

  Route(color c) {
    this.lineColor = c;
  }

  void addIntersection(Station current, Station[] neighbors) {
    intersections.add(new Intersection(current, neighbors));
  }

  void display() {
    for (Intersection i : intersections) {
      stroke(lineColor);
      strokeWeight(directions.isEmpty()?4:2);
      for (Station n : i.neighbors) {
        line(i.current.position.x, i.current.position.y, n.position.x, n.position.y);
      }
    }

    for (Intersection i : intersections) {
      i.current.display();
    }
  }
  
  boolean contains(Station s) {
    for(Intersection i : intersections) {
      if(i.current == s) return true;
    }
    return false;
  }
  
  
  boolean isLinked(Station s1, Station s2) {
    for(Intersection i : intersections) {
      if(i.current == s1) {
        for(Station neighbor : i.neighbors) {
          if(neighbor == s2) return true;
        }
      }
    }
    return false;
  }
}

class Intersection {
  Station current;
  Station[] neighbors;

  Intersection(Station current, Station[] neighbors) {
    this.current = current;
    this.neighbors = neighbors;
  }
}
