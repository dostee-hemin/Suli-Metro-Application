class Direction {
  String path;
  int distance;
  float time;

  Direction(String origin, String destination) {
    path = origin + "  →  " + destination;
  }

  String getDistance() {
    return "woopsie";
  }

  void displayMap() {
  }

  void displayPanel(float x, float y) {
    fill(255, 100);
    stroke(200);
    strokeWeight(3);
    rectMode(CORNER);
    rect(x+10, y, targetPanelX-60, 140, 10);

    fill(50);
    textSize(40);
    textAlign(CENTER, CENTER);
    text(path, (2*x+50+targetPanelX)/2, y+40);

    int hours = int(time) / 3600;
    int minutes = (int(time) % 3600) / 60;
    String formattedTime = "";
    if (hours > 0) {
      formattedTime += hours + " hrs ";
    }
    if (minutes > 1) {
      formattedTime += minutes + " mins";
    } else formattedTime = "~1min";
    textSize(25);
    textAlign(LEFT);
    text(formattedTime, x+30, y+125);
    textAlign(RIGHT);
    text(getDistance(), x+targetPanelX-90, y+125);
  }
}

class WalkingDirection extends Direction {
  ArrayList<PVector> path;

  WalkingDirection(String origin, String destination, ArrayList<PVector> path) {
    super(origin, destination);
    distance = path.size() * meterPerPixel;
    time = distance / 1.4;
    this.path = path;
  }

  @Override
    String getDistance() {
    String formattedDistance = "";
    if (distance < 1000) {
      formattedDistance = distance + " m";
    } else {
      float kmDistance = distance / 1000.0;
      formattedDistance = String.format("%.2f km", kmDistance);
    }
    return formattedDistance;
  }

  @Override
    void displayMap() {
    for (int i=0; i<path.size(); i+=map(zoomLevel, minZoom, maxZoom, 20, 5)) {
      PVector p = path.get(i);
      fill(#00b0ff);
      stroke(#1967d2);
      strokeWeight(map(zoomLevel, minZoom, maxZoom, 2, 0.5));
      ellipse(p.x, p.y, map(zoomLevel, minZoom, maxZoom, 5, 3), map(zoomLevel, minZoom, maxZoom, 5, 3));
    }
  }

  @Override
    void displayPanel(float x, float y) {
    super.displayPanel(x, y);

    imageMode(CENTER);
    image(directionIcons[0], x+70, y+50, 80, 80);
  }
}

class MetroDirection extends Direction {
  int numStops;
  color tagColor;
  ArrayList<Station> routeStations;

  MetroDirection(ArrayList<Station> routeStations, int numStops, float distance, color tagColor) {
    super(routeStations.get(0).name, routeStations.get(routeStations.size()-1).name);
    this.numStops = numStops;
    this.tagColor = tagColor;
    time = (distance*meterPerPixel) / 16.7 + (numStops * 10);
    this.distance = int(distance*meterPerPixel);
    this.routeStations = routeStations;
  }

  @Override
    String getDistance() {
    if (numStops == 1) return "1 stop";
    return numStops + " stops";
  }

  @Override
    void displayMap() {
    for (int i=0; i<routeStations.size()-1; i++) {
      Station p1 = routeStations.get(i);
      Station p2 = routeStations.get(i+1);

      stroke(darker(tagColor));
      strokeWeight(5);
      line(p1.position.x, p1.position.y, p2.position.x, p2.position.y);
      stroke(tagColor);
      strokeWeight(2);
      line(p1.position.x, p1.position.y, p2.position.x, p2.position.y);

      strokeWeight(2);
      stroke(0);
      fill(255);
      ellipse(p1.position.x, p1.position.y, 5, 5);
      ellipse(p2.position.x, p2.position.y, 5, 5);
    }
  }

  @Override
    void displayPanel(float x, float y) {
    super.displayPanel(x, y);

    imageMode(CENTER);
    image(directionIcons[1], x+70, y+50, 80, 80);
    fill(tagColor);
    noStroke();
    rectMode(CENTER);
    rect(x+70, y+90, 50, 10);
  }
}
